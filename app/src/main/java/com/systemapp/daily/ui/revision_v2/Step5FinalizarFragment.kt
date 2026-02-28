package com.systemapp.daily.ui.revision_v2

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.systemapp.daily.databinding.FragmentStep5FinalizarBinding
import com.systemapp.daily.ui.lectura.FotoAdapter
import com.systemapp.daily.ui.revision.EscPosPrinter
import com.systemapp.daily.utils.Constants
import com.systemapp.daily.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Step5FinalizarFragment : Fragment() {

    private var _binding: FragmentStep5FinalizarBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RevisionWizardViewModel by activityViewModels()
    private lateinit var fotoAdapter: FotoAdapter
    private var currentPhotoPath: String? = null
    private val escPosPrinter = EscPosPrinter()

    // Usa la camara del sistema (TakePicture) para mayor compatibilidad
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val photoFile = currentPhotoPath?.let { File(it) }
        val photoSaved = success || (photoFile != null && photoFile.exists() && photoFile.length() > 0)

        if (photoSaved && currentPhotoPath != null) {
            viewModel.agregarFoto(currentPhotoPath!!)
        } else {
            currentPhotoPath?.let { path ->
                val file = File(path)
                if (file.exists() && file.length() == 0L) file.delete()
            }
            currentPhotoPath = null
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) abrirCamara()
        else Toast.makeText(requireContext(), "Se requiere permiso de cámara", Toast.LENGTH_LONG).show()
    }

    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            mostrarDialogoImpresoras()
        } else {
            Toast.makeText(requireContext(), "Se necesitan permisos Bluetooth para imprimir", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStep5FinalizarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        savedInstanceState?.let {
            currentPhotoPath = it.getString("current_photo_path")
        }

        fotoAdapter = FotoAdapter(mutableListOf()) { index -> viewModel.eliminarFoto(index) }
        binding.rvFotos.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvFotos.adapter = fotoAdapter

        binding.btnTomarFoto.setOnClickListener {
            val fotos = viewModel.fotos.value ?: emptyList()
            if (fotos.size >= Constants.MAX_FOTOS_POR_LECTURA) {
                Toast.makeText(requireContext(), "Máximo ${Constants.MAX_FOTOS_POR_LECTURA} fotos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            checkCameraPermissionAndOpen()
        }

        binding.btnLimpiarFirma.setOnClickListener {
            binding.signaturePad.clear()
        }

        viewModel.fotos.observe(viewLifecycleOwner) { fotos ->
            fotoAdapter.updateFotos(fotos)
            binding.tvFotoCount.text = "Fotos: ${fotos.size} / ${Constants.MIN_FOTOS_POR_LECTURA} mínimo"
        }

        viewModel.orden.observe(viewLifecycleOwner) { orden ->
            if (orden != null) {
                updateResumen()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        // Show print button after successful save
        viewModel.saveResult.observe(viewLifecycleOwner) { result ->
            if (result != null) {
                binding.btnImprimirActa.visibility = View.VISIBLE
            }
        }

        binding.btnImprimirActa.setOnClickListener {
            solicitarPermisosBluetooth()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("current_photo_path", currentPhotoPath)
    }

    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> abrirCamara()
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun abrirCamara() {
        val photoFile = createPhotoFile() ?: run {
            Toast.makeText(requireContext(), "Error al crear archivo de foto", Toast.LENGTH_SHORT).show()
            return
        }
        currentPhotoPath = photoFile.absolutePath
        val photoUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", photoFile)
        takePictureLauncher.launch(photoUri)
    }

    private fun createPhotoFile(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir("Pictures") ?: requireContext().filesDir
        if (!storageDir.exists()) storageDir.mkdirs()
        return try {
            File.createTempFile("REVISION_V2_${timeStamp}_", ".jpg", storageDir)
        } catch (e: Exception) {
            null
        }
    }

    private fun updateResumen() {
        val resumen = buildString {
            append("Predio: ${viewModel.orden.value?.codigoPredio}\n")
            if (viewModel.nombreAtiende.isNotBlank()) append("Atiende: ${viewModel.nombreAtiende}\n")
            if (viewModel.motivoRevision.isNotBlank()) append("Motivo: ${viewModel.motivoRevision}\n")
            if (viewModel.estadoAcometida.isNotBlank()) append("Acometida: ${viewModel.estadoAcometida}\n")
            if (viewModel.estadoSellos.isNotBlank()) append("Sellos: ${viewModel.estadoSellos}\n")
            append("Familias: ${viewModel.numFamilias}, Personas: ${viewModel.numPersonas}\n")
            if (viewModel.lecturaActual.isNotBlank()) append("Lectura: ${viewModel.lecturaActual}\n")
            val censo = viewModel.censoItems.value
            if (!censo.isNullOrEmpty()) {
                append("Puntos hidráulicos: ${censo.size}\n")
            }
            val fotos = viewModel.fotos.value
            append("Fotos: ${fotos?.size ?: 0}")
        }
        binding.tvResumen.text = resumen
    }

    /**
     * Called from Activity on main thread to get firma bitmap before finalizar.
     */
    fun obtenerFirmaBitmap(): android.graphics.Bitmap? {
        val binding = _binding ?: return null
        return if (!binding.signaturePad.isEmpty()) {
            binding.signaturePad.getSignatureBitmap()
        } else null
    }

    override fun onPause() {
        super.onPause()
        // Save firma on pause only if not already saved
        if (viewModel.firmaClientePath == null && _binding != null && !binding.signaturePad.isEmpty()) {
            val bitmap = binding.signaturePad.getSignatureBitmap()
            viewModel.guardarFirma(bitmap)
            bitmap.recycle()
        }
    }

    // ========================================
    // IMPRESIÓN BLUETOOTH ESC/POS 58mm
    // ========================================

    private fun solicitarPermisosBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permisos = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
            val necesita = permisos.any {
                ActivityCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
            }
            if (necesita) {
                bluetoothPermissionLauncher.launch(permisos)
            } else {
                mostrarDialogoImpresoras()
            }
        } else {
            mostrarDialogoImpresoras()
        }
    }

    private fun mostrarDialogoImpresoras() {
        val printers = escPosPrinter.getPairedPrinters()
        if (printers.isEmpty()) {
            Toast.makeText(requireContext(), "No hay impresoras Bluetooth pareadas. Vincula tu impresora desde Configuración > Bluetooth", Toast.LENGTH_LONG).show()
            return
        }

        val nombres = printers.map { "${it.name ?: "Desconocida"} (${it.address})" }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("Seleccionar Impresora")
            .setItems(nombres) { _, which ->
                imprimirEnImpresora(printers[which])
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun imprimirEnImpresora(device: BluetoothDevice) {
        val sessionManager = SessionManager(requireContext())
        val firmaCliente = if (_binding != null && !binding.signaturePad.isEmpty()) {
            binding.signaturePad.getSignatureBitmap()
        } else null

        val censo = viewModel.censoItems.value ?: emptyList()
        val observacionTexto = buildString {
            if (viewModel.estadoAcometida.isNotBlank()) append("Acometida: ${viewModel.estadoAcometida}. ")
            if (viewModel.estadoSellos.isNotBlank()) append("Sellos: ${viewModel.estadoSellos}. ")
            if (viewModel.generalidades.isNotBlank()) append(viewModel.generalidades)
            if (viewModel.lecturaActual.isNotBlank()) append(" Lectura: ${viewModel.lecturaActual}.")
            if (censo.isNotEmpty()) {
                append(" Censo: ")
                append(censo.joinToString(", ") { "${it.tipoPunto} x${it.cantidad} (${it.estado})" })
            }
        }.ifBlank { null }

        val ticketData = EscPosPrinter.ActaTicketData(
            empresa = sessionManager.userEmpresa ?: "N/A",
            refMedidor = viewModel.orden.value?.codigoPredio ?: "",
            suscriptor = viewModel.nombreAtiende.ifBlank { null },
            direccion = null,
            medidorNombre = viewModel.orden.value?.codigoPredio ?: "",
            checklistItems = emptyList(),
            observacion = observacionTexto,
            latitud = viewModel.gpsLatitud,
            longitud = viewModel.gpsLongitud,
            usuario = sessionManager.userName ?: sessionManager.userUsuario ?: "Inspector",
            firmaUsuario = null,
            firmaCliente = firmaCliente
        )

        binding.progressBar.visibility = View.VISIBLE
        binding.btnImprimirActa.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            val result = escPosPrinter.imprimirActa(device, ticketData)

            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                binding.btnImprimirActa.isEnabled = true
                firmaCliente?.recycle()

                result.onSuccess {
                    Toast.makeText(requireContext(), "Copia impresa para el cliente", Toast.LENGTH_SHORT).show()
                }.onFailure { error ->
                    Toast.makeText(requireContext(), error.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
