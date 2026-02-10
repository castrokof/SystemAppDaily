package com.systemapp.daily.data.model

import com.google.gson.annotations.SerializedName

/**
 * Respuesta del endpoint de login.
 * Ajustar los campos según la respuesta real de tu API loginMovil1.
 */
data class LoginResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String?,

    @SerializedName("token")
    val token: String?,

    @SerializedName("user")
    val user: UserData?
)

data class UserData(
    @SerializedName("id")
    val id: Int,

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("usuario")
    val usuario: String,

    @SerializedName("email")
    val email: String?,

    @SerializedName("rol")
    val rol: String?
)
