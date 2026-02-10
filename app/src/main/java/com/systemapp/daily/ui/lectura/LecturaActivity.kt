package com.systemapp.daily.ui.lectura

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.systemapp.daily.databinding.ActivityLecturaBinding
import com.systemapp.daily.utils.Constants
import com.systemapp.daily.utils.NetworkResult
import com.systemapp.daily.utils.SessionManager

class LecturaActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLecturaBinding
    private lateinit var sessionManager: SessionManager
    private lateinit var fotoAdapter: FotoAdapter
    private val viewModel: LecturaViewModel by viewModels()

    private var macroId: Int = -1
    private var macroNombre: String = ""
    private var macroCodigo: String = ""

    // Launcher para la cámara
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val fotoPath = result.data?.getStringExtra(Constants.EXTRA_PHOTO_PATH)
            if (fotoPath != null) {
                viewModel.agregarFoto(fotoPath)
            }
        }
    }

    // Launcher para permiso de cámara
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            abrirCamara()
        } else {
            Toast.makeText(this, "Se requiere permiso de cámara para tomar fotos", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLecturaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        macroId = intent.getIntExtra(Constants.EXTRA_MACRO_ID, -1)
        macroNombre = intent.getStringExtra(Constants.EXTRA_MACRO_NOMBRE) ?: ""
        macroCodigo = intent.getStringExtra(Constants.EXTRA_MACRO_CODIGO) ?: ""

        if (macroId == -1) {
            Toast.makeText(this, "Error: Macro no válido", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        setupRecyclerView()
        observeViewModel()
        verificarPermisoLectura()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Lectura: $macroCodigo"
        supportActionBar?.subtitle = macroNombre

        binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        binding.btnTomarFoto.setOnClickListener {
            verificarPermisoYCapturar()
        }

        binding.btnEnviarLectura.setOnClickListener {
            validarYEnviar()
        }

        actualizarContadorFotos(0)
    }

    private fun setupRecyclerView() {
        fotoAdapter = FotoAdapter(mutableListOf()) { index ->
            confirmarEliminarFoto(index)
        }
        binding.rvFotos.apply {
            layoutManager = LinearLayoutManager(this@LecturaActivity, LinearLayoutManager.HORIZONTAL, false)
            adapter = fotoAdapter
        }
    }

    private fun observeViewModel() {
        // Observar fotos
        viewModel.fotos.observe(this) { fotos ->
            fotoAdapter.updateFotos(fotos)
            actualizarContadorFotos(fotos.size)
        }

        // Observar verificación de lectura
        viewModel.checkResult.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is NetworkResult.Success -> {
                    binding.progressBar.visibility = View.GONE
                    val check = result.data
                    if (!check.puedeLeer) {
                        AlertDialog.Builder(this)
                            .setTitle("Límite alcanzado")
                            .setMessage("Ya has tomado ${check.lecturasHoy} lecturas hoy para este macro. ${check.message ?: "Solicita autorización desde la web."}")
                            .setPositiveButton("Entendido") { _, _ -> finish() }
                            .setCancelable(false)
                            .show()
                    }
                }
                is NetworkResult.Error -> {
                    binding.progressBar.visibility = View.GONE
                    // Permitir continuar si falla la verificación
                }
            }
        }

        // Observar envío de lectura
        viewModel.envioResult.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.btnEnviarLectura.isEnabled = false
                    binding.btnTomarFoto.isEnabled = false
                }
                is NetworkResult.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Lectura enviada correctamente", Toast.LENGTH_LONG).show()
                    finish()
                }
                is NetworkResult.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.btnEnviarLectura.isEnabled = true
                    binding.btnTomarFoto.isEnabled = true
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun verificarPermisoLectura() {
        val apiToken = sessionManager.apiToken ?: return
        viewModel.checkPuedeLeer(apiToken, macroId)
    }

    private fun verificarPermisoYCapturar() {
        if (viewModel.getCantidadFotos() >= Constants.MAX_FOTOS_POR_LECTURA) {
            Toast.makeText(this, "Máximo ${Constants.MAX_FOTOS_POR_LECTURA} fotos por lectura", Toast.LENGTH_SHORT).show()
            return
        }

        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED -> {
                abrirCamara()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                AlertDialog.Builder(this)
                    .setTitle("Permiso de cámara")
                    .setMessage("Se necesita acceso a la cámara para tomar fotos del medidor.")
                    .setPositiveButton("Conceder") { _, _ ->
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun abrirCamara() {
        val intent = Intent(this, CameraActivity::class.java)
        cameraLauncher.launch(intent)
    }

    private fun actualizarContadorFotos(cantidad: Int) {
        binding.tvFotoCount.text = "Fotos: $cantidad / ${Constants.MIN_FOTOS_POR_LECTURA} mínimo"

        val cumpleMinimo = cantidad >= Constants.MIN_FOTOS_POR_LECTURA
        binding.btnEnviarLectura.isEnabled = cumpleMinimo
        binding.tvFotoCount.setTextColor(
            getColor(if (cumpleMinimo) android.R.color.holo_green_dark else android.R.color.holo_red_dark)
        )
    }

    private fun confirmarEliminarFoto(index: Int) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar foto")
            .setMessage("¿Deseas eliminar esta foto?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.eliminarFoto(index)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun validarYEnviar() {
        val valorLectura = binding.etValorLectura.text.toString().trim()
        val observacion = binding.etObservacion.text.toString().trim()

        if (valorLectura.isEmpty()) {
            binding.tilValorLectura.error = "Ingrese el valor de la lectura"
            return
        }
        binding.tilValorLectura.error = null

        if (viewModel.getCantidadFotos() < Constants.MIN_FOTOS_POR_LECTURA) {
            Toast.makeText(
                this,
                "Debe tomar al menos ${Constants.MIN_FOTOS_POR_LECTURA} fotos",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Confirmar lectura")
            .setMessage("Macro: $macroCodigo\nValor: $valorLectura\nFotos: ${viewModel.getCantidadFotos()}\n\n¿Enviar lectura?")
            .setPositiveButton("Enviar") { _, _ ->
                val apiToken = sessionManager.apiToken ?: return@setPositiveButton
                viewModel.enviarLectura(
                    token = apiToken,
                    macroId = macroId,
                    valorLectura = valorLectura,
                    observacion = observacion.ifEmpty { null }
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
