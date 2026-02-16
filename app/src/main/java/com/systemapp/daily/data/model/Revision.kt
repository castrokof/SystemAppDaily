package com.systemapp.daily.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/**
 * Representa una revisión de servicio de acueducto.
 * Entidad Room para almacenamiento local.
 */
@Entity(tableName = "revisiones")
data class Revision(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @SerializedName("medidor_id")
    val medidorId: Int,

    @SerializedName("ref_medidor")
    val refMedidor: String,

    @SerializedName("suscriptor")
    val suscriptor: String?,

    @SerializedName("direccion")
    val direccion: String?,

    @SerializedName("checklist_json")
    val checklistJson: String, // JSON del checklist completado

    @SerializedName("observacion")
    val observacion: String?,

    @SerializedName("latitud")
    val latitud: Double? = null,

    @SerializedName("longitud")
    val longitud: Double? = null,

    @SerializedName("fotos_json")
    val fotosJson: String? = null,

    @SerializedName("fecha")
    val fecha: String,

    @SerializedName("usuario")
    val usuario: String,

    @SerializedName("sincronizado")
    val sincronizado: Boolean = false
)

/**
 * Ítems del checklist para la revisión de acueducto.
 */
data class ChecklistItem(
    val id: String,
    val categoria: String,
    val descripcion: String,
    var estado: EstadoCheck = EstadoCheck.NO_REVISADO,
    var observacion: String? = null
)

enum class EstadoCheck(val label: String) {
    BUENO("Bueno"),
    MALO("Malo"),
    NO_APLICA("No Aplica"),
    NO_REVISADO("Sin revisar")
}

/**
 * Respuesta del endpoint de envío de revisión.
 */
data class RevisionResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String?,

    @SerializedName("data")
    val data: RevisionData?
)

data class RevisionData(
    @SerializedName("id")
    val id: Int
)
