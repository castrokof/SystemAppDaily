package com.systemapp.daily.ui.revision

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.systemapp.daily.R
import com.systemapp.daily.data.model.ChecklistAcueducto
import com.systemapp.daily.data.model.ChecklistItem
import com.systemapp.daily.data.model.EstadoCheck
import com.systemapp.daily.databinding.ActivityRevisionBinding
import com.systemapp.daily.ui.lectura.CameraActivity
import com.systemapp.daily.ui.lectura.FotoAdapter
import com.systemapp.daily.utils.Constants
import com.systemapp.daily.utils.NetworkResult
import com.systemapp.daily.utils.SessionManager

class RevisionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRevisionBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var checklistAdapter: ChecklistAdapter
    private lateinit var fotoAdapter: FotoAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val viewModel: RevisionViewModel by viewModels()

    private val checklistItems = ChecklistAcueducto.getChecklist().toMutableList()
    private val fotoPaths = mutableListOf<String>()
    private var currentLatitud: Double? = null
    private var currentLongitud: Double? = null

    private var medidorId: Int = 0
    private var medidorNombre: String = ""
    private var medidorCodigo: String = ""
    private var medidorSuscriptor: String? = null
    private var medidorDireccion: String? = null

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val photoPath = result.data?.getStringExtra(Constants.EXTRA_PHOTO_PATH)
            if (photoPath != null) {
                fotoPaths.add(photoPath)
                fotoAdapter.notifyItemInserted(fotoPaths.size - 1)
                updateFotoCount()
                updateEnviarButton()
            }
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

        setupToolbar()
        setupChecklist()
        setupFotos()
        setupButtons()
        observeViewModel()
        solicitarUbicacion()
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
            cameraLauncher.launch(Intent(this, CameraActivity::class.java))
        }

        binding.btnEnviarRevision.setOnClickListener {
            enviarRevision()
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
        // Verificar que al menos se hayan tomado las fotos mínimas
        if (fotoPaths.size < Constants.MIN_FOTOS_POR_LECTURA) {
            Toast.makeText(this, "Debe tomar al menos ${Constants.MIN_FOTOS_POR_LECTURA} fotos", Toast.LENGTH_SHORT).show()
            return
        }

        // Verificar que al menos un ítem del checklist fue revisado
        val revisados = checklistItems.count { it.estado != EstadoCheck.NO_REVISADO }
        if (revisados == 0) {
            Toast.makeText(this, "Debe revisar al menos un ítem del checklist", Toast.LENGTH_SHORT).show()
            return
        }

        val apiToken = sessionManager.apiToken ?: return
        val usuario = sessionManager.userUsuario ?: return
        val observacion = binding.etObservacionRevision.text.toString().trim().ifEmpty { null }

        // Convertir checklist a JSON
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

        viewModel.enviarRevision(
            apiToken = apiToken,
            medidorId = medidorId,
            refMedidor = medidorCodigo,
            suscriptor = medidorSuscriptor,
            direccion = medidorDireccion,
            checklistJson = checklistJson,
            observacion = observacion,
            latitud = currentLatitud,
            longitud = currentLongitud,
            fotoPaths = fotoPaths,
            usuario = usuario
        )
    }

    private fun observeViewModel() {
        viewModel.envioResult.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    binding.progressBarRevision.visibility = View.VISIBLE
                    binding.btnEnviarRevision.isEnabled = false
                }
                is NetworkResult.Success -> {
                    binding.progressBarRevision.visibility = View.GONE
                    Toast.makeText(this, "Revisión enviada correctamente", Toast.LENGTH_SHORT).show()
                    finish()
                }
                is NetworkResult.Error -> {
                    binding.progressBarRevision.visibility = View.GONE
                    updateEnviarButton()
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    companion object {
        const val EXTRA_SUSCRIPTOR = "extra_suscriptor"
        const val EXTRA_DIRECCION = "extra_direccion"
    }
}
