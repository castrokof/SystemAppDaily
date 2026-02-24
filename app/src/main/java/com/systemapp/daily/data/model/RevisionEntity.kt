package com.systemapp.daily.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ordenes_revision")
data class RevisionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id_orden")
    val idOrden: Int,

    @ColumnInfo(name = "codigo_predio")
    val codigoPredio: String,

    @ColumnInfo(name = "estado_orden")
    val estadoOrden: String = EstadoOrden.PENDIENTE,

    @ColumnInfo(name = "estado_acometida")
    val estadoAcometida: String? = null,

    @ColumnInfo(name = "estado_sellos")
    val estadoSellos: String? = null,

    @ColumnInfo(name = "nombre_atiende")
    val nombreAtiende: String? = null,

    @ColumnInfo(name = "tipo_documento")
    val tipoDocumento: String? = null,

    val documento: String? = null,

    @ColumnInfo(name = "num_familias")
    val numFamilias: Int? = null,

    @ColumnInfo(name = "num_personas")
    val numPersonas: Int? = null,

    @ColumnInfo(name = "motivo_revision")
    val motivoRevision: String? = null,

    @ColumnInfo(name = "motivo_detalle")
    val motivoDetalle: String? = null,

    val generalidades: String? = null,

    @ColumnInfo(name = "firma_cliente")
    val firmaCliente: String? = null,

    @ColumnInfo(name = "ruta_fotos")
    val rutaFotos: String? = null,

    @ColumnInfo(name = "gps_latitud_predio")
    val gpsLatitudPredio: Double? = null,

    @ColumnInfo(name = "gps_longitud_predio")
    val gpsLongitudPredio: Double? = null,

    @ColumnInfo(name = "fecha_cierre")
    val fechaCierre: String? = null,

    val sincronizado: Boolean = false
)

object MotivoRevision {
    const val DESVIACION_BAJA = "DESVIACION_BAJA"
    const val DESVIACION_ALTA = "DESVIACION_ALTA"
    const val OTRO = "OTRO"
}
