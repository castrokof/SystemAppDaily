package com.systemapp.daily.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Orden de revisión (Formato C de Acuasur: "Revisión de crítica de lectura").
 *
 * Se llena desde [RevisionDto] (descarga de `ordenesRevision`) y se completa en campo.
 * Los campos de encabezado vienen del servidor; los de ejecución los llena el revisor.
 * Todas las columnas nuevas son nullable para que la migración 6 -> 7 sea solo ALTER TABLE.
 */
@Entity(tableName = "ordenes_revision")
data class RevisionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id_orden")
    val idOrden: Int,

    @ColumnInfo(name = "codigo_predio")
    val codigoPredio: String,

    @ColumnInfo(name = "estado_orden")
    val estadoOrden: String = EstadoOrden.PENDIENTE,

    // ---------- Encabezado (solo lectura, viene del servidor) ----------
    @ColumnInfo(name = "numero_revision") val numeroRevision: String? = null,
    @ColumnInfo(name = "periodo_anio") val periodoAnio: String? = null,
    @ColumnInfo(name = "periodo_mes") val periodoMes: String? = null,
    /** "ALTO" | "BAJO" | null */
    @ColumnInfo(name = "tipo_consumo") val tipoConsumo: String? = null,
    @ColumnInfo(name = "nombre_suscriptor") val nombreSuscriptor: String? = null,
    @ColumnInfo(name = "direccion") val direccion: String? = null,
    @ColumnInfo(name = "telefono") val telefono: String? = null,
    @ColumnInfo(name = "ref_medidor") val refMedidor: String? = null,
    /** Lectura actual del periodo facturado (en el servidor se llama `lectura_actual`). */
    @ColumnInfo(name = "lectura_periodo") val lecturaPeriodo: String? = null,
    @ColumnInfo(name = "lectura_anterior") val lecturaAnterior: String? = null,
    @ColumnInfo(name = "consumo_actual") val consumoActual: String? = null,
    @ColumnInfo(name = "promedio") val promedio: String? = null,
    @ColumnInfo(name = "critica_original") val criticaOriginal: String? = null,
    @ColumnInfo(name = "funcionario_nombre") val funcionarioNombre: String? = null,
    @ColumnInfo(name = "funcionario_cc") val funcionarioCc: String? = null,

    // ---------- Formato C (lo llena el revisor) ----------
    /** H / D / C */
    @ColumnInfo(name = "tipo_predio") val tipoPredio: String? = null,
    /** R / C / I / O / E */
    @ColumnInfo(name = "uso_predio") val usoPredio: String? = null,
    @ColumnInfo(name = "tiempo_habitacion") val tiempoHabitacion: String? = null,
    /** yyyy-MM-dd HH:mm:ss tomada del teléfono al momento de la visita */
    @ColumnInfo(name = "fecha_visita") val fechaVisita: String? = null,
    /** SIN / CON */
    @ColumnInfo(name = "medidor_movimiento") val medidorMovimiento: String? = null,
    /** JSON array de códigos de CAUSA_CONSUMO */
    @ColumnInfo(name = "causas_consumo") val causasConsumo: String? = null,
    @ColumnInfo(name = "causa_otro_cual") val causaOtroCual: String? = null,
    /** Ruta local del PNG de la firma del funcionario usada en esta revisión */
    @ColumnInfo(name = "firma_funcionario") val firmaFuncionario: String? = null,

    // ---------- Información adicional (opcional) ----------
    @ColumnInfo(name = "estado_acometida") val estadoAcometida: String? = null,
    @ColumnInfo(name = "estado_sellos") val estadoSellos: String? = null,
    @ColumnInfo(name = "nombre_atiende") val nombreAtiende: String? = null,
    @ColumnInfo(name = "tipo_documento") val tipoDocumento: String? = null,
    val documento: String? = null,
    @ColumnInfo(name = "num_familias") val numFamilias: Int? = null,
    /** N° de habitantes del Formato C */
    @ColumnInfo(name = "num_personas") val numPersonas: Int? = null,
    @ColumnInfo(name = "motivo_revision") val motivoRevision: String? = null,
    @ColumnInfo(name = "motivo_detalle") val motivoDetalle: String? = null,
    /** Observación del Formato C */
    val generalidades: String? = null,
    @ColumnInfo(name = "firma_cliente") val firmaCliente: String? = null,
    @ColumnInfo(name = "ruta_fotos") val rutaFotos: String? = null,
    @ColumnInfo(name = "gps_latitud_predio") val gpsLatitudPredio: Double? = null,
    @ColumnInfo(name = "gps_longitud_predio") val gpsLongitudPredio: Double? = null,
    @ColumnInfo(name = "fecha_cierre") val fechaCierre: String? = null,
    /** Lectura al momento de la visita (se envía como `nueva_lectura`). */
    @ColumnInfo(name = "lectura_actual") val lecturaActual: String? = null,
    @ColumnInfo(name = "acta_pdf_path") val actaPdfPath: String? = null,
    @ColumnInfo(name = "orden_manual", defaultValue = "0") val ordenManual: Int = 0,

    val sincronizado: Boolean = false
) {
    val esAltoConsumo: Boolean get() = tipoConsumo.equals("ALTO", true) || motivoRevision == MotivoRevision.DESVIACION_ALTA
    val esBajoConsumo: Boolean get() = tipoConsumo.equals("BAJO", true) || motivoRevision == MotivoRevision.DESVIACION_BAJA

    /** Texto corto para mostrar el tipo de crítica. */
    val etiquetaConsumo: String?
        get() = when {
            esAltoConsumo -> "ALTO CONSUMO"
            esBajoConsumo -> "BAJO CONSUMO"
            else -> null
        }

    val numeroVisible: String get() = numeroRevision?.takeIf { it.isNotBlank() } ?: idOrden.toString()

    val fotos: List<String>
        get() = rutaFotos?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

    /** Una revisión ejecutada y enviada ya no se edita (solo consulta/reimpresión). */
    val soloLectura: Boolean get() = estadoOrden == EstadoOrden.EJECUTADO && sincronizado
}

object MotivoRevision {
    const val DESVIACION_BAJA = "DESVIACION_BAJA"
    const val DESVIACION_ALTA = "DESVIACION_ALTA"
    const val OTRO = "OTRO"
}
