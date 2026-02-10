package com.systemapp.daily.data.model

import com.google.gson.annotations.SerializedName

/**
 * Representa un macromedidor asignado al usuario.
 */
data class Macro(
    @SerializedName("id")
    val id: Int,

    @SerializedName("codigo")
    val codigo: String,

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("direccion")
    val direccion: String?,

    @SerializedName("descripcion")
    val descripcion: String?,

    @SerializedName("estado")
    val estado: String?,

    @SerializedName("ultima_lectura")
    val ultimaLectura: String?,

    @SerializedName("lecturas_hoy")
    val lecturasHoy: Int = 0,

    @SerializedName("lectura_autorizada")
    val lecturaAutorizada: Boolean = false
)

data class MacroListResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String?,

    @SerializedName("data")
    val data: List<Macro>?
)
