package com.systemapp.daily.data.model

import com.google.gson.annotations.SerializedName

/**
 * Respuesta del endpoint loginMovil1.
 * La API retorna un array JSON: [ { id, usuario, nombre, api_token, ... } ]
 * Cada elemento del array es un UserLogin.
 */
data class UserLogin(
    @SerializedName("id")
    val id: Int,

    @SerializedName("usuario")
    val usuario: String,

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("tipodeusuario")
    val tipoDeUsuario: String?,

    @SerializedName("email")
    val email: String?,

    @SerializedName("empresa")
    val empresa: String?,

    @SerializedName("remenber_token")
    val rememberToken: String?,

    @SerializedName("estado")
    val estado: String?,

    @SerializedName("api_token")
    val apiToken: String?,

    @SerializedName("created_at")
    val createdAt: String?,

    @SerializedName("updated_at")
    val updatedAt: String?
)
