package com.systemapp.daily.data.repository

import android.content.Context
import com.systemapp.daily.data.api.RetrofitClient
import com.systemapp.daily.data.local.AppDatabase
import com.systemapp.daily.data.model.CheckLecturaResponse
import com.systemapp.daily.data.model.Lectura
import com.systemapp.daily.data.model.LecturaResponse
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
 */
class LecturaRepository(context: Context) {

    private val api = RetrofitClient.apiService
    private val lecturaDao = AppDatabase.getDatabase(context).lecturaDao()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * Verifica si el usuario puede tomar lectura de un macro hoy.
     * Primero intenta verificar con la API, si falla usa datos locales.
     */
    suspend fun checkPuedeLeer(apiToken: String, macroId: Int): NetworkResult<CheckLecturaResponse> {
        return try {
            val response = api.checkLectura(apiToken, macroId)
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
            // Si falla la conexión, verificar localmente
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
     * Envía una lectura con fotos al servidor.
     */
    suspend fun enviarLectura(
        apiToken: String,
        macroId: Int,
        valorLectura: String,
        observacion: String?,
        fotoPaths: List<String>
    ): NetworkResult<LecturaResponse> {
        return try {
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
                apiToken = tokenBody,
                macroId = macroIdBody,
                valorLectura = valorBody,
                observacion = obsBody,
                fotos = fotoParts
            )

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success) {
                    // Guardar localmente como sincronizada
                    guardarLecturaLocal(macroId, valorLectura, observacion, fotoPaths, true)
                    NetworkResult.Success(body)
                } else {
                    NetworkResult.Error(body?.message ?: "Error al enviar lectura")
                }
            } else {
                // Guardar localmente como pendiente
                guardarLecturaLocal(macroId, valorLectura, observacion, fotoPaths, false)
                NetworkResult.Error("Error del servidor: ${response.code()}. Lectura guardada localmente.", response.code())
            }
        } catch (e: Exception) {
            // Sin conexión: guardar localmente
            guardarLecturaLocal(macroId, valorLectura, observacion, fotoPaths, false)
            NetworkResult.Error("Sin conexión. Lectura guardada localmente para sincronizar después.")
        }
    }

    /**
     * Guarda una lectura en la base de datos local.
     */
    private suspend fun guardarLecturaLocal(
        macroId: Int,
        valorLectura: String,
        observacion: String?,
        fotoPaths: List<String>,
        sincronizado: Boolean
    ) {
        val lectura = Lectura(
            macroId = macroId,
            valorLectura = valorLectura,
            observacion = observacion,
            fecha = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
            fotosJson = fotoPaths.joinToString(","),
            sincronizado = sincronizado
        )
        lecturaDao.insertLectura(lectura)
    }

    /**
     * Cuenta las lecturas locales del día para un macro.
     */
    suspend fun contarLecturasLocalesHoy(macroId: Int): Int {
        val hoy = dateFormat.format(Date())
        return lecturaDao.contarLecturasDelDia(macroId, hoy)
    }

    /**
     * Obtiene lecturas pendientes de sincronizar.
     */
    suspend fun getLecturasPendientes(): List<Lectura> {
        return lecturaDao.getLecturasPendientes()
    }
}
