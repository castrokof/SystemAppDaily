package com.systemapp.daily.ui.revision_v2

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.systemapp.daily.R
import com.systemapp.daily.data.location.GpsLocationManager
import com.systemapp.daily.data.model.EstadoPunto
import com.systemapp.daily.data.model.HidraulicoEntity
import com.systemapp.daily.data.model.TipoPunto
import com.systemapp.daily.databinding.FragmentPasoAdicionalBinding
import com.systemapp.daily.ui.lectura.FotoAdapter
import com.systemapp.daily.utils.Constants
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

class PasoAdicionalFragment : PasoFragment() {

    private var _binding: FragmentPasoAdicionalBinding? = null
    private val binding get() = _binding!!
    override val contenido get() = _binding?.contenido

    private lateinit var fotoAdapter: FotoAdapter
    private lateinit var censoAdapter: CensoAdapter

    /** Ruta de la foto en curso; se guarda en el estado por si Android recrea la pantalla. */
    private var fotoEnCurso: String? = null

    private val tomarFoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val path = fotoEnCurso ?: return@registerForActivityResult
        fotoEnCurso = null
        val f = File(path)
        if ((ok || f.length() > 0) && f.exists()) viewModel.agregarFoto(path)
        else f.delete()
    }

    private val permisoCamara = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) abrirCamara() else Toast.makeText(requireContext(), "Se requiere permiso de cámara", Toast.LENGTH_LONG).show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPasoAdicionalBinding.inflate(inflater, container, false)
        fotoEnCurso = savedInstanceState?.getString(KEY_FOTO)
        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_FOTO, fotoEnCurso)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Fotos y censo se muestran aunque el resto aún esté cargando
        fotoAdapter = FotoAdapter(mutableListOf()) { index ->
            if (!viewModel.soloLectura) viewModel.eliminarFoto(index)
        }
        binding.rvFotos.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvFotos.adapter = fotoAdapter
        viewModel.fotos.observe(viewLifecycleOwner) { fotos ->
            fotoAdapter.updateFotos(fotos)
            binding.tvFotoCount.text = "${fotos.size} de ${Constants.MAX_FOTOS_POR_LECTURA} fotos"
        }

        censoAdapter = CensoAdapter { index ->
            if (viewModel.soloLectura) return@CensoAdapter
            AlertDialog.Builder(requireContext())
                .setMessage("¿Eliminar este punto hidráulico?")
                .setPositiveButton("Eliminar") { _, _ -> viewModel.eliminarPuntoHidraulico(index) }
                .setNegativeButton("No", null)
                .show()
        }
        binding.rvCenso.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCenso.adapter = censoAdapter
        viewModel.censo.observe(viewLifecycleOwner) { items ->
            censoAdapter.submitList(items)
            binding.tvCensoCount.text = if (items.isEmpty()) "Sin puntos registrados" else "${items.size} punto(s)"
        }

        binding.btnTomarFoto.setOnClickListener {
            if ((viewModel.fotos.value?.size ?: 0) >= Constants.MAX_FOTOS_POR_LECTURA) {
                Toast.makeText(requireContext(), "Máximo ${Constants.MAX_FOTOS_POR_LECTURA} fotos", Toast.LENGTH_SHORT).show()
            } else if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                abrirCamara()
            } else permisoCamara.launch(Manifest.permission.CAMERA)
        }
        binding.btnAgregarPunto.setOnClickListener { dialogoPunto() }
        binding.btnGps.setOnClickListener { capturarGps() }
    }

    override fun onListo(listas: RevisionWizardViewModel.Listas) {
        binding.etNombreAtiende.setText(viewModel.nombreAtiende)
        binding.etNumFamilias.setText(viewModel.numFamilias)
        binding.etNombreAtiende.doAfterTextChanged { viewModel.nombreAtiende = it?.toString().orEmpty() }
        binding.etNumFamilias.doAfterTextChanged { viewModel.numFamilias = it?.toString()?.trim().orEmpty() }
        llenarLista(binding.actvAcometida, listas.acometida, viewModel.estadoAcometida) { viewModel.estadoAcometida = it }
        llenarLista(binding.actvSellos, listas.sellos, viewModel.estadoSellos) { viewModel.estadoSellos = it }

        if (viewModel.gpsLatitud != null) mostrarGps(viewModel.gpsLatitud!!, viewModel.gpsLongitud!!, null)
        else if (!viewModel.soloLectura) capturarGps()
        else binding.tvGpsStatus.text = "Sin ubicación"
    }

    override fun aplicarSoloLectura() {
        super.aplicarSoloLectura()
        // Las listas se pueden seguir desplazando en modo consulta
        binding.rvFotos.isEnabled = true
    }

    private fun abrirCamara() {
        try {
            val dir = (requireContext().getExternalFilesDir("Pictures") ?: requireContext().filesDir).apply { mkdirs() }
            val file = File.createTempFile("REV_${System.currentTimeMillis()}_", ".jpg", dir)
            fotoEnCurso = file.absolutePath
            val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
            tomarFoto.launch(uri)
        } catch (e: Exception) {
            fotoEnCurso = null
            Toast.makeText(requireContext(), "No se pudo abrir la cámara: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun capturarGps() {
        val gps = GpsLocationManager.getInstance(requireContext())
        if (!gps.hasPermission()) {
            binding.tvGpsStatus.text = "Sin permiso de ubicación"
            binding.tvGpsStatus.setTextColor(requireContext().getColor(R.color.error))
            return
        }
        binding.progressGps.visibility = View.VISIBLE
        binding.tvGpsStatus.text = "Obteniendo ubicación…"
        binding.tvGpsStatus.setTextColor(requireContext().getColor(R.color.text_secondary))
        viewLifecycleOwner.lifecycleScope.launch {
            val r = gps.getCurrentLocation()
            val b = _binding ?: return@launch
            b.progressGps.visibility = View.GONE
            when (r) {
                is GpsLocationManager.GpsResult.Success -> {
                    viewModel.gpsLatitud = r.location.latitude; viewModel.gpsLongitud = r.location.longitude
                    mostrarGps(r.location.latitude, r.location.longitude, r.location.accuracy)
                }
                is GpsLocationManager.GpsResult.LowAccuracy -> {
                    viewModel.gpsLatitud = r.location.latitude; viewModel.gpsLongitud = r.location.longitude
                    mostrarGps(r.location.latitude, r.location.longitude, r.location.accuracy)
                    b.tvGpsStatus.setTextColor(requireContext().getColor(R.color.warning))
                }
                is GpsLocationManager.GpsResult.Error -> {
                    b.tvGpsStatus.text = r.message
                    b.tvGpsStatus.setTextColor(requireContext().getColor(R.color.error))
                }
            }
        }
    }

    private fun mostrarGps(lat: Double, lng: Double, precision: Float?) {
        val b = _binding ?: return
        b.tvGpsStatus.text = String.format(Locale.US, "%.6f, %.6f", lat, lng) + (precision?.let { String.format(Locale.US, " (±%.0f m)", it) } ?: "")
        b.tvGpsStatus.setTextColor(requireContext().getColor(R.color.success))
    }

    private fun dialogoPunto() {
        val tipos = listOf(TipoPunto.GRIFO, TipoPunto.SANITARIO, TipoPunto.DUCHA, TipoPunto.LAVAMANOS,
            TipoPunto.LAVAPLATOS, TipoPunto.LAVADERO, TipoPunto.TANQUE, TipoPunto.CALENTADOR, TipoPunto.OTRO)
        val estados = listOf(EstadoPunto.BUENO, EstadoPunto.MALO)
        val ctx = requireContext()
        val pad = (16 * resources.displayMetrics.density).toInt()
        val layout = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(pad * 2, pad, pad * 2, 0) }
        val spTipo = Spinner(ctx).apply { adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, tipos) }
        val etCantidad = EditText(ctx).apply { inputType = android.text.InputType.TYPE_CLASS_NUMBER; setText("1"); selectAll() }
        val spEstado = Spinner(ctx).apply { adapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, estados) }
        layout.addView(TextView(ctx).apply { text = "Tipo de punto" }); layout.addView(spTipo)
        layout.addView(TextView(ctx).apply { text = "Cantidad"; setPadding(0, pad, 0, 0) }); layout.addView(etCantidad)
        layout.addView(TextView(ctx).apply { text = "Estado"; setPadding(0, pad, 0, 0) }); layout.addView(spEstado)

        AlertDialog.Builder(ctx)
            .setTitle("Agregar punto hidráulico")
            .setView(layout)
            .setPositiveButton("Agregar") { _, _ ->
                val cantidad = etCantidad.text.toString().toIntOrNull()?.coerceIn(1, 99) ?: 1
                viewModel.agregarPuntoHidraulico(spTipo.selectedItem.toString(), cantidad, spEstado.selectedItem.toString())
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }

    private class CensoAdapter(private val onDelete: (Int) -> Unit) : RecyclerView.Adapter<CensoAdapter.VH>() {
        private var items: List<HidraulicoEntity> = emptyList()

        @android.annotation.SuppressLint("NotifyDataSetChanged")
        fun submitList(list: List<HidraulicoEntity>) { items = list; notifyDataSetChanged() }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_censo_hidraulico, parent, false))

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.tipo.text = item.tipoPunto
            holder.cantidad.text = "x${item.cantidad}"
            holder.estado.text = item.estado
            val color = if (item.estado == EstadoPunto.BUENO) R.color.success else R.color.error
            (holder.estado.background?.mutate() as? GradientDrawable)?.setColor(holder.itemView.context.getColor(color))
            // posición actual (no la capturada al enlazar) para no borrar el ítem equivocado
            holder.eliminar.setOnClickListener {
                val pos = holder.bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) onDelete(pos)
            }
        }

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tipo: TextView = v.findViewById(R.id.tvTipoPunto)
            val cantidad: TextView = v.findViewById(R.id.tvCantidad)
            val estado: TextView = v.findViewById(R.id.tvEstado)
            val eliminar: ImageButton = v.findViewById(R.id.btnEliminar)
        }
    }

    companion object {
        private const val KEY_FOTO = "foto_en_curso"
    }
}
