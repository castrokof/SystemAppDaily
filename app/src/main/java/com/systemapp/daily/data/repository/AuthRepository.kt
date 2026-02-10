package com.systemapp.daily.data.repository

import com.systemapp.daily.data.api.RetrofitClient
import com.systemapp.daily.data.model.LoginResponse
import com.systemapp.daily.utils.NetworkResult

/**
 * Repositorio para operaciones de autenticación.
 */
class AuthRepository {

    private val api = RetrofitClient.apiService

    suspend fun login(usuario: String, password: String): NetworkResult<LoginResponse> {
        return try {
            val response = api.login(usuario, password)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success) {
                    NetworkResult.Success(body)
                } else {
                    NetworkResult.Error(body?.message ?: "Credenciales incorrectas")
                }
            } else {
                NetworkResult.Error("Error del servidor: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error("Error de conexión: ${e.localizedMessage}")
        }
    }
}
