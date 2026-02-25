package com.systemapp.daily.ui.revision_v2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.systemapp.daily.databinding.FragmentStep1RegistroBinding

class Step1RegistroFragment : Fragment() {

    private var _binding: FragmentStep1RegistroBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RevisionWizardViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStep1RegistroBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tiposDoc = listOf("CC", "TI", "CE", "NIT", "Pasaporte", "Otro")
        binding.spTipoDocumento.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, tiposDoc))

        val motivos = listOf("Desviación Baja", "Desviación Alta", "Revisión Periódica", "Solicitud Usuario", "Otro")
        binding.spMotivoRevision.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, motivos))

        binding.spMotivoRevision.setOnItemClickListener { _, _, position, _ ->
            binding.tilMotivoDetalle.visibility = if (position == motivos.size - 1) View.VISIBLE else View.GONE
        }

        // Restore data if exists
        if (viewModel.nombreAtiende.isNotBlank()) binding.etNombreAtiende.setText(viewModel.nombreAtiende)
        if (viewModel.tipoDocumento.isNotBlank()) binding.spTipoDocumento.setText(viewModel.tipoDocumento, false)
        if (viewModel.documento.isNotBlank()) binding.etDocumento.setText(viewModel.documento)
        if (viewModel.motivoRevision.isNotBlank()) binding.spMotivoRevision.setText(viewModel.motivoRevision, false)
        if (viewModel.motivoDetalle.isNotBlank()) {
            binding.etMotivoDetalle.setText(viewModel.motivoDetalle)
            binding.tilMotivoDetalle.visibility = View.VISIBLE
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.nombreAtiende = binding.etNombreAtiende.text.toString().trim()
        viewModel.tipoDocumento = binding.spTipoDocumento.text.toString().trim()
        viewModel.documento = binding.etDocumento.text.toString().trim()
        viewModel.motivoRevision = binding.spMotivoRevision.text.toString().trim()
        viewModel.motivoDetalle = binding.etMotivoDetalle.text.toString().trim()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
