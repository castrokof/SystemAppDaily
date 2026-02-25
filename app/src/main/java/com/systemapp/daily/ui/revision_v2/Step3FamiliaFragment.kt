package com.systemapp.daily.ui.revision_v2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.systemapp.daily.databinding.FragmentStep3FamiliaBinding

class Step3FamiliaFragment : Fragment() {

    private var _binding: FragmentStep3FamiliaBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RevisionWizardViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStep3FamiliaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.etNumFamilias.setText(viewModel.numFamilias.toString())
        if (viewModel.numPersonas > 0) binding.etNumPersonas.setText(viewModel.numPersonas.toString())
    }

    override fun onPause() {
        super.onPause()
        viewModel.numFamilias = binding.etNumFamilias.text.toString().toIntOrNull() ?: 1
        viewModel.numPersonas = binding.etNumPersonas.text.toString().toIntOrNull() ?: 0
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
