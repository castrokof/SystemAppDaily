package com.systemapp.daily.data.model

import com.google.gson.annotations.SerializedName

/**
 * Representa un medidor asignado al usuario.
 * Mapea exactamente la respuesta de /medidoresout?usuario=xxx
 */
data class Medidor(
    @SerializedName("id")
    val id: Int,

    @SerializedName("Ciclo")
    val ciclo: Int?,

    @SerializedName("Periodo")
    val periodo: Int?,

    @SerializedName("Ref_Medidor")
    val refMedidor: String,

    @SerializedName("Direccion")
    val direccion: String?,

    @SerializedName("Nombre")
    val nombre: String,

    @SerializedName("Apell")
    val apellido: String?,

    @SerializedName("LA")
    val lecturaAnterior: Int?,

    @SerializedName("Promedio")
    val promedio: Int?,

    @SerializedName("Año")
    val anio: Int?,

    @SerializedName("id_Ruta")
    val idRuta: Int?,

    @SerializedName("Ruta")
    val ruta: String?,

    @SerializedName("consecutivoRuta")
    val consecutivoRuta: Int?,

    @SerializedName("Usuario")
    val usuario: String?,

    @SerializedName("Estado")
    val estado: Int?,

    @SerializedName("Tope")
    val tope: Int?,

    @SerializedName("Suscriptor")
    val suscriptor: String?
) {
    /**
     * Nombre completo del suscriptor (Nombre + Apellido).
     */
    val nombreCompleto: String
        get() = if (apellido != null && apellido != "APELLIDO") "$nombre $apellido" else nombre
}
