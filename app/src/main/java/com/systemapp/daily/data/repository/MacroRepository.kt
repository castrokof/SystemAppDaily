package com.systemapp.daily.data.repository

import com.systemapp.daily.data.api.RetrofitClient
import com.systemapp.daily.data.model.Macro
import com.systemapp.daily.utils.NetworkResult

/**
 * Repositorio para operaciones con macromedidores.
 */
class MacroRepository {

    private val api = RetrofitClient.apiService

    suspend fun getMacros(token: String): NetworkResult<List<Macro>> {
        return try {
            val response = api.getMacros("Bearer $token")
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success) {
                    NetworkResult.Success(body.data ?: emptyList())
                } else {
                    NetworkResult.Error(body?.message ?: "Error al obtener macromedidores")
                }
            } else {
                NetworkResult.Error("Error del servidor: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error("Error de conexión: ${e.localizedMessage}")
        }
    }
}
