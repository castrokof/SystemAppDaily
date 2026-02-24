package com.systemapp.daily.data.model

import com.google.gson.annotations.SerializedName

data class SyncResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String?,

    @SerializedName("id")
    val id: Int? = null
)

data class SyncResult(
    val macrosSubidos: Int = 0,
    val revisionesSubidas: Int = 0,
    val macrosDescargados: Int = 0,
    val revisionesDescargadas: Int = 0,
    val listasDescargadas: Int = 0,
    val errores: List<String> = emptyList()
)
