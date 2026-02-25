package com.systemapp.daily.data.repository

import android.content.Context
import com.systemapp.daily.data.api.RetrofitClient
import com.systemapp.daily.data.local.AppDatabase
import com.systemapp.daily.data.model.EstadoSync
import com.systemapp.daily.data.model.Revision
import com.systemapp.daily.data.model.RevisionResponse
import com.systemapp.daily.data.model.SyncQueueEntity
import com.systemapp.daily.data.model.TipoSync
import com.systemapp.daily.data.network.NetworkMonitor
import com.systemapp.daily.data.sync.SyncWorker
import com.systemapp.daily.utils.NetworkResult
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RevisionRepository(private val context: Context) {

    private val api = RetrofitClient.apiService
    private val db = AppDatabase.getDatabase(context)
    private val revisionDao = db.revisionDao()
    private val syncQueueDao = db.syncQueueDao()
    private val networkMonitor = NetworkMonitor.getInstance(context)
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    /**
     * Resultado del envío con info de sync.
     */
    sealed class SyncStatus {
        data class EnviadoOk(val response: RevisionResponse) : SyncStatus()
        data class GuardadoLocal(val message: String) : SyncStatus()
        data class Error(val message: String) : SyncStatus()
    }

    /**
     * Guarda revisión localmente, intenta enviar inmediatamente.
     * Si no hay red o falla, encola para WorkManager.
     */
    suspend fun enviarRevision(
        apiToken: String,
        medidorId: Int,
        refMedidor: String,
        suscriptor: String?,
        direccion: String?,
        checklistJson: String,
        observacion: String?,
        latitud: Double?,
        longitud: Double?,
        fotoPaths: List<String>,
        usuario: String
    ): SyncStatus {
        // 1. SIEMPRE guardar localmente primero
        val revisionId = guardarRevisionLocal(
            medidorId, refMedidor, suscriptor, direccion,
            checklistJson, observacion, latitud, longitud,
            fotoPaths, usuario, false
        )

        // 2. Si hay conexión, intentar envío inmediato
        if (networkMonitor.isConnected()) {
            try {
                val textPlain = "text/plain".toMediaTypeOrNull()
                val tokenBody = apiToken.toRequestBody(textPlain)
                val medidorIdBody = medidorId.toString().toRequestBody(textPlain)
                val checklistBody = checklistJson.toRequestBody(textPlain)
                val obsBody = observacion?.toRequestBody(textPlain)
                val latBody = latitud?.toString()?.toRequestBody(textPlain)
                val lonBody = longitud?.toString()?.toRequestBody(textPlain)

                val imagePaths = fotoPaths.filter { !it.endsWith(".pdf") }
                val pdfPath = fotoPaths.firstOrNull { it.endsWith(".pdf") }

                val fotoParts = imagePaths.mapIndexed { index, path ->
                    val file = File(path)
                    val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("fotos[$index]", file.name, requestFile)
                }

                val actaPdfPart = pdfPath?.let { path ->
                    val file = File(path)
                    val requestFile = file.asRequestBody("application/pdf".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("acta_pdf", file.name, requestFile)
                }

                val response = api.enviarRevision(
                    apiToken = tokenBody,
                    medidorId = medidorIdBody,
                    checklistJson = checklistBody,
                    observacion = obsBody,
                    latitud = latBody,
                    longitud = lonBody,
                    fotos = fotoParts,
                    actaPdf = actaPdfPart
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    revisionDao.marcarComoSincronizada(revisionId.toInt())
                    return SyncStatus.EnviadoOk(response.body()!!)
                } else {
                    encolarParaSync(revisionId.toInt())
                    return SyncStatus.GuardadoLocal("Error del servidor. Se reintentará automáticamente.")
                }
            } catch (e: Exception) {
                encolarParaSync(revisionId.toInt())
                return SyncStatus.GuardadoLocal("Sin conexión. Se enviará cuando haya internet.")
            }
        } else {
            encolarParaSync(revisionId.toInt())
            return SyncStatus.GuardadoLocal("Sin conexión. Se enviará automáticamente.")
        }
    }

    private suspend fun encolarParaSync(revisionId: Int) {
        val ahora = dateTimeFormat.format(Date())
        val existente = syncQueueDao.buscarPorRegistro(TipoSync.REVISION, revisionId)
        if (existente == null) {
            syncQueueDao.insert(
                SyncQueueEntity(
                    tipo = TipoSync.REVISION,
                    registroId = revisionId,
                    estado = EstadoSync.PENDIENTE,
                    fechaCreacion = ahora
                )
            )
        }
        SyncWorker.ejecutarSincronizacionInmediata(context)
    }

    private suspend fun guardarRevisionLocal(
        medidorId: Int, refMedidor: String, suscriptor: String?, direccion: String?,
        checklistJson: String, observacion: String?,
        latitud: Double?, longitud: Double?,
        fotoPaths: List<String>, usuario: String, sincronizado: Boolean
    ): Long {
        val revision = Revision(
            medidorId = medidorId,
            refMedidor = refMedidor,
            suscriptor = suscriptor,
            direccion = direccion,
            checklistJson = checklistJson,
            observacion = observacion,
            latitud = latitud,
            longitud = longitud,
            fotosJson = fotoPaths.joinToString(","),
            fecha = dateTimeFormat.format(Date()),
            usuario = usuario,
            sincronizado = sincronizado
        )
        return revisionDao.insertRevision(revision)
    }

    suspend fun getRevisionesPendientes(): List<Revision> {
        return revisionDao.getRevisionesPendientes()
    }
}
