package com.systemapp.daily.ui.macro

import android.app.Application
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
import java.text.SimpleDateFormat
import java.util.*

class MacroViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val macroDao = db.macroDao()
    private val syncQueueDao = db.syncQueueDao()
    private val networkMonitor = NetworkMonitor.getInstance(application)
    private val api = RetrofitClient.apiService

    val pendientes: LiveData<List<MacroEntity>> = macroDao.getPendientes().flowOn(Dispatchers.IO).asLiveData()
    val ejecutados: LiveData<List<MacroEntity>> = macroDao.getEjecutados().flowOn(Dispatchers.IO).asLiveData()

    // For lectura flow
    private val _orden = MutableLiveData<MacroEntity?>()
    val orden: LiveData<MacroEntity?> = _orden

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
            _orden.postValue(macroDao.getById(idOrden))
        }
    }

    fun validarYGuardar(
        idOrden: Int,
        lecturaActual: String,
        observacion: String?,
        fotoPaths: List<String>,
        latitud: Double?,
        longitud: Double?
    ) {
        _isLoading.value = true
        _saveResult.value = null
        val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        viewModelScope.launch(Dispatchers.IO) {
            // 1. Update local entity to EJECUTADO
            val orden = macroDao.getById(idOrden) ?: run {
                _saveResult.postValue(SaveResult.Error("Orden no encontrada"))
                _isLoading.postValue(false)
                return@launch
            }

            val updated = orden.copy(
                estado = EstadoOrden.EJECUTADO,
                lecturaActual = lecturaActual,
                observacion = observacion,
                rutaFotos = fotoPaths.joinToString(","),
                gpsLatitudLectura = latitud,
                gpsLongitudLectura = longitud,
                fechaLectura = dateTimeFormat.format(Date()),
                sincronizado = false
            )
            macroDao.update(updated)

            // 2. Try immediate send
            val sessionManager = SessionManager(getApplication())
            val apiToken = sessionManager.apiToken
            if (apiToken != null && networkMonitor.isConnected()) {
                try {
                    val textPlain = "text/plain".toMediaTypeOrNull()
                    val fotoParts = fotoPaths.mapIndexedNotNull { index, path ->
                        val file = File(path)
                        if (file.exists()) {
                            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                            MultipartBody.Part.createFormData("fotos[$index]", file.name, requestFile)
                        } else null
                    }

                    val response = api.enviarMacro(
                        apiToken = apiToken.toRequestBody("text/plain".toMediaTypeOrNull()),
                        idOrden = idOrden.toString().toRequestBody(textPlain),
                        lecturaActual = lecturaActual.toRequestBody(textPlain),
                        observacion = observacion?.toRequestBody(textPlain),
                        gpsLatitud = latitud?.toString()?.toRequestBody(textPlain),
                        gpsLongitud = longitud?.toString()?.toRequestBody(textPlain),
                            fotos = fotoParts
                    )

                    if (response.isSuccessful && response.body()?.success == true) {
                        macroDao.marcarSincronizado(idOrden)
                        _saveResult.postValue(SaveResult.Success("Enviado correctamente"))
                        _isLoading.postValue(false)
                        return@launch
                    }
                } catch (_: Exception) { }
            }

            // 3. If send failed or no internet, queue for sync
            val ahora = dateTimeFormat.format(Date())
            val existente = syncQueueDao.buscarPorRegistro(TipoSync.LECTURA, idOrden)
            if (existente == null) {
                syncQueueDao.insert(SyncQueueEntity(
                    tipo = TipoSync.LECTURA,
                    registroId = idOrden,
                    estado = EstadoSync.PENDIENTE,
                    fechaCreacion = ahora
                ))
            }
            SyncWorker.ejecutarSincronizacionInmediata(getApplication())
            _saveResult.postValue(SaveResult.SavedLocal("Sin conexión. Se enviará automáticamente."))
            _isLoading.postValue(false)
        }
    }

    suspend fun getSyncStatus(idOrden: Int): String? {
        return syncQueueDao.getEstadoSync(TipoSync.LECTURA, idOrden)
    }
}
