package com.systemapp.daily.ui.revision_v2

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.systemapp.daily.databinding.FragmentStep5FinalizarBinding
import com.systemapp.daily.ui.lectura.FotoAdapter
import com.systemapp.daily.utils.Constants
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
            val censo = viewModel.censoItems.value
            if (!censo.isNullOrEmpty()) {
                append("Puntos hidráulicos: ${censo.size}\n")
            }
            val fotos = viewModel.fotos.value
            append("Fotos: ${fotos?.size ?: 0}")
        }
        binding.tvResumen.text = resumen
    }

    override fun onPause() {
        super.onPause()
        if (!binding.signaturePad.isEmpty()) {
            val bitmap = binding.signaturePad.getSignatureBitmap()
            viewModel.guardarFirma(bitmap)
            bitmap.recycle()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
