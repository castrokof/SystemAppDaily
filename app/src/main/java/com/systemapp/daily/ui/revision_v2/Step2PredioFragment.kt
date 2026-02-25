package com.systemapp.daily.ui.revision_v2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.systemapp.daily.R
import com.systemapp.daily.data.location.GpsLocationManager
import com.systemapp.daily.databinding.FragmentStep2PredioBinding
import kotlinx.coroutines.launch

class Step2PredioFragment : Fragment() {

    private var _binding: FragmentStep2PredioBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RevisionWizardViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStep2PredioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val estadosAcometida = listOf("Bueno", "Malo", "Regular", "No tiene")
        binding.spEstadoAcometida.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, estadosAcometida))

        val estadosSellos = listOf("Intactos", "Rotos", "Sin sellos", "No aplica")
        binding.spEstadoSellos.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, estadosSellos))

        // Restore data
        if (viewModel.estadoAcometida.isNotBlank()) binding.spEstadoAcometida.setText(viewModel.estadoAcometida, false)
        if (viewModel.estadoSellos.isNotBlank()) binding.spEstadoSellos.setText(viewModel.estadoSellos, false)
        if (viewModel.generalidades.isNotBlank()) binding.etGeneralidades.setText(viewModel.generalidades)

        // GPS capture
        obtenerGps()
    }

    private fun obtenerGps() {
        val gpsManager = GpsLocationManager.getInstance(requireContext())
        if (!gpsManager.hasPermission()) {
            binding.tvGpsStatus.text = "Sin permiso GPS"
            binding.tvGpsStatus.setTextColor(requireContext().getColor(R.color.error))
            binding.progressGps.visibility = View.GONE
            return
        }

        lifecycleScope.launch {
            when (val result = gpsManager.getCurrentLocation()) {
                is GpsLocationManager.GpsResult.Success -> {
                    viewModel.gpsLatitud = result.location.latitude
                    viewModel.gpsLongitud = result.location.longitude
                    binding.tvGpsStatus.text = String.format("%.6f, %.6f (±%.0fm)", result.location.latitude, result.location.longitude, result.location.accuracy)
                    binding.tvGpsStatus.setTextColor(requireContext().getColor(R.color.success))
                }
                is GpsLocationManager.GpsResult.LowAccuracy -> {
                    viewModel.gpsLatitud = result.location.latitude
                    viewModel.gpsLongitud = result.location.longitude
                    binding.tvGpsStatus.text = String.format("%.6f, %.6f (±%.0fm - baja precisión)", result.location.latitude, result.location.longitude, result.location.accuracy)
                    binding.tvGpsStatus.setTextColor(requireContext().getColor(R.color.warning))
                }
                is GpsLocationManager.GpsResult.Error -> {
                    binding.tvGpsStatus.text = result.message
                    binding.tvGpsStatus.setTextColor(requireContext().getColor(R.color.error))
                }
            }
            binding.progressGps.visibility = View.GONE
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.estadoAcometida = binding.spEstadoAcometida.text.toString().trim()
        viewModel.estadoSellos = binding.spEstadoSellos.text.toString().trim()
        viewModel.generalidades = binding.etGeneralidades.text.toString().trim()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
