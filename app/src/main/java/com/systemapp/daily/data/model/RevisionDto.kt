package com.systemapp.daily.data.model

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

/**
 * Elemento de `GET ordenesRevision`.
 *
 * Todo es nullable y los valores "numéricos" de encabezado se leen como String: Gson
 * convierte números a String sin problema, y así un servidor viejo (campos faltantes) o
 * un valor vacío ("") no rompen la descarga completa.
 */
data class RevisionDto(
    @SerializedName("id_orden") val idOrden: Int?,
    @SerializedName("codigo_predio") val codigoPredio: String?,
    @SerializedName("estado_orden") val estadoOrden: String?,
    @SerializedName("estado_acometida") val estadoAcometida: String?,
    @SerializedName("estado_sellos") val estadoSellos: String?,
    @SerializedName("nombre_atiende") val nombreAtiende: String?,
    @SerializedName("tipo_documento") val tipoDocumento: String?,
    @SerializedName("documento") val documento: String?,
    @SerializedName("num_familias") val numFamilias: String?,
    @SerializedName("num_personas") val numPersonas: String?,
    @SerializedName("motivo_revision") val motivoRevision: String?,
    @SerializedName("motivo_detalle") val motivoDetalle: String?,
    @SerializedName("generalidades") val generalidades: String?,

    // Formato C — encabezado
    @SerializedName("numero_revision") val numeroRevision: String?,
    @SerializedName("periodo_anio") val periodoAnio: String?,
    @SerializedName("periodo_mes") val periodoMes: String?,
    @SerializedName("tipo_consumo") val tipoConsumo: String?,
    @SerializedName("nombre_suscriptor") val nombreSuscriptor: String?,
    @SerializedName("direccion") val direccion: String?,
    @SerializedName("telefono") val telefono: String?,
    @SerializedName("ref_medidor") val refMedidor: String?,
    @SerializedName("lectura_actual") val lecturaActual: String?,
    @SerializedName("lectura_anterior") val lecturaAnterior: String?,
    @SerializedName("consumo_actual") val consumoActual: String?,
    @SerializedName("promedio") val promedio: String?,
    @SerializedName("critica_original") val criticaOriginal: String?,
    @SerializedName("funcionario_nombre") val funcionarioNombre: String?,
    @SerializedName("funcionario_cc") val funcionarioCc: String?,

    // Formato C — datos ya diligenciados (precarga)
    @SerializedName("uso_predio") val usoPredio: String?,
    @SerializedName("tipo_predio") val tipoPredio: String?,
    @SerializedName("tiempo_habitacion") val tiempoHabitacion: String?,
    @SerializedName("nueva_lectura") val nuevaLectura: String?,
    @SerializedName("fecha_visita") val fechaVisita: String?,
    @SerializedName("medidor_movimiento") val medidorMovimiento: String?,
    /** Puede llegar como array JSON o como string JSON; se normaliza en [causasJson]. */
    @SerializedName("causas_consumo") val causasConsumo: JsonElement?,
    @SerializedName("causa_otro_cual") val causaOtroCual: String?
) {
    fun causasJson(): String? {
        val el = causasConsumo ?: return null
        return when {
            el.isJsonNull -> null
            el.isJsonArray -> if (el.asJsonArray.size() == 0) null else el.toString()
            el.isJsonPrimitive -> el.asString.takeIf { it.isNotBlank() && it != "[]" }
            else -> null
        }
    }

    /** Entidad nueva (orden que no existía en el teléfono). */
    fun toEntity(): RevisionEntity? {
        val id = idOrden ?: return null
        return aplicarEncabezado(
            RevisionEntity(
                idOrden = id,
                codigoPredio = codigoPredio.orEmpty(),
                estadoOrden = estadoOrden?.takeIf { it.isNotBlank() } ?: EstadoOrden.PENDIENTE,
                estadoAcometida = estadoAcometida,
                estadoSellos = estadoSellos,
                nombreAtiende = nombreAtiende,
                tipoDocumento = tipoDocumento,
                documento = documento,
                numFamilias = numFamilias?.toIntOrNull(),
                numPersonas = numPersonas?.toIntOrNull(),
                motivoRevision = motivoRevision,
                motivoDetalle = motivoDetalle,
                generalidades = generalidades,
                usoPredio = usoPredio,
                tipoPredio = tipoPredio,
                tiempoHabitacion = tiempoHabitacion,
                lecturaActual = nuevaLectura,
                fechaVisita = fechaVisita,
                medidorMovimiento = medidorMovimiento,
                causasConsumo = causasJson(),
                causaOtroCual = causaOtroCual,
                sincronizado = estadoOrden == EstadoOrden.EJECUTADO
            )
        )
    }

    /**
     * Combina con lo que ya hay en el teléfono SIN perder el trabajo de campo:
     * solo se actualiza el encabezado. Si la orden local sigue pendiente y sin tocar,
     * se respeta el estado que diga el servidor.
     */
    fun mergeInto(local: RevisionEntity): RevisionEntity {
        val conEncabezado = aplicarEncabezado(local)
        val sinTrabajoLocal = local.estadoOrden == EstadoOrden.PENDIENTE &&
            local.fechaVisita == null && local.tipoPredio == null && local.firmaCliente == null
        return if (sinTrabajoLocal) {
            conEncabezado.copy(
                estadoOrden = estadoOrden?.takeIf { it.isNotBlank() } ?: local.estadoOrden,
                usoPredio = local.usoPredio ?: usoPredio
            )
        } else conEncabezado
    }

    private fun aplicarEncabezado(e: RevisionEntity) = e.copy(
        codigoPredio = codigoPredio ?: e.codigoPredio,
        numeroRevision = numeroRevision ?: e.numeroRevision,
        periodoAnio = periodoAnio ?: e.periodoAnio,
        periodoMes = periodoMes ?: e.periodoMes,
        tipoConsumo = tipoConsumo ?: e.tipoConsumo,
        nombreSuscriptor = nombreSuscriptor ?: e.nombreSuscriptor,
        direccion = direccion ?: e.direccion,
        telefono = telefono ?: e.telefono,
        refMedidor = refMedidor ?: e.refMedidor,
        lecturaPeriodo = lecturaActual ?: e.lecturaPeriodo,
        lecturaAnterior = lecturaAnterior ?: e.lecturaAnterior,
        consumoActual = consumoActual ?: e.consumoActual,
        promedio = promedio ?: e.promedio,
        criticaOriginal = criticaOriginal ?: e.criticaOriginal,
        funcionarioNombre = funcionarioNombre ?: e.funcionarioNombre,
        funcionarioCc = funcionarioCc ?: e.funcionarioCc,
        motivoRevision = e.motivoRevision ?: motivoRevision
    )
}

object CausasJson {
    private val gson = Gson()
    fun parse(json: String?): List<String> = try {
        if (json.isNullOrBlank()) emptyList()
        else gson.fromJson(json, Array<String>::class.java)?.toList() ?: emptyList()
    } catch (_: Exception) { emptyList() }

    fun toJson(codigos: Collection<String>): String? = if (codigos.isEmpty()) null else gson.toJson(codigos)
}
