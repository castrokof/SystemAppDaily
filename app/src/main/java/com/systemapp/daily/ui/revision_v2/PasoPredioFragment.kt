package com.systemapp.daily.ui.revision_v2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import com.systemapp.daily.databinding.FragmentPasoPredioBinding

class PasoPredioFragment : PasoFragment() {

    private var _binding: FragmentPasoPredioBinding? = null
    private val binding get() = _binding!!
    override val contenido get() = _binding?.contenido

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPasoPredioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onListo(listas: RevisionWizardViewModel.Listas) {
        llenarChips(binding.cgTipoPredio, listas.tiposPredio, viewModel.tipoPredio) { viewModel.tipoPredio = it }
        llenarChips(binding.cgUsoPredio, listas.usos, viewModel.usoPredio) { viewModel.usoPredio = it }

        binding.etHabitantes.setText(viewModel.habitantes)
        binding.etTiempoHabitacion.setText(viewModel.tiempoHabitacion)
        binding.etHabitantes.doAfterTextChanged { viewModel.habitantes = it?.toString()?.trim().orEmpty() }
        binding.etTiempoHabitacion.doAfterTextChanged { viewModel.tiempoHabitacion = it?.toString().orEmpty() }
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
