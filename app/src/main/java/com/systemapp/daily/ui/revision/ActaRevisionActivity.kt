package com.systemapp.daily.ui.revision

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.systemapp.daily.R
import com.systemapp.daily.data.model.ChecklistItem
import com.systemapp.daily.data.model.EstadoCheck
import com.systemapp.daily.databinding.ActivityActaRevisionBinding
import com.systemapp.daily.utils.Constants
import com.systemapp.daily.utils.NetworkResult
import com.systemapp.daily.utils.SessionManager
import java.io.File
import java.io.FileInputStream
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintDocumentInfo

/**
 * Activity para generar el acta de revisión con firmas.
 * Flujo:
 * 1. El inspector firma
 * 2. El cliente firma
 * 3. Se genera el PDF
 * 4. Se envía al servidor (revisión + acta PDF)
 * 5. Se imprime una copia para el cliente
 */
class ActaRevisionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityActaRevisionBinding
    private lateinit var sessionManager: SessionManager
    private val viewModel: RevisionViewModel by viewModels()

    private var actaPdfFile: File? = null

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
            imprimirActa()
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

        // Generar PDF
        val firmaUsuario = binding.signatureUsuario.getSignatureBitmap()
        val firmaCliente = binding.signatureCliente.getSignatureBitmap()

        val checklistItems = parseChecklist()

        val actaData = ActaPdfGenerator.ActaData(
            empresa = sessionManager.userEmpresa ?: "N/A",
            refMedidor = medidorCodigo,
            suscriptor = medidorSuscriptor,
            direccion = medidorDireccion,
            medidorNombre = medidorNombre,
            checklistItems = checklistItems,
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
        val apiToken = sessionManager.apiToken ?: return
        val usuario = sessionManager.userUsuario ?: return

        // Agregar el path del PDF a las fotos para enviarlo como archivo adicional
        val allFiles = ArrayList(fotoPaths)
        actaPdfFile?.let { allFiles.add(it.absolutePath) }

        viewModel.enviarRevision(
            apiToken = apiToken,
            medidorId = medidorId,
            refMedidor = medidorCodigo,
            suscriptor = medidorSuscriptor,
            direccion = medidorDireccion,
            checklistJson = checklistJson,
            observacion = observacion,
            latitud = latitud,
            longitud = longitud,
            fotoPaths = allFiles,
            usuario = usuario
        )
    }

    private fun observeViewModel() {
        viewModel.envioResult.observe(this) { result ->
            when (result) {
                is NetworkResult.Loading -> {
                    binding.progressBarActa.visibility = View.VISIBLE
                    binding.btnGenerarActa.isEnabled = false
                }
                is NetworkResult.Success -> {
                    binding.progressBarActa.visibility = View.GONE
                    binding.btnGenerarActa.visibility = View.GONE
                    binding.btnImprimirActa.visibility = View.VISIBLE
                    Toast.makeText(this, "Revisión y acta enviadas al servidor", Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.Error -> {
                    binding.progressBarActa.visibility = View.GONE
                    // Aun si falla el envío, permitir imprimir (se guardó localmente)
                    binding.btnGenerarActa.visibility = View.GONE
                    binding.btnImprimirActa.visibility = View.VISIBLE
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun imprimirActa() {
        val pdfFile = actaPdfFile
        if (pdfFile == null || !pdfFile.exists()) {
            Toast.makeText(this, "No se encontró el acta PDF", Toast.LENGTH_SHORT).show()
            return
        }

        val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "Acta_Revision_${medidorCodigo}"

        val printAdapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback.onLayoutCancelled()
                    return
                }

                val info = PrintDocumentInfo.Builder(jobName)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                    .build()

                callback.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback
            ) {
                try {
                    FileInputStream(pdfFile).use { input ->
                        ParcelFileDescriptor.AutoCloseOutputStream(destination).use { output ->
                            input.copyTo(output)
                        }
                    }

                    if (cancellationSignal?.isCanceled == true) {
                        callback.onWriteCancelled()
                    } else {
                        callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    }
                } catch (e: Exception) {
                    callback.onWriteFailed(e.message)
                }
            }
        }

        val printAttributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        printManager.print(jobName, printAdapter, printAttributes)

        // Después de imprimir, cerrar todo el flujo
        Toast.makeText(this, "Imprimiendo copia para el cliente", Toast.LENGTH_SHORT).show()
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
