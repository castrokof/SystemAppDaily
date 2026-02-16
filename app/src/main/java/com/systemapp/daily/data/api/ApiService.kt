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
 *   - loginMovil1: Login del usuario
 *   - medidoresout: Listar medidores asignados al usuario
 *
 * Endpoints nuevos que debes crear en Laravel:
 *   - lecturasMovil/check: Verificar si puede tomar lectura hoy
 *   - lecturasMovil: Enviar una lectura con fotos
 */
interface ApiService {

    // =============================================
    // LOGIN - Endpoint existente
    // =============================================

    /**
     * Login del usuario.
     * Retorna un array JSON: [ { id, usuario, nombre, api_token, ... } ]
     */
    @GET("loginMovil1")
    suspend fun login(
        @Query("usuario") usuario: String,
        @Query("password") password: String
    ): Response<List<UserLogin>>

    // =============================================
    // MEDIDORES - Endpoint existente
    // =============================================

    /**
     * Obtener lista de medidores asignados al usuario.
     * Endpoint existente: /medidoresout?usuario=xxx
     * Retorna array JSON con los medidores.
     */
    @GET("medidoresout")
    suspend fun getMedidores(
        @Query("usuario") usuario: String
    ): Response<List<Medidor>>

    // =============================================
    // LECTURAS - Endpoints nuevos
    // =============================================

    /**
     * Verificar si el usuario puede tomar lectura de un medidor hoy.
     * NUEVO: Debes crear este endpoint en Laravel.
     */
    @GET("lecturasMovil/check")
    suspend fun checkLectura(
        @Query("api_token") apiToken: String,
        @Query("medidor_id") medidorId: Int
    ): Response<CheckLecturaResponse>

    /**
     * Enviar una lectura con fotos (multipart).
     * NUEVO: Debes crear este endpoint en Laravel.
     */
    @Multipart
    @POST("lecturasMovil")
    suspend fun enviarLectura(
        @Part("api_token") apiToken: RequestBody,
        @Part("medidor_id") medidorId: RequestBody,
        @Part("valor_lectura") valorLectura: RequestBody,
        @Part("observacion") observacion: RequestBody?,
        @Part fotos: List<MultipartBody.Part>
    ): Response<LecturaResponse>

    // =============================================
    // REVISIONES - Endpoints nuevos
    // =============================================

    /**
     * Enviar una revisión de servicio de acueducto con fotos (multipart).
     * NUEVO: Debes crear este endpoint en Laravel.
     */
    @Multipart
    @POST("revisionesMovil")
    suspend fun enviarRevision(
        @Part("api_token") apiToken: RequestBody,
        @Part("medidor_id") medidorId: RequestBody,
        @Part("checklist_json") checklistJson: RequestBody,
        @Part("observacion") observacion: RequestBody?,
        @Part("latitud") latitud: RequestBody?,
        @Part("longitud") longitud: RequestBody?,
        @Part fotos: List<MultipartBody.Part>
    ): Response<RevisionResponse>
}
