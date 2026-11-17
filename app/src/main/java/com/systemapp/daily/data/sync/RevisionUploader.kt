package com.systemapp.daily.data.sync

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.systemapp.daily.data.acta.ActaFormatoC
import com.systemapp.daily.data.api.ApiService
import com.systemapp.daily.data.local.AppDatabase
import com.systemapp.daily.data.model.RevisionEntity
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

/**
 * Único punto de envío de una revisión a `POST revisionesMovilV2`.
 * Lo usan el wizard (envío inmediato), SyncWorker (cola) y SyncRepository (sincronización manual).
 * El servidor es idempotente por id_orden, así que reenviar no duplica.
 */
class RevisionUploader(private val context: Context, private val api: ApiService) {

    private val db = AppDatabase.getDatabase(context)
    private val dao = db.ordenRevisionDao()
    private val hidraulicoDao = db.hidraulicoDao()
    private val texto = "text/plain".toMediaTypeOrNull()

    /** @return null si se envió bien; mensaje de error en otro caso. */
    suspend fun enviar(idOrden: Int): String? {
        val rev = dao.getById(idOrden) ?: return "Revisión $idOrden no existe en el teléfono"
        if (rev.sincronizado) return null
        if (rev.estadoOrden != com.systemapp.daily.data.model.EstadoOrden.EJECUTADO) return "La revisión ${rev.numeroVisible} no está finalizada"
        return try {
            val acta = asegurarActa(rev)
            val response = api.enviarRevisionV2(campos(rev), archivos(rev, acta))
            val body = response.body()
            if (response.isSuccessful && body?.success == true) {
                dao.marcarSincronizado(idOrden)
                null
            } else {
                val err = body?.message ?: response.errorBody()?.string()?.take(200) ?: "HTTP ${response.code()}"
                Log.w(TAG, "Revisión $idOrden rechazada: $err")
                err
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error enviando revisión $idOrden", e)
            e.localizedMessage ?: e.javaClass.simpleName
        }
    }

    private suspend fun campos(r: RevisionEntity): Map<String, RequestBody> {
        val censo = hidraulicoDao.getByRevision(r.idOrden).map {
            mapOf("tipo_punto" to it.tipoPunto, "cantidad" to it.cantidad, "estado" to it.estado)
        }
        val m = linkedMapOf<String, String?>(
            "id_orden" to r.idOrden.toString(),
            "codigo_predio" to r.codigoPredio,
            // Formato C
            "tipo_predio" to r.tipoPredio,
            "uso_predio" to r.usoPredio,
            "num_personas" to r.numPersonas?.toString(),
            "tiempo_habitacion" to r.tiempoHabitacion,
            "nueva_lectura" to r.lecturaActual,
            // compatibilidad con servidores que aún leen el nombre anterior
            "lectura_actual" to r.lecturaActual,
            "fecha_visita" to r.fechaVisita,
            "medidor_movimiento" to r.medidorMovimiento,
            "causas_consumo_json" to r.causasConsumo,
            "causa_otro_cual" to r.causaOtroCual,
            "generalidades" to r.generalidades,
            "tipo_documento" to r.tipoDocumento,
            "documento" to r.documento,
            // Información adicional
            "estado_acometida" to r.estadoAcometida,
            "estado_sellos" to r.estadoSellos,
            "nombre_atiende" to r.nombreAtiende,
            "num_familias" to r.numFamilias?.toString(),
            "motivo_revision" to r.motivoRevision,
            "motivo_detalle" to r.motivoDetalle,
            "censo_hidraulico_json" to Gson().toJson(censo),
            "gps_latitud" to r.gpsLatitudPredio?.toString(),
            "gps_longitud" to r.gpsLongitudPredio?.toString()
        )
        return m.filterValues { !it.isNullOrBlank() }.mapValues { it.value!!.toRequestBody(texto) }
    }

    private fun archivos(r: RevisionEntity, acta: File?): List<MultipartBody.Part> {
        val partes = mutableListOf<MultipartBody.Part>()
        r.fotos.filter { !it.endsWith(".pdf") }.mapNotNull { File(it).takeIf(File::exists) }
            .forEachIndexed { i, f -> partes += parte("fotos[$i]", f, "image/jpeg") }
        archivo(r.firmaCliente)?.let { partes += parte("firma_cliente", it, "image/png") }
        archivo(r.firmaFuncionario)?.let { partes += parte("firma_funcionario", it, "image/png") }
        acta?.let { partes += parte("acta_pdf", it, "application/pdf") }
        return partes
    }

    /** El PDF es el mismo diseño del acta impresa; si falta (o se borró) se regenera. */
    private suspend fun asegurarActa(r: RevisionEntity): File? {
        archivo(r.actaPdfPath)?.let { return it }
        return try {
            val file = ActaFormatoC.generarPdf(context, ActaFormatoC.construirDatos(context, r))
            dao.actualizarActa(r.idOrden, file.absolutePath)
            file
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo generar el acta ${r.idOrden}", e)
            null
        }
    }

    private fun archivo(path: String?) = path?.takeIf { it.isNotBlank() }?.let(::File)?.takeIf { it.exists() && it.length() > 0 }

    private fun parte(nombre: String, f: File, tipo: String) =
        MultipartBody.Part.createFormData(nombre, f.name, f.asRequestBody(tipo.toMediaTypeOrNull()))

    companion object {
        private const val TAG = "RevisionUploader"
    }
}
