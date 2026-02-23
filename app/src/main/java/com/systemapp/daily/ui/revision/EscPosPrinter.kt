package com.systemapp.daily.ui.revision

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.graphics.Bitmap
import com.systemapp.daily.data.model.ChecklistItem
import com.systemapp.daily.data.model.EstadoCheck
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Impresora ESC/POS para impresoras térmicas Bluetooth de 58mm (384 dots).
 * Genera e imprime el acta de revisión en formato ticket.
 */
class EscPosPrinter {

    companion object {
        // UUID estándar para SPP (Serial Port Profile) Bluetooth
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        // Ancho en caracteres para 58mm (32 chars en fuente normal)
        private const val LINE_WIDTH = 32
        // Ancho en dots para imagen (384 para 58mm)
        private const val PRINT_WIDTH_DOTS = 384

        // Comandos ESC/POS
        private val ESC_INIT = byteArrayOf(0x1B, 0x40)              // Inicializar
        private val ESC_ALIGN_CENTER = byteArrayOf(0x1B, 0x61, 0x01) // Centrar
        private val ESC_ALIGN_LEFT = byteArrayOf(0x1B, 0x61, 0x00)   // Izquierda
        private val ESC_BOLD_ON = byteArrayOf(0x1B, 0x45, 0x01)      // Negrita ON
        private val ESC_BOLD_OFF = byteArrayOf(0x1B, 0x45, 0x00)     // Negrita OFF
        private val ESC_DOUBLE_HEIGHT = byteArrayOf(0x1D, 0x21, 0x01) // Doble alto
        private val ESC_NORMAL_SIZE = byteArrayOf(0x1D, 0x21, 0x00)   // Tamaño normal
        private val ESC_CUT = byteArrayOf(0x1D, 0x56, 0x00)           // Cortar papel
        private val ESC_FEED_LINES = byteArrayOf(0x1B, 0x64, 0x04)    // Avanzar 4 líneas
        private val LINE_FEED = byteArrayOf(0x0A)                     // Salto de línea
    }

    data class ActaTicketData(
        val empresa: String,
        val refMedidor: String,
        val suscriptor: String?,
        val direccion: String?,
        val medidorNombre: String,
        val checklistItems: List<ChecklistItem>,
        val observacion: String?,
        val latitud: Double?,
        val longitud: Double?,
        val usuario: String,
        val firmaUsuario: Bitmap?,
        val firmaCliente: Bitmap?
    )

    /**
     * Obtiene la lista de impresoras Bluetooth disponibles (pareadas).
     */
    fun getPairedPrinters(): List<BluetoothDevice> {
        val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()
        return adapter.bondedDevices?.toList() ?: emptyList()
    }

    /**
     * Imprime el acta en la impresora Bluetooth especificada.
     */
    fun imprimirActa(device: BluetoothDevice, data: ActaTicketData): Result<Unit> {
        var socket: BluetoothSocket? = null
        return try {
            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            val outputStream = socket.outputStream

            printActa(outputStream, data)

            outputStream.flush()
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(Exception("Error de conexión con la impresora: ${e.message}"))
        } catch (e: SecurityException) {
            Result.failure(Exception("Sin permiso Bluetooth: ${e.message}"))
        } finally {
            try {
                socket?.close()
            } catch (_: IOException) { }
        }
    }

    private fun printActa(out: OutputStream, data: ActaTicketData) {
        val fecha = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

        // Inicializar impresora
        out.write(ESC_INIT)

        // === ENCABEZADO ===
        out.write(ESC_ALIGN_CENTER)
        out.write(ESC_DOUBLE_HEIGHT)
        out.write(ESC_BOLD_ON)
        printLine(out, "ACTA DE REVISION")
        printLine(out, "SERVICIO ACUEDUCTO")
        out.write(ESC_NORMAL_SIZE)
        out.write(ESC_BOLD_OFF)
        printLine(out, "")
        printLine(out, data.empresa)
        printLine(out, fecha)
        printSeparator(out, '=')

        // === DATOS DEL SUSCRIPTOR ===
        out.write(ESC_ALIGN_LEFT)
        out.write(ESC_BOLD_ON)
        printLine(out, "DATOS DEL SUSCRIPTOR")
        out.write(ESC_BOLD_OFF)
        printSeparator(out, '-')
        printLine(out, "Ref: ${data.refMedidor}")
        printLine(out, "Suscriptor: ${data.suscriptor ?: "N/A"}")
        printLine(out, truncate("Nombre: ${data.medidorNombre}", LINE_WIDTH))
        printLine(out, truncate("Dir: ${data.direccion ?: "N/A"}", LINE_WIDTH))
        printLine(out, "Inspector: ${data.usuario}")

        if (data.latitud != null && data.longitud != null) {
            printLine(out, "GPS: ${String.format("%.4f", data.latitud)},${String.format("%.4f", data.longitud)}")
        }
        printSeparator(out, '=')

        // === CHECKLIST ===
        out.write(ESC_BOLD_ON)
        printLine(out, "RESULTADO DE REVISION")
        out.write(ESC_BOLD_OFF)
        printSeparator(out, '-')

        var currentCategoria = ""
        for (item in data.checklistItems) {
            if (item.categoria != currentCategoria) {
                currentCategoria = item.categoria
                out.write(ESC_BOLD_ON)
                printLine(out, "[$currentCategoria]")
                out.write(ESC_BOLD_OFF)
            }

            val estado = when (item.estado) {
                EstadoCheck.BUENO -> "[OK]"
                EstadoCheck.MALO -> "[MAL]"
                EstadoCheck.NO_APLICA -> "[N/A]"
                EstadoCheck.NO_REVISADO -> "[ - ]"
            }

            val desc = truncate(item.descripcion, LINE_WIDTH - estado.length - 1)
            val padding = LINE_WIDTH - desc.length - estado.length
            val line = desc + " ".repeat(maxOf(padding, 1)) + estado
            printLine(out, line)
        }
        printSeparator(out, '-')

        // === RESUMEN ===
        val buenos = data.checklistItems.count { it.estado == EstadoCheck.BUENO }
        val malos = data.checklistItems.count { it.estado == EstadoCheck.MALO }
        val na = data.checklistItems.count { it.estado == EstadoCheck.NO_APLICA }
        val sinRev = data.checklistItems.count { it.estado == EstadoCheck.NO_REVISADO }

        printLine(out, "OK:$buenos MAL:$malos N/A:$na S/R:$sinRev")
        printSeparator(out, '=')

        // === OBSERVACIONES ===
        if (!data.observacion.isNullOrBlank()) {
            out.write(ESC_BOLD_ON)
            printLine(out, "OBSERVACIONES")
            out.write(ESC_BOLD_OFF)
            // Dividir en líneas del ancho máximo
            val lines = wrapText(data.observacion, LINE_WIDTH)
            for (line in lines) {
                printLine(out, line)
            }
            printSeparator(out, '=')
        }

        // === FIRMAS ===
        out.write(ESC_BOLD_ON)
        printLine(out, "FIRMAS")
        out.write(ESC_BOLD_OFF)
        printLine(out, "")

        // Imprimir firma del inspector como imagen
        if (data.firmaUsuario != null) {
            out.write(ESC_ALIGN_CENTER)
            printLine(out, "Inspector:")
            printBitmap(out, data.firmaUsuario)
            printLine(out, data.usuario)
            printLine(out, "")
        }

        // Imprimir firma del cliente como imagen
        if (data.firmaCliente != null) {
            out.write(ESC_ALIGN_CENTER)
            printLine(out, "Cliente:")
            printBitmap(out, data.firmaCliente)
            printLine(out, data.suscriptor ?: data.medidorNombre)
            printLine(out, "")
        }

        out.write(ESC_ALIGN_LEFT)
        printSeparator(out, '=')

        // === PIE ===
        out.write(ESC_ALIGN_CENTER)
        printLine(out, "Copia para el cliente")
        printLine(out, "SystemApp Lecturas")
        printLine(out, fecha)

        // Avanzar papel y cortar
        out.write(ESC_FEED_LINES)
        out.write(ESC_CUT)
    }

    /**
     * Imprime un bitmap escalado al ancho de la impresora usando comandos ESC/POS.
     */
    private fun printBitmap(out: OutputStream, bitmap: Bitmap) {
        // Escalar al ancho de impresión manteniendo proporción
        val ratio = PRINT_WIDTH_DOTS.toFloat() / bitmap.width
        val newHeight = (bitmap.height * ratio).toInt()
        val scaled = Bitmap.createScaledBitmap(bitmap, PRINT_WIDTH_DOTS, newHeight, true)

        // Convertir a monocromo y enviar como imagen raster
        val width = scaled.width
        val height = scaled.height
        val bytesPerRow = (width + 7) / 8

        for (y in 0 until height) {
            // Comando: imprimir línea raster
            out.write(byteArrayOf(0x1D, 0x76, 0x30, 0x00))
            out.write(byteArrayOf(
                (bytesPerRow and 0xFF).toByte(),
                ((bytesPerRow shr 8) and 0xFF).toByte(),
                0x01, 0x00
            ))

            val rowData = ByteArray(bytesPerRow)
            for (x in 0 until width) {
                val pixel = scaled.getPixel(x, y)
                val gray = (0.299 * ((pixel shr 16) and 0xFF) +
                           0.587 * ((pixel shr 8) and 0xFF) +
                           0.114 * (pixel and 0xFF)).toInt()
                // Si es oscuro, poner bit en 1
                if (gray < 128) {
                    rowData[x / 8] = (rowData[x / 8].toInt() or (0x80 shr (x % 8))).toByte()
                }
            }
            out.write(rowData)
        }

        scaled.recycle()
    }

    private fun printLine(out: OutputStream, text: String) {
        out.write(text.toByteArray(Charsets.UTF_8))
        out.write(LINE_FEED)
    }

    private fun printSeparator(out: OutputStream, char: Char) {
        printLine(out, char.toString().repeat(LINE_WIDTH))
    }

    private fun truncate(text: String, maxLen: Int): String {
        return if (text.length > maxLen) text.substring(0, maxLen - 2) + ".." else text
    }

    private fun wrapText(text: String, maxLen: Int): List<String> {
        val result = mutableListOf<String>()
        var remaining = text
        while (remaining.length > maxLen) {
            val breakPoint = remaining.lastIndexOf(' ', maxLen)
            val splitAt = if (breakPoint > 0) breakPoint else maxLen
            result.add(remaining.substring(0, splitAt))
            remaining = remaining.substring(splitAt).trimStart()
        }
        if (remaining.isNotEmpty()) result.add(remaining)
        return result
    }
}
