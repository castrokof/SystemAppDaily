package com.systemapp.daily.ui.revision

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.systemapp.daily.R
import com.systemapp.daily.data.model.ChecklistAcueducto
import com.systemapp.daily.data.model.EstadoCheck
import com.systemapp.daily.databinding.ActivityRevisionBinding
import com.systemapp.daily.ui.lectura.FotoAdapter
import com.systemapp.daily.utils.Constants
import com.systemapp.daily.utils.SessionManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RevisionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRevisionBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var checklistAdapter: ChecklistAdapter
    private lateinit var fotoAdapter: FotoAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val checklistItems = ChecklistAcueducto.getChecklist().toMutableList()
    private val fotoPaths = mutableListOf<String>()
    private var currentLatitud: Double? = null
    private var currentLongitud: Double? = null
    private var currentPhotoPath: String? = null

    private var medidorId: Int = 0
    private var medidorNombre: String = ""
    private var medidorCodigo: String = ""
    private var medidorSuscriptor: String? = null
    private var medidorDireccion: String? = null

    // Usa la camara del sistema (TakePicture) en vez de CameraX para mayor compatibilidad
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val photoFile = currentPhotoPath?.let { File(it) }
        val photoSaved = success || (photoFile != null && photoFile.exists() && photoFile.length() > 0)

        if (photoSaved && currentPhotoPath != null) {
            fotoPaths.add(currentPhotoPath!!)
            fotoAdapter.updateFotos(fotoPaths)
            updateFotoCount()
            updateEnviarButton()
        } else {
            currentPhotoPath?.let { path ->
                val file = File(path)
                if (file.exists() && file.length() == 0L) file.delete()
            }
            currentPhotoPath = null
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) abrirCamara()
        else Toast.makeText(this, "Se requiere permiso de cámara para tomar fotos", Toast.LENGTH_LONG).show()
    }

    private val actaLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            finish()
        }
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            obtenerUbicacion()
        }
    }

    companion object {
        const val EXTRA_SUSCRIPTOR = "extra_suscriptor"
        const val EXTRA_DIRECCION = "extra_direccion"
        private const val STATE_FOTO_PATHS = "state_foto_paths"
        private const val STATE_CURRENT_PHOTO_PATH = "state_current_photo_path"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRevisionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        medidorId = intent.getIntExtra(Constants.EXTRA_MACRO_ID, 0)
        medidorNombre = intent.getStringExtra(Constants.EXTRA_MACRO_NOMBRE) ?: ""
        medidorCodigo = intent.getStringExtra(Constants.EXTRA_MACRO_CODIGO) ?: ""
        medidorSuscriptor = intent.getStringExtra(EXTRA_SUSCRIPTOR)
        medidorDireccion = intent.getStringExtra(EXTRA_DIRECCION)

        savedInstanceState?.let {
            it.getStringArrayList(STATE_FOTO_PATHS)?.let { saved ->
                fotoPaths.clear()
                fotoPaths.addAll(saved)
            }
            currentPhotoPath = it.getString(STATE_CURRENT_PHOTO_PATH)
        }

        setupToolbar()
        setupChecklist()
        setupFotos()
        setupButtons()
        solicitarUbicacion()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(STATE_FOTO_PATHS, ArrayList(fotoPaths))
        outState.putString(STATE_CURRENT_PHOTO_PATH, currentPhotoPath)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Revisión"
        supportActionBar?.subtitle = "$medidorCodigo - $medidorNombre"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupChecklist() {
        checklistAdapter = ChecklistAdapter(checklistItems)
        binding.rvChecklist.apply {
            layoutManager = LinearLayoutManager(this@RevisionActivity)
            adapter = checklistAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupFotos() {
        fotoAdapter = FotoAdapter(fotoPaths) { position ->
            fotoPaths.removeAt(position)
            fotoAdapter.notifyItemRemoved(position)
            fotoAdapter.notifyItemRangeChanged(position, fotoPaths.size)
            updateFotoCount()
            updateEnviarButton()
        }
        binding.rvFotosRevision.apply {
            layoutManager = LinearLayoutManager(this@RevisionActivity, RecyclerView.HORIZONTAL, false)
            adapter = fotoAdapter
        }
        updateFotoCount()
    }

    private fun setupButtons() {
        binding.btnTomarFotoRevision.setOnClickListener {
            if (fotoPaths.size >= Constants.MAX_FOTOS_POR_LECTURA) {
                Toast.makeText(this, "Máximo ${Constants.MAX_FOTOS_POR_LECTURA} fotos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            checkCameraPermissionAndOpen()
        }

        binding.btnEnviarRevision.setOnClickListener {
            enviarRevision()
        }
    }

    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> abrirCamara()
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun abrirCamara() {
        val photoFile = createPhotoFile() ?: run {
            Toast.makeText(this, "Error al crear archivo de foto", Toast.LENGTH_SHORT).show()
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
            File.createTempFile("REVISION_${timeStamp}_", ".jpg", storageDir)
        } catch (e: Exception) {
            null
        }
    }

    private fun solicitarUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            obtenerUbicacion()
        }
    }

    private fun obtenerUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    currentLatitud = location.latitude
                    currentLongitud = location.longitude
                    binding.tvUbicacion.text = "GPS: ${String.format("%.6f", location.latitude)}, ${String.format("%.6f", location.longitude)}"
                    binding.tvUbicacion.setTextColor(getColor(R.color.success))
                } else {
                    binding.tvUbicacion.text = "GPS: No disponible"
                    binding.tvUbicacion.setTextColor(getColor(R.color.warning))
                }
            }
        }
    }

    private fun updateFotoCount() {
        binding.tvFotoCountRevision.text = "${fotoPaths.size} / ${Constants.MAX_FOTOS_POR_LECTURA} fotos"
    }

    private fun updateEnviarButton() {
        binding.btnEnviarRevision.isEnabled = fotoPaths.size >= Constants.MIN_FOTOS_POR_LECTURA
    }

    private fun enviarRevision() {
        if (fotoPaths.size < Constants.MIN_FOTOS_POR_LECTURA) {
            Toast.makeText(this, "Debe tomar al menos ${Constants.MIN_FOTOS_POR_LECTURA} fotos", Toast.LENGTH_SHORT).show()
            return
        }

        val revisados = checklistItems.count { it.estado != EstadoCheck.NO_REVISADO }
        if (revisados == 0) {
            Toast.makeText(this, "Debe revisar al menos un ítem del checklist", Toast.LENGTH_SHORT).show()
            return
        }

        val observacion = binding.etObservacionRevision.text.toString().trim().ifEmpty { null }

        val checklistData = checklistItems.map { item ->
            mapOf(
                "id" to item.id,
                "categoria" to item.categoria,
                "descripcion" to item.descripcion,
                "estado" to item.estado.name,
                "estado_label" to item.estado.label
            )
        }
        val checklistJson = Gson().toJson(checklistData)

        val intent = Intent(this, ActaRevisionActivity::class.java).apply {
            putExtra(Constants.EXTRA_MACRO_ID, medidorId)
            putExtra(Constants.EXTRA_MACRO_NOMBRE, medidorNombre)
            putExtra(Constants.EXTRA_MACRO_CODIGO, medidorCodigo)
            putExtra(ActaRevisionActivity.EXTRA_SUSCRIPTOR, medidorSuscriptor)
            putExtra(ActaRevisionActivity.EXTRA_DIRECCION, medidorDireccion)
            putExtra(ActaRevisionActivity.EXTRA_CHECKLIST_JSON, checklistJson)
            putExtra(ActaRevisionActivity.EXTRA_OBSERVACION, observacion)
            if (currentLatitud != null) putExtra(ActaRevisionActivity.EXTRA_LATITUD, currentLatitud!!)
            if (currentLongitud != null) putExtra(ActaRevisionActivity.EXTRA_LONGITUD, currentLongitud!!)
            putStringArrayListExtra(ActaRevisionActivity.EXTRA_FOTO_PATHS, ArrayList(fotoPaths))
        }
        actaLauncher.launch(intent)
    }
}
