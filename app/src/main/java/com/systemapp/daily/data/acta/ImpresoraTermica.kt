package com.systemapp.daily.data.acta

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.UUID

/**
 * Envía una imagen (el acta ya dibujada al ancho del papel) a una térmica ESC/POS
 * por Bluetooth SPP. Sirve igual para 58 mm y 80 mm: el ancho lo define el bitmap.
 */
object ImpresoraTermica {

    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private const val FILAS_POR_BLOQUE = 128

    @SuppressLint("MissingPermission")
    fun impresorasPareadas(): List<BluetoothDevice> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()
        return try { adapter.bondedDevices?.toList() ?: emptyList() } catch (_: SecurityException) { emptyList() }
    }

    @SuppressLint("MissingPermission")
    fun buscarPorDireccion(direccion: String?): BluetoothDevice? =
        direccion?.let { d -> impresorasPareadas().firstOrNull { it.address == d } }

    /** Bloqueante: llamar desde Dispatchers.IO. */
    @SuppressLint("MissingPermission")
    fun imprimir(device: BluetoothDevice, imagen: Bitmap, copias: Int = 1): Result<Unit> {
        var socket: BluetoothSocket? = null
        return try {
            try { BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery() } catch (_: SecurityException) { }
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            val out = socket.outputStream
            val datos = comandos(imagen)
            repeat(copias.coerceAtLeast(1)) {
                // Se envía en trozos para no desbordar el buffer de impresoras económicas
                var i = 0
                while (i < datos.size) {
                    val n = minOf(4096, datos.size - i)
                    out.write(datos, i, n)
                    out.flush()
                    i += n
                    Thread.sleep(20)
                }
            }
            Thread.sleep(400) // dar tiempo a vaciar el buffer antes de cerrar
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(Exception("No se pudo conectar con la impresora: ${e.message}"))
        } catch (e: SecurityException) {
            Result.failure(Exception("Sin permiso Bluetooth"))
        } finally {
            try { socket?.close() } catch (_: IOException) { }
        }
    }

    /** ESC @ + imagen raster (GS v 0) por bloques + avance y corte. */
    private fun comandos(img: Bitmap): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(byteArrayOf(0x1B, 0x40))       // init
        out.write(byteArrayOf(0x1B, 0x61, 0x01)) // centrar
        val ancho = img.width
        val bytesFila = (ancho + 7) / 8
        val pixeles = IntArray(ancho)
        var y = 0
        while (y < img.height) {
            val filas = minOf(FILAS_POR_BLOQUE, img.height - y)
            out.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00,
                (bytesFila and 0xFF).toByte(), ((bytesFila shr 8) and 0xFF).toByte(),
                (filas and 0xFF).toByte(), ((filas shr 8) and 0xFF).toByte()))
            for (f in 0 until filas) {
                img.getPixels(pixeles, 0, ancho, 0, y + f, ancho, 1)
                val fila = ByteArray(bytesFila)
                for (x in 0 until ancho) {
                    val p = pixeles[x]
                    val gris = (((p shr 16) and 0xFF) * 299 + ((p shr 8) and 0xFF) * 587 + (p and 0xFF) * 114) / 1000
                    if (gris < 150) fila[x shr 3] = (fila[x shr 3].toInt() or (0x80 shr (x and 7))).toByte()
                }
                out.write(fila)
            }
            y += filas
        }
        out.write(byteArrayOf(0x1B, 0x64, 0x04)) // avanzar 4 líneas
        out.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00)) // corte (si la impresora lo soporta)
        return out.toByteArray()
    }
}
