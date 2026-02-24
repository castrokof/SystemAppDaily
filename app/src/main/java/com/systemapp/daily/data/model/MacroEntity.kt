package com.systemapp.daily.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "macromedidores")
data class MacroEntity(
    @PrimaryKey
    @ColumnInfo(name = "id_orden")
    val idOrden: Int,

    @ColumnInfo(name = "codigo_macro")
    val codigoMacro: String,

    val ubicacion: String? = null,

    @ColumnInfo(name = "lectura_anterior")
    val lecturaAnterior: Int? = null,

    val estado: String = EstadoOrden.PENDIENTE,

    @ColumnInfo(name = "lectura_actual")
    val lecturaActual: String? = null,

    val observacion: String? = null,

    @ColumnInfo(name = "ruta_fotos")
    val rutaFotos: String? = null,

    @ColumnInfo(name = "gps_latitud_lectura")
    val gpsLatitudLectura: Double? = null,

    @ColumnInfo(name = "gps_longitud_lectura")
    val gpsLongitudLectura: Double? = null,

    @ColumnInfo(name = "fecha_lectura")
    val fechaLectura: String? = null,

    val sincronizado: Boolean = false
)

object EstadoOrden {
    const val PENDIENTE = "PENDIENTE"
    const val EJECUTADO = "EJECUTADO"
}
