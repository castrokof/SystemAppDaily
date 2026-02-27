package com.systemapp.daily.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "macromedidores")
data class MacroEntity(

    @SerializedName("id_orden")
    @PrimaryKey
    @ColumnInfo(name = "id_orden")
    val idOrden: Int,

    @SerializedName("codigo_macro")
    @ColumnInfo(name = "codigo_macro")
    val codigoMacro: String,

    val ubicacion: String? = null,

    @SerializedName("lectura_anterior")
    @ColumnInfo(name = "lectura_anterior")
    val lecturaAnterior: Int? = null,

    val estado: String = EstadoOrden.PENDIENTE,

    @SerializedName("lectura_actual")
    @ColumnInfo(name = "lectura_actual")
    val lecturaActual: String? = null,

    val observacion: String? = null,

    @SerializedName("ruta_fotos")
    @ColumnInfo(name = "ruta_fotos")
    val rutaFotos: String? = null,

    @SerializedName("gps_latitud_lectura")
    @ColumnInfo(name = "gps_latitud_lectura")
    val gpsLatitudLectura: Double? = null,

    @SerializedName("gps_longitud_lectura")
    @ColumnInfo(name = "gps_longitud_lectura")
    val gpsLongitudLectura: Double? = null,

    @SerializedName("fecha_lectura")
    @ColumnInfo(name = "fecha_lectura")
    val fechaLectura: String? = null,

    val sincronizado: Boolean = false
)

object EstadoOrden {
    const val PENDIENTE = "PENDIENTE"
    const val EJECUTADO = "EJECUTADO"
}
