package com.systemapp.daily.data.repository

import android.content.Context
import com.systemapp.daily.data.api.RetrofitClient
import com.systemapp.daily.data.local.AppDatabase
import com.systemapp.daily.data.model.SyncResult
import com.systemapp.daily.utils.NetworkResult
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class SyncRepository(context: Context) {

    private val api = RetrofitClient.apiService
    private val db = AppDatabase.getDatabase(context)
    private val macroDao = db.macroDao()
    private val ordenRevisionDao = db.ordenRevisionDao()
    private val listaDao = db.listaDao()

    suspend fun sincronizar(apiToken: String): NetworkResult<SyncResult> {
        val errores = mutableListOf<String>()
        var macrosSubidos = 0
        var revisionesSubidas = 0
        var macrosDescargados = 0
        var revisionesDescargadas = 0
        var listasDescargadas = 0

        // 1. Subir macros pendientes
        try {
            val macrosPendientes = macroDao.getPendientesSincronizar()
            for (macro in macrosPendientes) {
                try {
                    val textPlain = "text/plain".toMediaTypeOrNull()
                    val fotoParts = buildFotoParts(macro.rutaFotos)

                    val response = api.enviarMacro(
                        apiToken = "Bearer $apiToken",
                        idOrden = macro.idOrden.toString().toRequestBody(textPlain),
                        lecturaActual = (macro.lecturaActual ?: "").toRequestBody(textPlain),
                        observacion = macro.observacion?.toRequestBody(textPlain),
                        gpsLatitud = macro.gpsLatitudLectura?.toString()?.toRequestBody(textPlain),
                        gpsLongitud = macro.gpsLongitudLectura?.toString()?.toRequestBody(textPlain),
                        fotos = fotoParts
                    )
                    if (response.isSuccessful && response.body()?.success == true) {
                        macroDao.marcarSincronizado(macro.idOrden)
                        macrosSubidos++
                    } else {
                        errores.add("Macro ${macro.codigoMacro}: ${response.body()?.message ?: "Error"}")
                    }
                } catch (e: Exception) {
                    errores.add("Macro ${macro.codigoMacro}: ${e.localizedMessage}")
                }
            }
        } catch (e: Exception) {
            errores.add("Error subiendo macros: ${e.localizedMessage}")
        }

        // 2. Subir revisiones pendientes
        try {
            val revisionesPendientes = ordenRevisionDao.getPendientesSincronizar()
            for (revision in revisionesPendientes) {
                try {
                    val textPlain = "text/plain".toMediaTypeOrNull()
                    val fotoParts = buildFotoParts(revision.rutaFotos)
                    val firmaPart = buildFilePart("firma_cliente", revision.firmaCliente)

                    val response = api.enviarRevisionV2(
                        apiToken = "Bearer $apiToken",
                        idOrden = revision.idOrden.toString().toRequestBody(textPlain),
                        codigoPredio = revision.codigoPredio.toRequestBody(textPlain),
                        estadoAcometida = revision.estadoAcometida?.toRequestBody(textPlain),
                        estadoSellos = revision.estadoSellos?.toRequestBody(textPlain),
                        nombreAtiende = revision.nombreAtiende?.toRequestBody(textPlain),
                        tipoDocumento = revision.tipoDocumento?.toRequestBody(textPlain),
                        documento = revision.documento?.toRequestBody(textPlain),
                        numFamilias = revision.numFamilias?.toString()?.toRequestBody(textPlain),
                        numPersonas = revision.numPersonas?.toString()?.toRequestBody(textPlain),
                        motivoRevision = revision.motivoRevision?.toRequestBody(textPlain),
                        motivoDetalle = revision.motivoDetalle?.toRequestBody(textPlain),
                        generalidades = revision.generalidades?.toRequestBody(textPlain),
                        censoHidraulicoJson = null,
                        gpsLatitud = revision.gpsLatitudPredio?.toString()?.toRequestBody(textPlain),
                        gpsLongitud = revision.gpsLongitudPredio?.toString()?.toRequestBody(textPlain),
                        fotos = fotoParts,
                        firmaCliente = firmaPart,
                        actaPdf = null
                    )
                    if (response.isSuccessful && response.body()?.success == true) {
                        ordenRevisionDao.marcarSincronizado(revision.idOrden)
                        revisionesSubidas++
                    } else {
                        errores.add("Revisión ${revision.codigoPredio}: ${response.body()?.message ?: "Error"}")
                    }
                } catch (e: Exception) {
                    errores.add("Revisión ${revision.codigoPredio}: ${e.localizedMessage}")
                }
            }
        } catch (e: Exception) {
            errores.add("Error subiendo revisiones: ${e.localizedMessage}")
        }

        // 3. Descargar órdenes de macros
        try {
            val response = api.getOrdenesMacro("Bearer $apiToken")
            if (response.isSuccessful) {
                val macros = response.body() ?: emptyList()
                if (macros.isNotEmpty()) {
                    macroDao.insertAll(macros)
                    macrosDescargados = macros.size
                }
            } else {
                errores.add("Error descargando macros: ${response.code()}")
            }
        } catch (e: Exception) {
            errores.add("Error descargando macros: ${e.localizedMessage}")
        }

        // 4. Descargar órdenes de revisión
        try {
            val response = api.getOrdenesRevision("Bearer $apiToken")
            if (response.isSuccessful) {
                val revisiones = response.body() ?: emptyList()
                if (revisiones.isNotEmpty()) {
                    ordenRevisionDao.insertAll(revisiones)
                    revisionesDescargadas = revisiones.size
                }
            } else {
                errores.add("Error descargando revisiones: ${response.code()}")
            }
        } catch (e: Exception) {
            errores.add("Error descargando revisiones: ${e.localizedMessage}")
        }

        // 5. Descargar listas/catálogos
        try {
            val response = api.getListasParametros("Bearer $apiToken")
            if (response.isSuccessful) {
                val listas = response.body() ?: emptyList()
                if (listas.isNotEmpty()) {
                    listaDao.deleteAll()
                    listaDao.insertAll(listas)
                    listasDescargadas = listas.size
                }
            } else {
                errores.add("Error descargando listas: ${response.code()}")
            }
        } catch (e: Exception) {
            errores.add("Error descargando listas: ${e.localizedMessage}")
        }

        val result = SyncResult(
            macrosSubidos = macrosSubidos,
            revisionesSubidas = revisionesSubidas,
            macrosDescargados = macrosDescargados,
            revisionesDescargadas = revisionesDescargadas,
            listasDescargadas = listasDescargadas,
            errores = errores
        )

        return if (errores.isEmpty()) {
            NetworkResult.Success(result)
        } else {
            NetworkResult.Success(result) // Parcialmente exitoso
        }
    }

    suspend fun descargarDatos(apiToken: String): NetworkResult<SyncResult> {
        val errores = mutableListOf<String>()
        var macrosDescargados = 0
        var revisionesDescargadas = 0
        var listasDescargadas = 0

        try {
            val macroResponse = api.getOrdenesMacro("Bearer $apiToken")
            if (macroResponse.isSuccessful) {
                val macros = macroResponse.body() ?: emptyList()
                macroDao.insertAll(macros)
                macrosDescargados = macros.size
            }
        } catch (e: Exception) {
            errores.add("Macros: ${e.localizedMessage}")
        }

        try {
            val revResponse = api.getOrdenesRevision("Bearer $apiToken")
            if (revResponse.isSuccessful) {
                val revisiones = revResponse.body() ?: emptyList()
                ordenRevisionDao.insertAll(revisiones)
                revisionesDescargadas = revisiones.size
            }
        } catch (e: Exception) {
            errores.add("Revisiones: ${e.localizedMessage}")
        }

        try {
            val listasResponse = api.getListasParametros("Bearer $apiToken")
            if (listasResponse.isSuccessful) {
                val listas = listasResponse.body() ?: emptyList()
                listaDao.deleteAll()
                listaDao.insertAll(listas)
                listasDescargadas = listas.size
            }
        } catch (e: Exception) {
            errores.add("Listas: ${e.localizedMessage}")
        }

        val result = SyncResult(
            macrosDescargados = macrosDescargados,
            revisionesDescargadas = revisionesDescargadas,
            listasDescargadas = listasDescargadas,
            errores = errores
        )
        return NetworkResult.Success(result)
    }

    suspend fun borrarDatosLocales() {
        macroDao.deleteAll()
        ordenRevisionDao.deleteAll()
        db.hidraulicoDao().deleteAll()
        listaDao.deleteAll()
        db.lecturaDao().run {
            // Borrar lecturas legacy también
        }
    }

    private fun buildFotoParts(rutaFotos: String?): List<MultipartBody.Part> {
        if (rutaFotos.isNullOrBlank()) return emptyList()
        return rutaFotos.split(",")
            .filter { it.isNotBlank() }
            .mapIndexedNotNull { index, path ->
                val file = File(path.trim())
                if (file.exists()) {
                    val mediaType = if (path.endsWith(".pdf")) "application/pdf" else "image/jpeg"
                    val requestFile = file.asRequestBody(mediaType.toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("fotos[$index]", file.name, requestFile)
                } else null
            }
    }

    private fun buildFilePart(name: String, path: String?): MultipartBody.Part? {
        if (path.isNullOrBlank()) return null
        val file = File(path)
        if (!file.exists()) return null
        val mediaType = if (path.endsWith(".pdf")) "application/pdf" else "image/jpeg"
        val requestFile = file.asRequestBody(mediaType.toMediaTypeOrNull())
        return MultipartBody.Part.createFormData(name, file.name, requestFile)
    }
}
