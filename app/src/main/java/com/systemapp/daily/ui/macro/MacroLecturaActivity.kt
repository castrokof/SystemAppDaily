package com.systemapp.daily.ui.macro

import android.Manifest
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
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.systemapp.daily.R
import com.systemapp.daily.data.location.GpsLocationManager
import com.systemapp.daily.databinding.ActivityMacroLecturaBinding
import com.systemapp.daily.ui.lectura.FotoAdapter
import com.systemapp.daily.utils.Constants
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MacroLecturaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMacroLecturaBinding
    private val viewModel: MacroViewModel by viewModels()
    private lateinit var fotoAdapter: FotoAdapter
    private lateinit var gpsManager: GpsLocationManager

    private var idOrden: Int = -1
    private var gpsLatitud: Double? = null
    private var gpsLongitud: Double? = null

    // Ruta del archivo de foto actual (para la camara del sistema)
    private var currentPhotoPath: String? = null

    // Modo auto-foto: se activa cuando el usuario toca Validar sin tener fotos suficientes
    private var autoFotoMode = false

    /**
     * Usa la camara del sistema (ACTION_IMAGE_CAPTURE) via TakePicture().
     * Al retornar, agrega la foto al ViewModel (igual que en revisiones).
     */
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val photoFile = currentPhotoPath?.let { File(it) }
        val photoSaved = success || (photoFile != null && photoFile.exists() && photoFile.length() > 0)

        if (photoSaved && currentPhotoPath != null) {
            // Guardar en ViewModel (sobrevive recreacion de activity)
            viewModel.agregarFoto(currentPhotoPath!!)

            if (autoFotoMode) {
                val fotosSize = viewModel.fotos.value?.size ?: 0
                if (fotosSize < Constants.MIN_FOTOS_POR_LECTURA) {
                    val faltantes = Constants.MIN_FOTOS_POR_LECTURA - fotosSize
                    Toast.makeText(
                        this,
                        "Foto $fotosSize/${Constants.MIN_FOTOS_POR_LECTURA} - Tome $faltantes mas",
                        Toast.LENGTH_SHORT
                    ).show()
                    abrirCamara()
                } else {
                    autoFotoMode = false
                    validarYGuardar()
                }
            }
        } else {
            if (autoFotoMode) {
                autoFotoMode = false
                val fotosSize = viewModel.fotos.value?.size ?: 0
                if (fotosSize < Constants.MIN_FOTOS_POR_LECTURA) {
                    Toast.makeText(
                        this,
                        "Se necesitan ${Constants.MIN_FOTOS_POR_LECTURA} fotos para guardar",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            currentPhotoPath?.let { path ->
                val file = File(path)
                if (file.exists() && file.length() == 0L) file.delete()
            }
            currentPhotoPath = null
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) abrirCamara()
        else Toast.makeText(this, "Se requiere permiso de camara", Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val STATE_CURRENT_PHOTO_PATH = "state_current_photo_path"
        private const val STATE_AUTO_FOTO_MODE = "state_auto_foto_mode"
        private const val STATE_FOTO_PATHS = "state_foto_paths"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMacroLecturaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        gpsManager = GpsLocationManager.getInstance(this)
        idOrden = intent.getIntExtra(Constants.EXTRA_ORDEN_ID, -1)

        if (idOrden == -1) {
            Toast.makeText(this, "Orden no valida", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        savedInstanceState?.let {
            currentPhotoPath = it.getString(STATE_CURRENT_PHOTO_PATH)
            autoFotoMode = it.getBoolean(STATE_AUTO_FOTO_MODE, false)
            // Restaurar fotos si el ViewModel fue destruido (process death)
            val savedFotos = it.getStringArrayList(STATE_FOTO_PATHS)
            if (savedFotos != null && (viewModel.fotos.value.isNullOrEmpty())) {
                savedFotos.forEach { path -> viewModel.agregarFoto(path) }
            }
        }

        setupUI()
        setupFotos()
        observeViewModel()
        viewModel.cargarOrden(idOrden)
        obtenerGps()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_CURRENT_PHOTO_PATH, currentPhotoPath)
        outState.putBoolean(STATE_AUTO_FOTO_MODE, autoFotoMode)
        // Guardar fotos como respaldo por si el ViewModel se pierde
        outState.putStringArrayList(STATE_FOTO_PATHS, ArrayList(viewModel.fotos.value ?: emptyList()))
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Lectura Macro"
        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.btnTomarFoto.setOnClickListener {
            val fotosSize = viewModel.fotos.value?.size ?: 0
            if (fotosSize >= Constants.MAX_FOTOS_POR_LECTURA) {
                Toast.makeText(this, "Maximo ${Constants.MAX_FOTOS_POR_LECTURA} fotos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            checkCameraPermissionAndOpen()
        }

        binding.btnValidar.setOnClickListener {
            val lectura = binding.etLectura.text.toString().trim()
            if (lectura.isEmpty()) {
                binding.tilLectura.error = "Ingrese el valor de lectura"
                return@setOnClickListener
            }
            binding.tilLectura.error = null

            val fotosSize = viewModel.fotos.value?.size ?: 0
            if (fotosSize < Constants.MIN_FOTOS_POR_LECTURA) {
                autoFotoMode = true
                val faltantes = Constants.MIN_FOTOS_POR_LECTURA - fotosSize
                Toast.makeText(this, "Tome $faltantes foto(s)", Toast.LENGTH_SHORT).show()
                checkCameraPermissionAndOpen()
                return@setOnClickListener
            }

            validarYGuardar()
        }

        binding.etLectura.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateValidarButton()
            }
        })

        updateFotoCount()
        updateValidarButton()
    }

    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> abrirCamara()
            else -> permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun setupFotos() {
        // Inicializar con lista vacia, el observer de fotos la actualizara
        fotoAdapter = FotoAdapter(mutableListOf()) { index ->
            AlertDialog.Builder(this)
                .setTitle("Eliminar foto")
                .setMessage("Eliminar esta foto?")
                .setPositiveButton("Si") { _, _ ->
                    viewModel.eliminarFoto(index)
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
        // Observer de fotos (igual que en revisiones) - unica fuente de verdad
        viewModel.fotos.observe(this) { fotos ->
            fotoAdapter.updateFotos(fotos)
            updateFotoCount()
            updateValidarButton()
        }

        viewModel.orden.observe(this) { orden ->
            if (orden != null) {
                binding.tvOrdenCodigo.text = "Macro: ${orden.codigoMacro}"
                binding.tvOrdenUbicacion.text = orden.ubicacion ?: "Sin ubicacion"
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
                        "%.6f, %.6f (%.0fm)",
                        result.location.latitude, result.location.longitude, result.location.accuracy
                    )
                    binding.tvGpsStatus.setTextColor(getColor(R.color.success))
                }
                is GpsLocationManager.GpsResult.LowAccuracy -> {
                    gpsLatitud = result.location.latitude
                    gpsLongitud = result.location.longitude
                    binding.tvGpsStatus.text = String.format(
                        "%.6f, %.6f (%.0fm - baja precision)",
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
        val photoFile = createPhotoFile() ?: run {
            Toast.makeText(this, "Error al crear archivo de foto", Toast.LENGTH_SHORT).show()
            autoFotoMode = false
            return
        }
        currentPhotoPath = photoFile.absolutePath
        val photoUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile)
        takePictureLauncher.launch(photoUri)
    }

    private fun createPhotoFile(): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir("Pictures") ?: filesDir
        if (!storageDir.exists()) storageDir.mkdirs()
        return try {
            File.createTempFile("MACRO_${timeStamp}_", ".jpg", storageDir)
        } catch (e: Exception) {
            null
        }
    }

    private fun updateFotoCount() {
        val count = viewModel.fotos.value?.size ?: 0
        val min = Constants.MIN_FOTOS_POR_LECTURA
        binding.tvFotoCount.text = "Fotos: $count / $min minimo"
        binding.tvFotoCount.setTextColor(
            getColor(if (count >= min) android.R.color.holo_green_dark else android.R.color.holo_red_dark)
        )
    }

    private fun updateValidarButton() {
        val lecturaOk = binding.etLectura.text.toString().trim().isNotEmpty()
        val fotosSize = viewModel.fotos.value?.size ?: 0
        val fotosOk = fotosSize >= Constants.MIN_FOTOS_POR_LECTURA

        binding.btnValidar.isEnabled = lecturaOk

        binding.btnValidar.text = if (lecturaOk && !fotosOk) {
            "Tomar Fotos y Guardar"
        } else {
            "Validar y Guardar"
        }
    }

    private fun validarYGuardar() {
        val lectura = binding.etLectura.text.toString().trim()
        val observacion = binding.etObservacion.text.toString().trim()

        if (lectura.isEmpty()) {
            binding.tilLectura.error = "Ingrese el valor de lectura"
            return
        }
        binding.tilLectura.error = null

        val fotoPaths = viewModel.fotos.value ?: emptyList()
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
                    .setMessage("La lectura ($lectura) es menor que la anterior ($lecturaAnterior). Continuar?")
                    .setPositiveButton("Si") { _, _ -> ejecutarGuardado(lectura, observacion) }
                    .setNegativeButton("No", null)
                    .show()
                return
            }
        }

        ejecutarGuardado(lectura, observacion)
    }

    private fun ejecutarGuardado(lectura: String, observacion: String) {
        val fotoPaths = viewModel.fotos.value ?: emptyList()
        AlertDialog.Builder(this)
            .setTitle("Confirmar lectura")
            .setMessage("Macro: ${viewModel.orden.value?.codigoMacro}\nLectura: $lectura\nFotos: ${fotoPaths.size}\n\nValidar y guardar?")
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
