package com.systemapp.daily.data.repository

import android.content.Context
import com.systemapp.daily.data.api.RetrofitClient
import com.systemapp.daily.data.local.AppDatabase
import com.systemapp.daily.data.model.CheckLecturaResponse
import com.systemapp.daily.data.model.EstadoSync
import com.systemapp.daily.data.model.Lectura
import com.systemapp.daily.data.model.LecturaResponse
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

/**
 * Repositorio para operaciones de lecturas.
 * Maneja tanto la API remota como el almacenamiento local.
 * Implementa auto-sync: guarda local → intenta enviar → encola si falla.
 */
class LecturaRepository(private val context: Context) {

    private val api = RetrofitClient.apiService
    private val db = AppDatabase.getDatabase(context)
    private val lecturaDao = db.lecturaDao()
    private val syncQueueDao = db.syncQueueDao()
    private val networkMonitor = NetworkMonitor.getInstance(context)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    /**
     * Resultado del envío con info de sync.
     */
    sealed class SyncStatus {
        data class EnviadoOk(val response: LecturaResponse) : SyncStatus()
        data class GuardadoLocal(val message: String) : SyncStatus()
        data class Error(val message: String) : SyncStatus()
    }

    /**
     * Verifica si el usuario puede tomar lectura de un macro hoy.
     */
    suspend fun checkPuedeLeer(apiToken: String, macroId: Int): NetworkResult<CheckLecturaResponse> {
        return try {
            val response = api.checkLectura("Bearer $apiToken", macroId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    NetworkResult.Success(body)
                } else {
                    NetworkResult.Error("Respuesta vacía del servidor")
                }
            } else {
                NetworkResult.Error("Error del servidor: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            val lecturasHoy = contarLecturasLocalesHoy(macroId)
            val check = CheckLecturaResponse(
                success = true,
                puedeLeer = lecturasHoy < 2,
                lecturasHoy = lecturasHoy,
                maxLecturas = 2,
                autorizadoExtra = false,
                message = if (lecturasHoy < 2) "Puede tomar lectura (verificación local)"
                else "Límite de lecturas alcanzado (verificación local)"
            )
            NetworkResult.Success(check)
        }
    }

    /**
     * Guarda lectura localmente, intenta enviar inmediatamente.
     * Si no hay red o falla, encola para WorkManager.
     * Retorna SyncStatus para feedback en UI.
     */
    suspend fun enviarLectura(
        apiToken: String,
        macroId: Int,
        valorLectura: String,
        observacion: String?,
        fotoPaths: List<String>
    ): SyncStatus {
        // 1. SIEMPRE guardar localmente primero
        val lecturaId = guardarLecturaLocal(macroId, valorLectura, observacion, fotoPaths, false)

        // 2. Si hay conexión, intentar envío inmediato
        if (networkMonitor.isConnected()) {
            try {
                val tokenBody = apiToken.toRequestBody("text/plain".toMediaTypeOrNull())
                val macroIdBody = macroId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
                val valorBody = valorLectura.toRequestBody("text/plain".toMediaTypeOrNull())
                val obsBody = observacion?.toRequestBody("text/plain".toMediaTypeOrNull())

                val fotoParts = fotoPaths.mapIndexed { index, path ->
                    val file = File(path)
                    val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("fotos[$index]", file.name, requestFile)
                }

                val response = api.enviarLectura(
                    apiToken = "Bearer $apiToken",
                    medidorId = macroIdBody,
                    valorLectura = valorBody,
                    observacion = obsBody,
                    fotos = fotoParts
                )

                if (response.isSuccessful && response.body()?.success == true) {
                    // Envío exitoso: marcar como sincronizado
                    lecturaDao.marcarComoSincronizada(lecturaId.toInt())
                    return SyncStatus.EnviadoOk(response.body()!!)
                } else {
                    // Servidor rechazó: encolar para retry
                    encolarParaSync(lecturaId.toInt())
                    return SyncStatus.GuardadoLocal("Error del servidor. Se reintentará automáticamente.")
                }
            } catch (e: Exception) {
                // Fallo de red: encolar
                encolarParaSync(lecturaId.toInt())
                return SyncStatus.GuardadoLocal("Sin conexión. Se enviará cuando haya internet.")
            }
        } else {
            // Sin red: encolar para cuando haya conexión
            encolarParaSync(lecturaId.toInt())
            return SyncStatus.GuardadoLocal("Sin conexión. Se enviará automáticamente.")
        }
    }

    /**
     * Encola un registro en la cola de sincronización y dispara WorkManager.
     */
    private suspend fun encolarParaSync(lecturaId: Int) {
        val ahora = dateTimeFormat.format(Date())
        val existente = syncQueueDao.buscarPorRegistro(TipoSync.LECTURA, lecturaId)
        if (existente == null) {
            syncQueueDao.insert(
                SyncQueueEntity(
                    tipo = TipoSync.LECTURA,
                    registroId = lecturaId,
                    estado = EstadoSync.PENDIENTE,
                    fechaCreacion = ahora
                )
            )
        }
        // Disparar WorkManager inmediato
        SyncWorker.ejecutarSincronizacionInmediata(context)
    }

    /**
     * Guarda una lectura en la base de datos local.
     * @return ID del registro insertado.
     */
    private suspend fun guardarLecturaLocal(
        macroId: Int,
        valorLectura: String,
        observacion: String?,
        fotoPaths: List<String>,
        sincronizado: Boolean
    ): Long {
        val lectura = Lectura(
            macroId = macroId,
            valorLectura = valorLectura,
            observacion = observacion,
            fecha = dateTimeFormat.format(Date()),
            fotosJson = fotoPaths.joinToString(","),
            sincronizado = sincronizado
        )
        return lecturaDao.insertLectura(lectura)
    }

    suspend fun contarLecturasLocalesHoy(macroId: Int): Int {
        val hoy = dateFormat.format(Date())
        return lecturaDao.contarLecturasDelDia(macroId, hoy)
    }

    suspend fun getLecturasPendientes(): List<Lectura> {
        return lecturaDao.getLecturasPendientes()
    }
}
