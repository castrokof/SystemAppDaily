package com.systemapp.daily.ui.impresora

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.systemapp.daily.R
import com.systemapp.daily.databinding.ActivityImpresoraBinding
import com.systemapp.daily.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

class ImpresoraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImpresoraBinding
    private lateinit var sessionManager: SessionManager
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val dispositivosAdapter = DispositivoAdapter { device ->
        seleccionarDispositivo(device)
    }

    private val btPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            cargarDispositivosVinculados()
        } else {
            Toast.makeText(this, R.string.permiso_bluetooth_requerido, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImpresoraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        val btManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = btManager.adapter

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.rvDispositivos.layoutManager = LinearLayoutManager(this)
        binding.rvDispositivos.adapter = dispositivosAdapter

        binding.btnBuscar.setOnClickListener {
            checkBluetoothPermissions()
        }

        binding.btnTestPrint.setOnClickListener {
            imprimirPrueba()
        }

        mostrarImpresoraActual()
        checkBluetoothPermissions()
    }

    private fun mostrarImpresoraActual() {
        val nombre = sessionManager.printerName
        val address = sessionManager.printerAddress
        if (nombre != null && address != null) {
            binding.tvImpresoraActual.text = "$nombre\n$address"
            binding.btnTestPrint.isEnabled = true
        }
    }

    private fun checkBluetoothPermissions() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.bluetooth_no_disponible, Toast.LENGTH_LONG).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permsNeeded = mutableListOf<String>()
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permsNeeded.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (permsNeeded.isNotEmpty()) {
                btPermissionLauncher.launch(permsNeeded.toTypedArray())
                return
            }
        }
        cargarDispositivosVinculados()
    }

    @Suppress("MissingPermission")
    private fun cargarDispositivosVinculados() {
        try {
            val pairedDevices = bluetoothAdapter?.bondedDevices ?: emptySet()
            val lista = pairedDevices.toList()
            dispositivosAdapter.submitList(lista)
            if (lista.isEmpty()) {
                Toast.makeText(this, R.string.no_dispositivos_vinculados, Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, R.string.permiso_bluetooth_requerido, Toast.LENGTH_LONG).show()
        }
    }

    @Suppress("MissingPermission")
    private fun seleccionarDispositivo(device: BluetoothDevice) {
        try {
            sessionManager.printerName = device.name ?: "Desconocido"
            sessionManager.printerAddress = device.address
            binding.tvImpresoraActual.text = "${device.name}\n${device.address}"
            binding.btnTestPrint.isEnabled = true
            Toast.makeText(this, getString(R.string.impresora_seleccionada, device.name), Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(this, R.string.permiso_bluetooth_requerido, Toast.LENGTH_LONG).show()
        }
    }

    @Suppress("MissingPermission")
    private fun imprimirPrueba() {
        val address = sessionManager.printerAddress ?: return
        binding.progressBar.visibility = View.VISIBLE
        binding.btnTestPrint.isEnabled = false

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val sppUuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                    val device = bluetoothAdapter?.getRemoteDevice(address)
                        ?: throw IOException("Dispositivo no encontrado")
                    val socket = device.createRfcommSocketToServiceRecord(sppUuid)
                    socket.connect()
                    val out = socket.outputStream
                    val esc = byteArrayOf(0x1B, 0x40) // Init
                    val center = byteArrayOf(0x1B, 0x61, 0x01)
                    val bold = byteArrayOf(0x1B, 0x45, 0x01)
                    val boldOff = byteArrayOf(0x1B, 0x45, 0x00)
                    val lf = byteArrayOf(0x0A)
                    val feed = byteArrayOf(0x1B, 0x64, 0x04)

                    out.write(esc)
                    out.write(center)
                    out.write(bold)
                    out.write("================================".toByteArray()); out.write(lf)
                    out.write("   PRUEBA DE IMPRESION".toByteArray()); out.write(lf)
                    out.write("   SystemApp Lecturas".toByteArray()); out.write(lf)
                    out.write("================================".toByteArray()); out.write(lf)
                    out.write(boldOff)
                    out.write("Impresora configurada OK".toByteArray()); out.write(lf)
                    out.write(feed)
                    out.flush()
                    socket.close()
                }
                Toast.makeText(this@ImpresoraActivity, R.string.impresion_prueba_ok, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@ImpresoraActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnTestPrint.isEnabled = true
            }
        }
    }

    // Adapter para lista de dispositivos BT
    class DispositivoAdapter(
        private val onClick: (BluetoothDevice) -> Unit
    ) : RecyclerView.Adapter<DispositivoAdapter.VH>() {

        private var devices = listOf<BluetoothDevice>()

        fun submitList(list: List<BluetoothDevice>) {
            devices = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_2, parent, false)
            return VH(view)
        }

        @Suppress("MissingPermission")
        override fun onBindViewHolder(holder: VH, position: Int) {
            val device = devices[position]
            try {
                holder.text1.text = device.name ?: "Sin nombre"
                holder.text2.text = device.address
            } catch (e: SecurityException) {
                holder.text1.text = "Dispositivo"
                holder.text2.text = device.address
            }
            holder.itemView.setOnClickListener { onClick(device) }
        }

        override fun getItemCount() = devices.size

        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val text1: TextView = view.findViewById(android.R.id.text1)
            val text2: TextView = view.findViewById(android.R.id.text2)
        }
    }
}
