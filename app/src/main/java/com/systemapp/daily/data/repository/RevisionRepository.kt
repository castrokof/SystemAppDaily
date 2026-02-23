package com.systemapp.daily.data.repository

import android.content.Context
import com.systemapp.daily.data.api.RetrofitClient
import com.systemapp.daily.data.local.AppDatabase
import com.systemapp.daily.data.model.Revision
import com.systemapp.daily.data.model.RevisionResponse
import com.systemapp.daily.utils.NetworkResult
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RevisionRepository(context: Context) {

    private val api = RetrofitClient.apiService
    private val revisionDao = AppDatabase.getDatabase(context).revisionDao()

    /**
     * Envía una revisión con fotos y acta PDF al servidor.
     * Los fotoPaths pueden incluir un archivo PDF al final (el acta firmada).
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
    ): NetworkResult<RevisionResponse> {
        return try {
            val tokenBody = apiToken.toRequestBody("text/plain".toMediaTypeOrNull())
            val medidorIdBody = medidorId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val checklistBody = checklistJson.toRequestBody("text/plain".toMediaTypeOrNull())
            val obsBody = observacion?.toRequestBody("text/plain".toMediaTypeOrNull())
            val latBody = latitud?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            val lonBody = longitud?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())

            // Separar fotos (imágenes) del acta PDF
            val imagePaths = fotoPaths.filter { !it.endsWith(".pdf") }
            val pdfPath = fotoPaths.firstOrNull { it.endsWith(".pdf") }

            val fotoParts = imagePaths.mapIndexed { index, path ->
                val file = File(path)
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("fotos[$index]", file.name, requestFile)
            }

            // Acta PDF como parte separada
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

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success) {
                    guardarRevisionLocal(medidorId, refMedidor, suscriptor, direccion, checklistJson, observacion, latitud, longitud, fotoPaths, usuario, true)
                    NetworkResult.Success(body)
                } else {
                    guardarRevisionLocal(medidorId, refMedidor, suscriptor, direccion, checklistJson, observacion, latitud, longitud, fotoPaths, usuario, false)
                    NetworkResult.Error(body?.message ?: "Error al enviar revisión")
                }
            } else {
                guardarRevisionLocal(medidorId, refMedidor, suscriptor, direccion, checklistJson, observacion, latitud, longitud, fotoPaths, usuario, false)
                NetworkResult.Error("Error del servidor: ${response.code()}. Revisión guardada localmente.", response.code())
            }
        } catch (e: Exception) {
            guardarRevisionLocal(medidorId, refMedidor, suscriptor, direccion, checklistJson, observacion, latitud, longitud, fotoPaths, usuario, false)
            NetworkResult.Error("Sin conexión. Revisión guardada localmente.")
        }
    }

    private suspend fun guardarRevisionLocal(
        medidorId: Int, refMedidor: String, suscriptor: String?, direccion: String?,
        checklistJson: String, observacion: String?,
        latitud: Double?, longitud: Double?,
        fotoPaths: List<String>, usuario: String, sincronizado: Boolean
    ) {
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
            fecha = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
            usuario = usuario,
            sincronizado = sincronizado
        )
        revisionDao.insertRevision(revision)
    }

    suspend fun getRevisionesPendientes(): List<Revision> {
        return revisionDao.getRevisionesPendientes()
    }
}
