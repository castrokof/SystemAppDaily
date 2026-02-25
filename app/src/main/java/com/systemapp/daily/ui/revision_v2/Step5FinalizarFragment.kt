package com.systemapp.daily.ui.revision_v2

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.systemapp.daily.databinding.FragmentStep5FinalizarBinding
import com.systemapp.daily.ui.lectura.CameraActivity
import com.systemapp.daily.ui.lectura.FotoAdapter
import com.systemapp.daily.utils.Constants

class Step5FinalizarFragment : Fragment() {

    private var _binding: FragmentStep5FinalizarBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RevisionWizardViewModel by activityViewModels()
    private lateinit var fotoAdapter: FotoAdapter

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val path = result.data?.getStringExtra(Constants.EXTRA_PHOTO_PATH)
            if (path != null) viewModel.agregarFoto(path)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStep5FinalizarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fotoAdapter = FotoAdapter(mutableListOf()) { index -> viewModel.eliminarFoto(index) }
        binding.rvFotos.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvFotos.adapter = fotoAdapter

        binding.btnTomarFoto.setOnClickListener {
            val fotos = viewModel.fotos.value ?: emptyList()
            if (fotos.size >= Constants.MAX_FOTOS_POR_LECTURA) {
                Toast.makeText(requireContext(), "Máximo ${Constants.MAX_FOTOS_POR_LECTURA} fotos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            cameraLauncher.launch(Intent(requireContext(), CameraActivity::class.java))
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
        // Save signature if not empty
        if (!binding.signaturePad.isEmpty()) {
            val bitmap = binding.signaturePad.getSignatureBitmap()
            viewModel.guardarFirma(bitmap)
            bitmap.recycle()
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
