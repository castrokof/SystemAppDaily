package com.systemapp.daily.data.repository

import com.systemapp.daily.data.api.RetrofitClient
import com.systemapp.daily.data.model.Medidor
import com.systemapp.daily.utils.NetworkResult

/**
 * Repositorio para operaciones con medidores.
 */
class MacroRepository {


    /**
     * Obtiene los medidores del usuario usando el endpoint existente /medidoresout.
     * Se pasa el nombre de usuario (no el api_token).
     */
    suspend fun getMedidores(apiToken: String, usuario: String): NetworkResult<List<Medidor>> {

        // ✅ Crear instancia con token para esta llamada
        val api = RetrofitClient.getApiService(apiToken)

        return try {
            val response = api.getMedidores(usuario)
            if (response.isSuccessful) {
                val body = response.body()
                if (!body.isNullOrEmpty()) {
                    NetworkResult.Success(body)
                } else {
                    NetworkResult.Success(emptyList())
                }
            } else {
                NetworkResult.Error("Error del servidor: ${response.code()}", response.code())
            }
        } catch (e: Exception) {
            NetworkResult.Error("Error de conexión: ${e.localizedMessage}")
        }
    }
}
