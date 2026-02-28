package com.systemapp.daily.ui.revision_v2

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.*
import com.systemapp.daily.data.local.AppDatabase
import com.systemapp.daily.data.model.*
import com.systemapp.daily.data.api.RetrofitClient
import com.systemapp.daily.data.network.NetworkMonitor
import com.systemapp.daily.data.sync.SyncWorker
import com.systemapp.daily.utils.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class RevisionWizardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val ordenRevisionDao = db.ordenRevisionDao()
    private val hidraulicoDao = db.hidraulicoDao()
    private val syncQueueDao = db.syncQueueDao()
    private val networkMonitor = NetworkMonitor.getInstance(application)


    // Orden being edited
    private val _orden = MutableLiveData<RevisionEntity?>()
    val orden: LiveData<RevisionEntity?> = _orden

    // Step 1 data
    var nombreAtiende: String = ""
    var tipoDocumento: String = ""
    var documento: String = ""
    var motivoRevision: String = ""
    var motivoDetalle: String = ""

    // Step 2 data
    var estadoAcometida: String = ""
    var estadoSellos: String = ""
    var generalidades: String = ""
    var gpsLatitud: Double? = null
    var gpsLongitud: Double? = null

    // Step 3 data
    var numFamilias: Int = 1
    var numPersonas: Int = 0

    // Step 4 data - lectura y censo hidraulico
    var lecturaActual: String = ""

    // Step 4 data - censo hidraulico
    private val _censoItems = MutableLiveData<MutableList<HidraulicoEntity>>(mutableListOf())
    val censoItems: LiveData<MutableList<HidraulicoEntity>> = _censoItems

    // Step 5 data
    private val _fotos = MutableLiveData<MutableList<String>>(mutableListOf())
    val fotos: LiveData<MutableList<String>> = _fotos
    var firmaClientePath: String? = null
    var actaPdfPath: String? = null

    // Save result
    sealed class SaveResult {
        data class Success(val message: String) : SaveResult()
        data class SavedLocal(val message: String) : SaveResult()
        data class Error(val message: String) : SaveResult()
    }
    private val _saveResult = MutableLiveData<SaveResult?>()
    val saveResult: LiveData<SaveResult?> = _saveResult
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun cargarOrden(idOrden: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            _orden.postValue(ordenRevisionDao.getById(idOrden))
        }
    }

    fun agregarPuntoHidraulico(tipoPunto: String, cantidad: Int, estado: String) {
        val list = _censoItems.value ?: mutableListOf()
        list.add(HidraulicoEntity(idRevision = 0, tipoPunto = tipoPunto, cantidad = cantidad, estado = estado))
        _censoItems.value = list
    }

    fun eliminarPuntoHidraulico(index: Int) {
        val list = _censoItems.value ?: mutableListOf()
        if (index in list.indices) {
            list.removeAt(index)
            _censoItems.value = list
        }
    }

    fun agregarFoto(path: String) {
        val list = _fotos.value ?: mutableListOf()
        list.add(path)
        _fotos.value = list
    }

    fun eliminarFoto(index: Int) {
        val list = _fotos.value ?: mutableListOf()
        if (index in list.indices) {
            list.removeAt(index)
            _fotos.value = list
        }
    }

    fun guardarFirma(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            guardarFirmaBlocking(bitmap)
        }
    }

    fun guardarFirmaBlocking(bitmap: Bitmap) {
        val dir = File(getApplication<Application>().filesDir, "firmas")
        dir.mkdirs()
        val file = File(dir, "firma_cliente_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        firmaClientePath = file.absolutePath
    }

    fun generarActaPdf(firmaCliente: Bitmap?) {
        val currentOrden = _orden.value ?: return
        val context = getApplication<Application>()
        val sessionManager = SessionManager(context)
        val censo = _censoItems.value ?: emptyList()

        val actaData = com.systemapp.daily.ui.revision.ActaPdfGenerator.ActaData(
            empresa = sessionManager.userEmpresa ?: "N/A",
            refMedidor = currentOrden.codigoPredio,
            suscriptor = nombreAtiende.ifBlank { null },
            direccion = null,
            medidorNombre = currentOrden.codigoPredio,
            checklistItems = emptyList(),
            observacion = buildString {
                if (estadoAcometida.isNotBlank()) append("Acometida: $estadoAcometida. ")
                if (estadoSellos.isNotBlank()) append("Sellos: $estadoSellos. ")
                if (generalidades.isNotBlank()) append(generalidades)
                if (lecturaActual.isNotBlank()) append(" Lectura: $lecturaActual.")
                if (censo.isNotEmpty()) {
                    append(" Censo: ")
                    append(censo.joinToString(", ") { "${it.tipoPunto} x${it.cantidad} (${it.estado})" })
                }
            }.ifBlank { null },
            latitud = gpsLatitud,
            longitud = gpsLongitud,
            usuario = sessionManager.userName ?: sessionManager.userUsuario ?: "Inspector",
            firmaUsuario = null,
            firmaCliente = firmaCliente
        )

        try {
            val generator = com.systemapp.daily.ui.revision.ActaPdfGenerator(context)
            val pdfFile = generator.generarActaPdf(actaData)
            actaPdfPath = pdfFile.absolutePath
        } catch (_: Exception) { }
    }

    fun finalizar() {
        val currentOrden = _orden.value ?: return
        _isLoading.value = true
        _saveResult.value = null
        val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val fotoPaths = _fotos.value ?: emptyList()

        viewModelScope.launch(Dispatchers.IO) {
            // 1. Update local entity
            val updated = currentOrden.copy(
                estadoOrden = EstadoOrden.EJECUTADO,
                nombreAtiende = nombreAtiende.ifBlank { null },
                tipoDocumento = tipoDocumento.ifBlank { null },
                documento = documento.ifBlank { null },
                motivoRevision = motivoRevision.ifBlank { null },
                motivoDetalle = motivoDetalle.ifBlank { null },
                estadoAcometida = estadoAcometida.ifBlank { null },
                estadoSellos = estadoSellos.ifBlank { null },
                generalidades = generalidades.ifBlank { null },
                numFamilias = numFamilias,
                numPersonas = numPersonas,
                firmaCliente = firmaClientePath,
                rutaFotos = fotoPaths.joinToString(","),
                gpsLatitudPredio = gpsLatitud,
                gpsLongitudPredio = gpsLongitud,
                lecturaActual = lecturaActual.ifBlank { null },
                actaPdfPath = actaPdfPath,
                fechaCierre = dateTimeFormat.format(Date()),
                sincronizado = false
            )
            ordenRevisionDao.update(updated)

            // Save censo hidraulico
            hidraulicoDao.deleteByRevision(currentOrden.idOrden)
            val censo = _censoItems.value ?: emptyList()
            for (item in censo) {
                hidraulicoDao.insert(item.copy(idRevision = currentOrden.idOrden))
            }

            // 2. Try immediate send
            val sessionManager = SessionManager(getApplication())
            val apiToken = sessionManager.apiToken
            if (apiToken != null && networkMonitor.isConnected()) {
                try {
                    val api = RetrofitClient.getApiService(apiToken)
                    val textPlain = "text/plain".toMediaTypeOrNull()
                    val fotoParts = fotoPaths.mapIndexedNotNull { index, path ->
                        val file = File(path)
                        if (file.exists()) {
                            val req = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                            MultipartBody.Part.createFormData("fotos[$index]", file.name, req)
                        } else null
                    }
                    val firmaPart = firmaClientePath?.let { path ->
                        val file = File(path)
                        if (file.exists()) {
                            val req = file.asRequestBody("image/png".toMediaTypeOrNull())
                            MultipartBody.Part.createFormData("firma_cliente", file.name, req)
                        } else null
                    }
                    val censoJson = com.google.gson.Gson().toJson(censo.map {
                        mapOf("tipo_punto" to it.tipoPunto, "cantidad" to it.cantidad, "estado" to it.estado)
                    })

                    // Generate acta PDF if firma exists
                    val actaPdfPart = actaPdfPath?.let { path ->
                        val file = File(path)
                        if (file.exists()) {
                            val req = file.asRequestBody("application/pdf".toMediaTypeOrNull())
                            MultipartBody.Part.createFormData("acta_pdf", file.name, req)
                        } else null
                    }

                    val response = api.enviarRevisionV2(
                        idOrden = currentOrden.idOrden.toString().toRequestBody(textPlain),
                        codigoPredio = currentOrden.codigoPredio.toRequestBody(textPlain),
                        estadoAcometida = estadoAcometida.ifBlank { null }?.toRequestBody(textPlain),
                        estadoSellos = estadoSellos.ifBlank { null }?.toRequestBody(textPlain),
                        nombreAtiende = nombreAtiende.ifBlank { null }?.toRequestBody(textPlain),
                        tipoDocumento = tipoDocumento.ifBlank { null }?.toRequestBody(textPlain),
                        documento = documento.ifBlank { null }?.toRequestBody(textPlain),
                        numFamilias = numFamilias.toString().toRequestBody(textPlain),
                        numPersonas = numPersonas.toString().toRequestBody(textPlain),
                        motivoRevision = motivoRevision.ifBlank { null }?.toRequestBody(textPlain),
                        motivoDetalle = motivoDetalle.ifBlank { null }?.toRequestBody(textPlain),
                        generalidades = generalidades.ifBlank { null }?.toRequestBody(textPlain),
                        censoHidraulicoJson = censoJson.toRequestBody(textPlain),
                        lecturaActual = lecturaActual.ifBlank { null }?.toRequestBody(textPlain),
                        gpsLatitud = gpsLatitud?.toString()?.toRequestBody(textPlain),
                        gpsLongitud = gpsLongitud?.toString()?.toRequestBody(textPlain),
                        fotos = fotoParts,
                        firmaCliente = firmaPart,
                        actaPdf = actaPdfPart,
                    )
                    if (response.isSuccessful && response.body()?.success == true) {
                        ordenRevisionDao.marcarSincronizado(currentOrden.idOrden)
                        _saveResult.postValue(SaveResult.Success("Enviado correctamente"))
                        _isLoading.postValue(false)
                        return@launch
                    }
                } catch (_: Exception) { }
            }

            // 3. Queue for sync
            val ahora = dateTimeFormat.format(Date())
            val existente = syncQueueDao.buscarPorRegistro(TipoSync.REVISION_V2, currentOrden.idOrden)
            if (existente == null) {
                syncQueueDao.insert(SyncQueueEntity(
                    tipo = TipoSync.REVISION_V2,
                    registroId = currentOrden.idOrden,
                    estado = EstadoSync.PENDIENTE,
                    fechaCreacion = ahora
                ))
            }
            SyncWorker.ejecutarSincronizacionInmediata(getApplication())
            _saveResult.postValue(SaveResult.SavedLocal("Sin conexión. Se enviará automáticamente."))
            _isLoading.postValue(false)
        }
    }

    // For ejecutados list
    val pendientes: LiveData<List<RevisionEntity>> = ordenRevisionDao.getPendientes().flowOn(Dispatchers.IO).asLiveData()
    val ejecutados: LiveData<List<RevisionEntity>> = ordenRevisionDao.getEjecutados().flowOn(Dispatchers.IO).asLiveData()

    // Search
    private val _searchQueryPendientes = MutableLiveData("")
    private val _searchQueryEjecutados = MutableLiveData("")

    val pendientesFiltrados: LiveData<List<RevisionEntity>> = _searchQueryPendientes.switchMap { query ->
        if (query.isNullOrBlank()) {
            ordenRevisionDao.getPendientes().flowOn(Dispatchers.IO).asLiveData()
        } else {
            ordenRevisionDao.buscarPendientes(query).flowOn(Dispatchers.IO).asLiveData()
        }
    }

    val ejecutadosFiltrados: LiveData<List<RevisionEntity>> = _searchQueryEjecutados.switchMap { query ->
        if (query.isNullOrBlank()) {
            ordenRevisionDao.getEjecutados().flowOn(Dispatchers.IO).asLiveData()
        } else {
            ordenRevisionDao.buscarEjecutados(query).flowOn(Dispatchers.IO).asLiveData()
        }
    }

    fun buscarPendientes(query: String) {
        _searchQueryPendientes.value = query
    }

    fun buscarEjecutados(query: String) {
        _searchQueryEjecutados.value = query
    }

    suspend fun getSyncStatus(idOrden: Int): String? {
        return syncQueueDao.getEstadoSync(TipoSync.REVISION_V2, idOrden)
    }

    fun retomarRevision(idOrden: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            ordenRevisionDao.retomar(idOrden)
        }
    }
}
