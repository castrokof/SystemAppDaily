package com.systemapp.daily.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/**
 * Representa una lectura de un medidor.
 * También se usa como entidad Room para almacenamiento local.
 */
@Entity(tableName = "lecturas")
data class Lectura(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @SerializedName("medidor_id")
    val macroId: Int,

    @SerializedName("valor_lectura")
    val valorLectura: String,

    @SerializedName("observacion")
    val observacion: String?,

    @SerializedName("fecha")
    val fecha: String,

    @SerializedName("fotos")
    val fotosJson: String? = null, // JSON array de paths/urls de fotos

    @SerializedName("sincronizado")
    val sincronizado: Boolean = false,

    @SerializedName("usuario_id")
    val usuarioId: Int = 0
)

data class LecturaRequest(
    @SerializedName("macro_id")
    val macroId: Int,

    @SerializedName("valor_lectura")
    val valorLectura: String,

    @SerializedName("observacion")
    val observacion: String?
)

data class LecturaResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String?,

    @SerializedName("data")
    val data: LecturaData?
)

data class LecturaData(
    @SerializedName("id")
    val id: Int,

    @SerializedName("lecturas_restantes")
    val lecturasRestantes: Int,

    @SerializedName("puede_leer")
    val puedeLeer: Boolean
)

data class CheckLecturaResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("puede_leer")
    val puedeLeer: Boolean,

    @SerializedName("lecturas_hoy")
    val lecturasHoy: Int,

    @SerializedName("max_lecturas")
    val maxLecturas: Int,

    @SerializedName("autorizado_extra")
    val autorizadoExtra: Boolean,

    @SerializedName("message")
    val message: String?
)
