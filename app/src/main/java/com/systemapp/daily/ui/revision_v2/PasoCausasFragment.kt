package com.systemapp.daily.ui.revision_v2

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.checkbox.MaterialCheckBox
import com.systemapp.daily.data.model.ListasRespaldo
import com.systemapp.daily.databinding.FragmentPasoCausasBinding

class PasoCausasFragment : PasoFragment() {

    private var _binding: FragmentPasoCausasBinding? = null
    private val binding get() = _binding!!
    override val contenido get() = _binding?.contenido

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPasoCausasBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onListo(listas: RevisionWizardViewModel.Listas) {
        binding.layoutCausas.removeAllViews()
        val alto = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48f, resources.displayMetrics).toInt()
        listas.causas.forEach { op ->
            val cb = MaterialCheckBox(requireContext()).apply {
                text = op.descripcion
                textSize = 16f
                minHeight = alto
                isChecked = op.codigo in viewModel.causas
                setOnCheckedChangeListener { _, marcado ->
                    viewModel.toggleCausa(op.codigo, marcado)
                    if (op.codigo == ListasRespaldo.CAUSA_OTRO) mostrarOtro()
                }
            }
            binding.layoutCausas.addView(cb)
        }
        binding.etOtroCual.setText(viewModel.causaOtroCual)
        binding.etOtroCual.doAfterTextChanged { viewModel.causaOtroCual = it?.toString().orEmpty() }
        mostrarOtro()
    }

    private fun mostrarOtro() {
        val visible = ListasRespaldo.CAUSA_OTRO in viewModel.causas
        binding.tilOtroCual.visibility = if (visible) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
