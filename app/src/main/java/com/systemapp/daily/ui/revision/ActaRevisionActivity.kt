package com.systemapp.daily.ui.revision

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.systemapp.daily.data.model.ChecklistItem
import com.systemapp.daily.data.model.EstadoCheck
import com.systemapp.daily.databinding.ActivityActaRevisionBinding
import com.systemapp.daily.utils.Constants
import com.systemapp.daily.data.repository.RevisionRepository
import com.systemapp.daily.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/**
 * Activity para generar el acta de revisión con firmas.
 * Flujo:
 * 1. El inspector firma
 * 2. El cliente firma
 * 3. Se genera el PDF (para enviar al servidor)
 * 4. Se envía al servidor (revisión + acta PDF)
 * 5. Se imprime una copia para el cliente via Bluetooth ESC/POS 58mm
 */
class ActaRevisionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityActaRevisionBinding
    private lateinit var sessionManager: SessionManager
    private val viewModel: RevisionViewModel by viewModels()
    private val escPosPrinter = EscPosPrinter()

    private var actaPdfFile: File? = null
    private var cachedChecklistItems: List<ChecklistItem> = emptyList()

    // Datos recibidos del RevisionActivity
    private var medidorId: Int = 0
    private var medidorNombre: String = ""
    private var medidorCodigo: String = ""
    private var medidorSuscriptor: String? = null
    private var medidorDireccion: String? = null
    private var checklistJson: String = ""
    private var observacion: String? = null
    private var latitud: Double? = null
    private var longitud: Double? = null
    private var fotoPaths: ArrayList<String> = arrayListOf()

    private val bluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            mostrarDialogoImpresoras()
        } else {
            Toast.makeText(this, "Se necesitan permisos Bluetooth para imprimir", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityActaRevisionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessionManager = SessionManager(this)

        // Recuperar datos del intent
        medidorId = intent.getIntExtra(Constants.EXTRA_MACRO_ID, 0)
        medidorNombre = intent.getStringExtra(Constants.EXTRA_MACRO_NOMBRE) ?: ""
        medidorCodigo = intent.getStringExtra(Constants.EXTRA_MACRO_CODIGO) ?: ""
        medidorSuscriptor = intent.getStringExtra(EXTRA_SUSCRIPTOR)
        medidorDireccion = intent.getStringExtra(EXTRA_DIRECCION)
        checklistJson = intent.getStringExtra(EXTRA_CHECKLIST_JSON) ?: ""
        observacion = intent.getStringExtra(EXTRA_OBSERVACION)
        latitud = if (intent.hasExtra(EXTRA_LATITUD)) intent.getDoubleExtra(EXTRA_LATITUD, 0.0) else null
        longitud = if (intent.hasExtra(EXTRA_LONGITUD)) intent.getDoubleExtra(EXTRA_LONGITUD, 0.0) else null
        fotoPaths = intent.getStringArrayListExtra(EXTRA_FOTO_PATHS) ?: arrayListOf()

        setupToolbar()
        setupInfo()
        setupButtons()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Acta de Revisión"
        supportActionBar?.subtitle = "$medidorCodigo - $medidorNombre"
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupInfo() {
        binding.tvActaRef.text = "Ref: $medidorCodigo"
        binding.tvActaSuscriptor.text = "Suscriptor: ${medidorSuscriptor ?: "N/A"}"
        binding.tvActaDireccion.text = "Dirección: ${medidorDireccion ?: "N/A"}"
        binding.tvActaNombre.text = "Nombre: $medidorNombre"
    }

    private fun setupButtons() {
        binding.btnLimpiarFirmaUsuario.setOnClickListener {
            binding.signatureUsuario.clear()
        }

        binding.btnLimpiarFirmaCliente.setOnClickListener {
            binding.signatureCliente.clear()
        }

        binding.btnGenerarActa.setOnClickListener {
            generarYEnviar()
        }

        binding.btnImprimirActa.setOnClickListener {
            solicitarPermisosBluetooth()
        }
    }

    private fun generarYEnviar() {
        // Validar firmas
        if (binding.signatureUsuario.isEmpty()) {
            Toast.makeText(this, "El inspector debe firmar el acta", Toast.LENGTH_SHORT).show()
            return
        }
        if (binding.signatureCliente.isEmpty()) {
            Toast.makeText(this, "El cliente debe firmar el acta", Toast.LENGTH_SHORT).show()
            return
        }

        // Generar PDF para enviar al servidor
        val firmaUsuario = binding.signatureUsuario.getSignatureBitmap()
        val firmaCliente = binding.signatureCliente.getSignatureBitmap()

        cachedChecklistItems = parseChecklist()

        val actaData = ActaPdfGenerator.ActaData(
            empresa = sessionManager.userEmpresa ?: "N/A",
            refMedidor = medidorCodigo,
            suscriptor = medidorSuscriptor,
            direccion = medidorDireccion,
            medidorNombre = medidorNombre,
            checklistItems = cachedChecklistItems,
            observacion = observacion,
            latitud = latitud,
            longitud = longitud,
            usuario = sessionManager.userName ?: sessionManager.userUsuario ?: "Inspector",
            firmaUsuario = firmaUsuario,
            firmaCliente = firmaCliente
        )

        try {
            val generator = ActaPdfGenerator(this)
            actaPdfFile = generator.generarActaPdf(actaData)

            Toast.makeText(this, "Acta generada correctamente", Toast.LENGTH_SHORT).show()

            // Enviar revisión + acta al servidor
            enviarConActa()

        } catch (e: Exception) {
            Toast.makeText(this, "Error generando el acta: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            firmaUsuario.recycle()
            firmaCliente.recycle()
        }
    }

    private fun parseChecklist(): List<ChecklistItem> {
        return try {
            val type = object : TypeToken<List<Map<String, Any>>>() {}.type
            val list: List<Map<String, Any>> = Gson().fromJson(checklistJson, type)
            list.map { map ->
                ChecklistItem(
                    id = map["id"]?.toString() ?: "",
                    categoria = map["categoria"]?.toString() ?: "",
                    descripcion = map["descripcion"]?.toString() ?: "",
                    estado = try {
                        EstadoCheck.valueOf(map["estado"]?.toString() ?: "NO_REVISADO")
                    } catch (e: Exception) {
                        EstadoCheck.NO_REVISADO
                    }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun enviarConActa() {

        val usuario = sessionManager.userUsuario ?: return

        // Agregar el path del PDF a las fotos para enviarlo como archivo adicional
        val allFiles = ArrayList(fotoPaths)
        actaPdfFile?.let { allFiles.add(it.absolutePath) }

        val apiToken = sessionManager.apiToken ?: ""
        viewModel.enviarRevision(

            medidorId = medidorId,
            refMedidor = medidorCodigo,
            suscriptor = medidorSuscriptor,
            direccion = medidorDireccion,
            checklistJson = checklistJson,
            observacion = observacion,
            latitud = latitud,
            longitud = longitud,
            fotoPaths = allFiles,
            usuario = usuario,
            apiToken = apiToken,
        )
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { loading ->
            binding.progressBarActa.visibility = if (loading) View.VISIBLE else View.GONE
            binding.btnGenerarActa.isEnabled = !loading
        }

        viewModel.syncStatus.observe(this) { status ->
            if (status == null) return@observe

            when (status) {
                is RevisionRepository.SyncStatus.EnviadoOk -> {
                    binding.btnGenerarActa.visibility = View.GONE
                    binding.btnImprimirActa.visibility = View.VISIBLE
                    Toast.makeText(this, "Enviado correctamente", Toast.LENGTH_SHORT).show()
                }
                is RevisionRepository.SyncStatus.GuardadoLocal -> {
                    // Se guardó local, se enviará después. Permitir imprimir
                    binding.btnGenerarActa.visibility = View.GONE
                    binding.btnImprimirActa.visibility = View.VISIBLE
                    Toast.makeText(this, status.message, Toast.LENGTH_LONG).show()
                }
                is RevisionRepository.SyncStatus.Error -> {
                    binding.btnGenerarActa.visibility = View.GONE
                    binding.btnImprimirActa.visibility = View.VISIBLE
                    Toast.makeText(this, status.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // ========================================
    // IMPRESIÓN BLUETOOTH ESC/POS 58mm
    // ========================================

    private fun solicitarPermisosBluetooth() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            val permisos = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
            val necesita = permisos.any {
                ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (necesita) {
                bluetoothPermissionLauncher.launch(permisos)
            } else {
                mostrarDialogoImpresoras()
            }
        } else {
            // Android 11 y menores
            mostrarDialogoImpresoras()
        }
    }

    private fun mostrarDialogoImpresoras() {
        val printers = escPosPrinter.getPairedPrinters()

        if (printers.isEmpty()) {
            Toast.makeText(this, "No hay impresoras Bluetooth pareadas. Vincula tu impresora desde Configuración > Bluetooth", Toast.LENGTH_LONG).show()
            return
        }

        val nombres = printers.map { "${it.name ?: "Desconocida"} (${it.address})" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Seleccionar Impresora")
            .setItems(nombres) { _, which ->
                imprimirEnImpresora(printers[which])
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun imprimirEnImpresora(device: BluetoothDevice) {
        // Obtener las firmas como bitmap para imprimir
        val firmaUsuario = binding.signatureUsuario.getSignatureBitmap()
        val firmaCliente = binding.signatureCliente.getSignatureBitmap()

        val ticketData = EscPosPrinter.ActaTicketData(
            empresa = sessionManager.userEmpresa ?: "N/A",
            refMedidor = medidorCodigo,
            suscriptor = medidorSuscriptor,
            direccion = medidorDireccion,
            medidorNombre = medidorNombre,
            checklistItems = cachedChecklistItems.ifEmpty { parseChecklist() },
            observacion = observacion,
            latitud = latitud,
            longitud = longitud,
            usuario = sessionManager.userName ?: sessionManager.userUsuario ?: "Inspector",
            firmaUsuario = firmaUsuario,
            firmaCliente = firmaCliente
        )

        binding.progressBarActa.visibility = View.VISIBLE
        binding.btnImprimirActa.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            val result = escPosPrinter.imprimirActa(device, ticketData)

            withContext(Dispatchers.Main) {
                binding.progressBarActa.visibility = View.GONE
                binding.btnImprimirActa.isEnabled = true
                firmaUsuario.recycle()
                firmaCliente.recycle()

                result.onSuccess {
                    Toast.makeText(this@ActaRevisionActivity, "Copia impresa para el cliente", Toast.LENGTH_SHORT).show()
                }.onFailure { error ->
                    Toast.makeText(this@ActaRevisionActivity, error.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onBackPressed() {
        if (actaPdfFile != null) {
            // Si ya se generó el acta, cerrar el flujo completo
            setResult(RESULT_OK)
            finish()
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        const val EXTRA_SUSCRIPTOR = "extra_suscriptor"
        const val EXTRA_DIRECCION = "extra_direccion"
        const val EXTRA_CHECKLIST_JSON = "extra_checklist_json"
        const val EXTRA_OBSERVACION = "extra_observacion"
        const val EXTRA_LATITUD = "extra_latitud"
        const val EXTRA_LONGITUD = "extra_longitud"
        const val EXTRA_FOTO_PATHS = "extra_foto_paths"
    }
}
