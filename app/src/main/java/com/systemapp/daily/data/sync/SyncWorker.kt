package com.systemapp.daily.data.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.systemapp.daily.data.api.ApiService
import com.systemapp.daily.data.api.RetrofitClient
import com.systemapp.daily.data.local.AppDatabase
import com.systemapp.daily.data.model.EstadoSync
import com.systemapp.daily.data.model.TipoSync
import com.systemapp.daily.utils.SessionManager
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Worker de sincronización que procesa la cola de envío pendiente.
 * Se ejecuta cada 15 minutos o cuando hay conectividad.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val db = AppDatabase.getDatabase(applicationContext)
    private val syncQueueDao = db.syncQueueDao()
    private val lecturaDao = db.lecturaDao()
    private val revisionDao = db.revisionDao()
    private val macroDao = db.macroDao()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    override suspend fun doWork(): Result {
        val sessionManager = SessionManager(applicationContext)
        val apiToken = sessionManager.apiToken ?: return Result.failure()
        val api = RetrofitClient.getApiService(apiToken)

        val pendientes = syncQueueDao.getPendientes()
        if (pendientes.isEmpty()) return Result.success()

        Log.d(TAG, "Procesando ${pendientes.size} registros pendientes")

        var todosExitosos = true

        for (item in pendientes) {
            val ahora = dateFormat.format(Date())

            // Marcar como enviando
            syncQueueDao.actualizarEstado(item.id, EstadoSync.ENVIANDO, ahora)

            val exito = when (item.tipo) {
                TipoSync.LECTURA -> enviarLectura(api, item.registroId)
                TipoSync.REVISION -> enviarRevision(api, item.registroId)
                TipoSync.MACRO -> enviarMacro(api, item.registroId)
                else -> false
            }

            if (exito) {
                syncQueueDao.actualizarEstado(item.id, EstadoSync.ENVIADO, ahora)
                Log.d(TAG, "${item.tipo} #${item.registroId} enviado exitosamente")
            } else {
                val maxIntentos = 5
                if (item.intentos + 1 >= maxIntentos) {
                    syncQueueDao.actualizarEstado(item.id, EstadoSync.ERROR, ahora, "Máximo de intentos alcanzado")
                } else {
                    syncQueueDao.actualizarEstado(item.id, EstadoSync.PENDIENTE, ahora, "Reintentando...")
                }
                todosExitosos = false
            }
        }

        // Limpiar los enviados exitosamente
        syncQueueDao.limpiarEnviados()

        return if (todosExitosos) Result.success() else Result.retry()
    }

    private suspend fun enviarLectura(api: ApiService, lecturaId: Int): Boolean {
        return try {
            val lecturas = lecturaDao.getLecturasPendientes()
            val lectura = lecturas.firstOrNull { it.id == lecturaId } ?: return false

            val textPlain = "text/plain".toMediaTypeOrNull()
            val fotoParts = buildFotoParts(lectura.fotosJson)

            val response = api.enviarLectura(
                medidorId = lectura.macroId.toString().toRequestBody(textPlain),
                valorLectura = lectura.valorLectura.toRequestBody(textPlain),
                observacion = lectura.observacion?.toRequestBody(textPlain),
                fotos = fotoParts
            )

            if (response.isSuccessful && response.body()?.success == true) {
                lecturaDao.marcarComoSincronizada(lecturaId)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando lectura $lecturaId: ${e.message}")
            false
        }
    }

    private suspend fun enviarRevision(api: ApiService, revisionId: Int): Boolean {
        return try {
            val revisiones = revisionDao.getRevisionesPendientes()
            val revision = revisiones.firstOrNull { it.id == revisionId } ?: return false

            val textPlain = "text/plain".toMediaTypeOrNull()
            val imagePaths = revision.fotosJson?.split(",")?.filter { it.isNotBlank() && !it.endsWith(".pdf") } ?: emptyList()
            val pdfPath = revision.fotosJson?.split(",")?.firstOrNull { it.trim().endsWith(".pdf") }?.trim()

            val fotoParts = imagePaths.mapIndexedNotNull { index, path ->
                val file = File(path.trim())
                if (file.exists()) {
                    val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("fotos[$index]", file.name, requestFile)
                } else null
            }

            val actaPdfPart = pdfPath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    val requestFile = file.asRequestBody("application/pdf".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("acta_pdf", file.name, requestFile)
                } else null
            }

            val response = api.enviarRevision(
                medidorId = revision.medidorId.toString().toRequestBody(textPlain),
                checklistJson = revision.checklistJson.toRequestBody(textPlain),
                observacion = revision.observacion?.toRequestBody(textPlain),
                latitud = revision.latitud?.toString()?.toRequestBody(textPlain),
                longitud = revision.longitud?.toString()?.toRequestBody(textPlain),
                fotos = fotoParts,
                actaPdf = actaPdfPart
            )

            if (response.isSuccessful && response.body()?.success == true) {
                revisionDao.marcarComoSincronizada(revisionId)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando revisión $revisionId: ${e.message}")
            false
        }
    }

    private suspend fun enviarMacro(api: ApiService, macroId: Int): Boolean {
        return try {
            val macro = macroDao.getById(macroId) ?: return false

            val textPlain = "text/plain".toMediaTypeOrNull()
            val fotoParts = buildFotoParts(macro.rutaFotos)

            val response = api.enviarMacro(
                idOrden = macroId.toString().toRequestBody(textPlain),
                lecturaActual = (macro.lecturaActual ?: "").toRequestBody(textPlain),
                observacion = macro.observacion?.toRequestBody(textPlain),
                gpsLatitud = macro.gpsLatitudLectura?.toString()?.toRequestBody(textPlain),
                gpsLongitud = macro.gpsLongitudLectura?.toString()?.toRequestBody(textPlain),
                fotos = fotoParts
            )

            if (response.isSuccessful && response.body()?.success == true) {
                macroDao.marcarSincronizado(macroId)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando macro $macroId: ${e.message}")
            false
        }
    }

    private fun buildFotoParts(fotosJson: String?): List<MultipartBody.Part> {
        if (fotosJson.isNullOrBlank()) return emptyList()
        return fotosJson.split(",")
            .filter { it.isNotBlank() }
            .mapIndexedNotNull { index, path ->
                val file = File(path.trim())
                if (file.exists()) {
                    val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("fotos[$index]", file.name, requestFile)
                } else null
            }
    }

    companion object {
        private const val TAG = "SyncWorker"
        const val WORK_NAME_PERIODIC = "sync_periodic"
        const val WORK_NAME_IMMEDIATE = "sync_immediate"

        /**
         * Programa sincronización periódica cada 15 minutos.
         */
        fun programarSincronizacionPeriodica(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.d(TAG, "Sincronización periódica programada")
        }

        /**
         * Ejecuta sincronización inmediata (one-time).
         */
        fun ejecutarSincronizacionInmediata(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_IMMEDIATE,
                ExistingWorkPolicy.REPLACE,
                request
            )
            Log.d(TAG, "Sincronización inmediata encolada")
        }
    }
}
