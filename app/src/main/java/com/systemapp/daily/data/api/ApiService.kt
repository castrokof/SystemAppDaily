package com.systemapp.daily.data.api

import com.systemapp.daily.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Definición de endpoints de la API.
 *
 * Endpoints existentes:
 *   - loginMovil1: Login del usuario (ya existe en tu Laravel)
 *
 * Endpoints nuevos que debes crear en Laravel:
 *   - macrosMovil: Listar macromedidores asignados al usuario
 *   - lecturasMovil/check: Verificar si puede tomar lectura hoy
 *   - lecturasMovil: Enviar una lectura con fotos
 */
interface ApiService {

    // =============================================
    // LOGIN - Endpoint existente en tu API
    // =============================================

    /**
     * Login del usuario.
     * Endpoint existente: loginMovil1
     * Parámetros por query: usuario, password
     */
    @GET("loginMovil1")
    suspend fun login(
        @Query("usuario") usuario: String,
        @Query("password") password: String
    ): Response<LoginResponse>

    // =============================================
    // MACROMEDIDORES - Endpoints nuevos
    // =============================================

    /**
     * Obtener lista de macromedidores asignados al usuario.
     * NUEVO: Debes crear este endpoint en Laravel.
     */
    @GET("macrosMovil")
    suspend fun getMacros(
        @Header("Authorization") token: String
    ): Response<MacroListResponse>

    // =============================================
    // LECTURAS - Endpoints nuevos
    // =============================================

    /**
     * Verificar si el usuario puede tomar lectura de un macro hoy.
     * Retorna cuántas lecturas ha hecho hoy y si está autorizado para más.
     * NUEVO: Debes crear este endpoint en Laravel.
     */
    @GET("lecturasMovil/check")
    suspend fun checkLectura(
        @Header("Authorization") token: String,
        @Query("macro_id") macroId: Int
    ): Response<CheckLecturaResponse>

    /**
     * Enviar una lectura con fotos (multipart).
     * Las fotos se envían como archivos adjuntos.
     * NUEVO: Debes crear este endpoint en Laravel.
     */
    @Multipart
    @POST("lecturasMovil")
    suspend fun enviarLectura(
        @Header("Authorization") token: String,
        @Part("macro_id") macroId: RequestBody,
        @Part("valor_lectura") valorLectura: RequestBody,
        @Part("observacion") observacion: RequestBody?,
        @Part fotos: List<MultipartBody.Part>
    ): Response<LecturaResponse>
}
