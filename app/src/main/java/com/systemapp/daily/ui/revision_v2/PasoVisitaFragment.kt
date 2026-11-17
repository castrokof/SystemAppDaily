package com.systemapp.daily.ui.revision_v2

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import com.systemapp.daily.databinding.FragmentPasoVisitaBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class PasoVisitaFragment : PasoFragment() {

    private var _binding: FragmentPasoVisitaBinding? = null
    private val binding get() = _binding!!
    override val contenido get() = _binding?.contenido

    private val iso = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val fmtFecha = SimpleDateFormat("dd/MM/yyyy", Locale.US)
    private val fmtHora = SimpleDateFormat("h:mm a", Locale("es", "CO"))

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPasoVisitaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onListo(listas: RevisionWizardViewModel.Listas) {
        binding.etLecturaVisita.setText(viewModel.lecturaVisita)
        binding.etLecturaVisita.doAfterTextChanged {
            viewModel.lecturaVisita = it?.toString()?.trim().orEmpty()
            mostrarAdvertencia()
        }
        mostrarAdvertencia()

        mostrarFecha()
        binding.btnFecha.setOnClickListener { elegirFecha() }
        binding.btnHora.setOnClickListener { elegirHora() }

        llenarChips(binding.cgMovimiento, listas.movimientos, viewModel.medidorMovimiento,
            etiqueta = { it.descripcion }) { viewModel.medidorMovimiento = it }
    }

    private fun mostrarAdvertencia() {
        val adv = viewModel.advertenciaLectura()
        binding.tvAdvertenciaLectura.text = adv?.let { "⚠ $it" }
        binding.tvAdvertenciaLectura.visibility = if (adv == null) View.GONE else View.VISIBLE
    }

    private fun calendario(): Calendar = Calendar.getInstance().apply {
        viewModel.fechaVisita?.let { runCatching { iso.parse(it) }.getOrNull() }?.let { time = it }
    }

    private fun mostrarFecha() {
        val c = calendario()
        binding.btnFecha.text = fmtFecha.format(c.time)
        binding.btnHora.text = fmtHora.format(c.time)
    }

    private fun elegirFecha() {
        val c = calendario()
        DatePickerDialog(requireContext(), { _, y, m, d ->
            c.set(y, m, d)
            viewModel.fechaVisita = iso.format(c.time)
            mostrarFecha()
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }.show()
    }

    private fun elegirHora() {
        val c = calendario()
        TimePickerDialog(requireContext(), { _, h, min ->
            c.set(Calendar.HOUR_OF_DAY, h); c.set(Calendar.MINUTE, min)
            viewModel.fechaVisita = iso.format(c.time)
            mostrarFecha()
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false).show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
