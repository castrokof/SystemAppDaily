package com.systemapp.daily.data.api

import com.systemapp.daily.data.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // =============================================
    // LOGIN
    // =============================================
    @POST("loginMovil1")
    suspend fun login(
        @Query("usuario") usuario: String,
        @Query("password") password: String
    ): Response<List<UserLogin>>

    // =============================================
    // MEDIDORES
    // =============================================
    @GET("medidoresout")
    suspend fun getMedidores(
        @Query("usuario") usuario: String
    ): Response<List<Medidor>>

    // =============================================
    // SINCRONIZACIÓN - Descarga
    // =============================================
    @GET("ordenesMacro")
    suspend fun getOrdenesMacro(apiToken: String): Response<List<MacroEntity>>

    @GET("ordenesRevision")
    suspend fun getOrdenesRevision(apiToken: String): Response<List<RevisionEntity>>

    @GET("listasParametros")
    suspend fun getListasParametros(apiToken: String): Response<List<ListaEntity>>

    // =============================================
    // LECTURAS - Legacy
    // =============================================
    @GET("lecturasMovil/check")
    suspend fun checkLectura(
        @Query("medidor_id") medidorId: Int
    ): Response<CheckLecturaResponse>

    @Multipart
    @POST("lecturasMovil")
    suspend fun enviarLectura(
        @Part("medidor_id") medidorId: RequestBody,
        @Part("valor_lectura") valorLectura: RequestBody,
        @Part("observacion") observacion: RequestBody?,
        @Part fotos: List<MultipartBody.Part>,
        apiToken: RequestBody
    ): Response<LecturaResponse>

    // =============================================
    // MACROS - Subida
    // =============================================
    @Multipart
    @POST("macromedidoresMovil")
    suspend fun enviarMacro(
        @Part("id_orden") idOrden: RequestBody,
        @Part("lectura_actual") lecturaActual: RequestBody,
        @Part("observacion") observacion: RequestBody?,
        @Part("gps_latitud") gpsLatitud: RequestBody?,
        @Part("gps_longitud") gpsLongitud: RequestBody?,
        @Part fotos: List<MultipartBody.Part>,
        apiToken: RequestBody
    ): Response<SyncResponse>

    // =============================================
    // REVISIONES - Subida v2
    // =============================================
    @Multipart
    @POST("revisionesMovil")
    suspend fun enviarRevision(
        @Part("medidor_id") medidorId: RequestBody,
        @Part("checklist_json") checklistJson: RequestBody,
        @Part("observacion") observacion: RequestBody?,
        @Part("latitud") latitud: RequestBody?,
        @Part("longitud") longitud: RequestBody?,
        @Part fotos: List<MultipartBody.Part>,
        @Part actaPdf: MultipartBody.Part?,
        apiToken: RequestBody
    ): Response<RevisionResponse>

    @Multipart
    @POST("revisionesMovilV2")
    suspend fun enviarRevisionV2(
        @Part("id_orden") idOrden: RequestBody,
        @Part("codigo_predio") codigoPredio: RequestBody,
        @Part("estado_acometida") estadoAcometida: RequestBody?,
        @Part("estado_sellos") estadoSellos: RequestBody?,
        @Part("nombre_atiende") nombreAtiende: RequestBody?,
        @Part("tipo_documento") tipoDocumento: RequestBody?,
        @Part("documento") documento: RequestBody?,
        @Part("num_familias") numFamilias: RequestBody?,
        @Part("num_personas") numPersonas: RequestBody?,
        @Part("motivo_revision") motivoRevision: RequestBody?,
        @Part("motivo_detalle") motivoDetalle: RequestBody?,
        @Part("generalidades") generalidades: RequestBody?,
        @Part("censo_hidraulico_json") censoHidraulicoJson: RequestBody?,
        @Part("gps_latitud") gpsLatitud: RequestBody?,
        @Part("gps_longitud") gpsLongitud: RequestBody?,
        @Part fotos: List<MultipartBody.Part>,
        @Part firmaCliente: MultipartBody.Part?,
        @Part actaPdf: MultipartBody.Part?,
        apiToken: RequestBody
    ): Response<SyncResponse>
}
