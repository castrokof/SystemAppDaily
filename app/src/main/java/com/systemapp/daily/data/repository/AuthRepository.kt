package com.systemapp.daily.data.repository

import com.systemapp.daily.data.api.RetrofitClient
import com.systemapp.daily.data.model.UserLogin
import com.systemapp.daily.utils.NetworkResult

/**
 * Repositorio para operaciones de autenticación.
 */
class AuthRepository {

    private val api = RetrofitClient.apiService

    /**
     * Login contra la API existente loginMovil1.
     * La API retorna un array JSON. Si el array no está vacío, el login es exitoso.
     * Si está vacío o hay error HTTP, el login falló.
     */
    suspend fun login(usuario: String, password: String): NetworkResult<UserLogin> {
        return try {
            val response = api.login(usuario, password)
            if (response.isSuccessful) {
                val body = response.body()
                if (!body.isNullOrEmpty()) {
                    val user = body.first()
                    if (user.estado == "activo") {
                        NetworkResult.Success(user)
                    } else {
                        NetworkResult.Error("Usuario inactivo. Contacte al administrador.")
                    }
                } else {
                    NetworkResult.Error("Usuario o contraseña incorrectos")
                }
            } else {
                NetworkResult.Error("Error del servidor: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error("Error de conexión: ${e.localizedMessage}")
        }
    }
}
