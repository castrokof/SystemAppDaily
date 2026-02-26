package com.systemapp.daily.ui.macro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.systemapp.daily.R
import com.systemapp.daily.data.location.GpsLocationManager
import com.systemapp.daily.databinding.ActivityMacroLecturaBinding
import com.systemapp.daily.ui.lectura.CameraActivity
import com.systemapp.daily.ui.lectura.FotoAdapter
import com.systemapp.daily.utils.Constants
import kotlinx.coroutines.launch

class MacroLecturaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMacroLecturaBinding
    private val viewModel: MacroViewModel by viewModels()
    private lateinit var fotoAdapter: FotoAdapter
    private lateinit var gpsManager: GpsLocationManager

    private var idOrden: Int = -1
    private var gpsLatitud: Double? = null
    private var gpsLongitud: Double? = null
    private val fotoPaths = mutableListOf<String>()

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val path = result.data?.getStringExtra(Constants.EXTRA_PHOTO_PATH)
            if (path != null) {
                fotoPaths.add(path)
                fotoAdapter.updateFotos(fotoPaths)
                updateFotoCount()
                updateValidarButton()
            }
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) abrirCamara()
        else Toast.makeText(this, "Se requiere permiso de cámara", Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val STATE_FOTO_PATHS = "state_foto_paths"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMacroLecturaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        gpsManager = GpsLocationManager.getInstance(this)
        idOrden = intent.getIntExtra(Constants.EXTRA_ORDEN_ID, -1)

        if (idOrden == -1) {
            Toast.makeText(this, "Orden no válida", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Restaurar fotos si la actividad fue recreada (ej. al volver de cámara en dispositivos con poca RAM)
        savedInstanceState?.getStringArrayList(STATE_FOTO_PATHS)?.let { saved ->
            fotoPaths.clear()
            fotoPaths.addAll(saved)
        }

        setupUI()
        setupFotos()
        observeViewModel()
        viewModel.cargarOrden(idOrden)
        obtenerGps()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(STATE_FOTO_PATHS, ArrayList(fotoPaths))
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Lectura Macro"
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.btnTomarFoto.setOnClickListener {
            if (fotoPaths.size >= Constants.MAX_FOTOS_POR_LECTURA) {
                Toast.makeText(this, "Máximo ${Constants.MAX_FOTOS_POR_LECTURA} fotos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> abrirCamara()
                else -> permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        binding.btnValidar.setOnClickListener {
            validarYGuardar()
        }

        // TextWatcher para habilitar el botón Validar cuando se escribe la lectura
        binding.etLectura.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateValidarButton()
            }
        })

        updateFotoCount()
    }

    private fun setupFotos() {
        fotoAdapter = FotoAdapter(fotoPaths) { index ->
            AlertDialog.Builder(this)
                .setTitle("Eliminar foto")
                .setMessage("¿Eliminar esta foto?")
                .setPositiveButton("Sí") { _, _ ->
                    fotoPaths.removeAt(index)
                    fotoAdapter.notifyItemRemoved(index)
                    fotoAdapter.notifyItemRangeChanged(index, fotoPaths.size)
                    updateFotoCount()
                    updateValidarButton()
                }
                .setNegativeButton("No", null)
                .show()
        }
        binding.rvFotos.apply {
            layoutManager = LinearLayoutManager(this@MacroLecturaActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = fotoAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.orden.observe(this) { orden ->
            if (orden != null) {
                binding.tvOrdenCodigo.text = "Macro: ${orden.codigoMacro}"
                binding.tvOrdenUbicacion.text = orden.ubicacion ?: "Sin ubicación"
                binding.tvOrdenLecturaAnterior.text = "Lectura anterior: ${orden.lecturaAnterior ?: "-"}"
                supportActionBar?.subtitle = orden.codigoMacro
            }
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnValidar.isEnabled = !loading
            binding.btnTomarFoto.isEnabled = !loading
        }

        viewModel.saveResult.observe(this) { result ->
            if (result == null) return@observe
            when (result) {
                is MacroViewModel.SaveResult.Success -> {
                    Snackbar.make(binding.rootLayout, result.message, Snackbar.LENGTH_SHORT)
                        .setBackgroundTint(getColor(R.color.success))
                        .show()
                    binding.rootLayout.postDelayed({ finish() }, 1500)
                }
                is MacroViewModel.SaveResult.SavedLocal -> {
                    Snackbar.make(binding.rootLayout, result.message, Snackbar.LENGTH_LONG)
                        .setBackgroundTint(getColor(R.color.warning))
                        .show()
                    binding.rootLayout.postDelayed({ finish() }, 2000)
                }
                is MacroViewModel.SaveResult.Error -> {
                    Snackbar.make(binding.rootLayout, result.message, Snackbar.LENGTH_LONG)
                        .setBackgroundTint(getColor(R.color.error))
                        .show()
                }
            }
        }
    }

    private fun obtenerGps() {
        if (!gpsManager.hasPermission()) {
            binding.tvGpsStatus.text = "Sin permiso GPS"
            binding.tvGpsStatus.setTextColor(getColor(R.color.error))
            binding.progressGps.visibility = View.GONE
            return
        }

        lifecycleScope.launch {
            when (val result = gpsManager.getCurrentLocation()) {
                is GpsLocationManager.GpsResult.Success -> {
                    gpsLatitud = result.location.latitude
                    gpsLongitud = result.location.longitude
                    binding.tvGpsStatus.text = String.format(
                        "%.6f, %.6f (±%.0fm)",
                        result.location.latitude, result.location.longitude, result.location.accuracy
                    )
                    binding.tvGpsStatus.setTextColor(getColor(R.color.success))
                }
                is GpsLocationManager.GpsResult.LowAccuracy -> {
                    gpsLatitud = result.location.latitude
                    gpsLongitud = result.location.longitude
                    binding.tvGpsStatus.text = String.format(
                        "%.6f, %.6f (±%.0fm - baja precisión)",
                        result.location.latitude, result.location.longitude, result.location.accuracy
                    )
                    binding.tvGpsStatus.setTextColor(getColor(R.color.warning))
                }
                is GpsLocationManager.GpsResult.Error -> {
                    binding.tvGpsStatus.text = result.message
                    binding.tvGpsStatus.setTextColor(getColor(R.color.error))
                }
            }
            binding.progressGps.visibility = View.GONE
        }
    }

    private fun abrirCamara() {
        cameraLauncher.launch(Intent(this, CameraActivity::class.java))
    }

    private fun updateFotoCount() {
        val count = fotoPaths.size
        val min = Constants.MIN_FOTOS_POR_LECTURA
        binding.tvFotoCount.text = "Fotos: $count / $min mínimo"
        binding.tvFotoCount.setTextColor(
            getColor(if (count >= min) android.R.color.holo_green_dark else android.R.color.holo_red_dark)
        )
    }

    private fun updateValidarButton() {
        val lecturaOk = binding.etLectura.text.toString().trim().isNotEmpty()
        val fotosOk = fotoPaths.size >= Constants.MIN_FOTOS_POR_LECTURA
        binding.btnValidar.isEnabled = lecturaOk && fotosOk
    }

    private fun validarYGuardar() {
        val lectura = binding.etLectura.text.toString().trim()
        val observacion = binding.etObservacion.text.toString().trim()

        if (lectura.isEmpty()) {
            binding.tilLectura.error = "Ingrese el valor de lectura"
            return
        }
        binding.tilLectura.error = null

        if (fotoPaths.size < Constants.MIN_FOTOS_POR_LECTURA) {
            Toast.makeText(this, "Debe tomar al menos ${Constants.MIN_FOTOS_POR_LECTURA} fotos", Toast.LENGTH_LONG).show()
            return
        }

        val lecturaAnterior = viewModel.orden.value?.lecturaAnterior
        if (lecturaAnterior != null) {
            val lecturaInt = lectura.toIntOrNull()
            if (lecturaInt != null && lecturaInt < lecturaAnterior) {
                AlertDialog.Builder(this)
                    .setTitle("Lectura menor")
                    .setMessage("La lectura ($lectura) es menor que la anterior ($lecturaAnterior). ¿Continuar?")
                    .setPositiveButton("Sí") { _, _ -> ejecutarGuardado(lectura, observacion) }
                    .setNegativeButton("No", null)
                    .show()
                return
            }
        }

        ejecutarGuardado(lectura, observacion)
    }

    private fun ejecutarGuardado(lectura: String, observacion: String) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar lectura")
            .setMessage("Macro: ${viewModel.orden.value?.codigoMacro}\nLectura: $lectura\nFotos: ${fotoPaths.size}\n\n¿Validar y guardar?")
            .setPositiveButton("Validar") { _, _ ->
                viewModel.validarYGuardar(
                    idOrden = idOrden,
                    lecturaActual = lectura,
                    observacion = observacion.ifEmpty { null },
                    fotoPaths = fotoPaths,
                    latitud = gpsLatitud,
                    longitud = gpsLongitud
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
