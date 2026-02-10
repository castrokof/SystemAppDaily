package com.systemapp.daily.data.model

/**
 * Datos enviados al endpoint de login.
 * Usa los mismos parámetros que loginMovil1: usuario y password.
 */
data class LoginRequest(
    val usuario: String,
    val password: String
)
