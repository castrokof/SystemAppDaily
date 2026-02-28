package com.systemapp.daily.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cola de sincronización.
 * Almacena registros pendientes de enviar al servidor.
 */
@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    /** Tipo: "LECTURA", "REVISION" o "MACRO" */
    @ColumnInfo(name = "tipo")
    val tipo: String,

    /** ID del registro local (lectura.id o revision.id) */
    @ColumnInfo(name = "registro_id")
    val registroId: Int,

    /** Estado: PENDIENTE, ENVIANDO, ENVIADO, ERROR */
    @ColumnInfo(name = "estado")
    val estado: String = EstadoSync.PENDIENTE,

    /** Número de intentos realizados */
    @ColumnInfo(name = "intentos")
    val intentos: Int = 0,

    /** Mensaje de error del último intento fallido */
    @ColumnInfo(name = "error_mensaje")
    val errorMensaje: String? = null,

    /** Fecha de creación */
    @ColumnInfo(name = "fecha_creacion")
    val fechaCreacion: String,

    /** Fecha del último intento */
    @ColumnInfo(name = "fecha_ultimo_intento")
    val fechaUltimoIntento: String? = null
)

object EstadoSync {
    const val PENDIENTE = "PENDIENTE"
    const val ENVIANDO = "ENVIANDO"
    const val ENVIADO = "ENVIADO"
    const val ERROR = "ERROR"
}

object TipoSync {
    const val LECTURA = "LECTURA"
    const val REVISION = "REVISION"
    const val REVISION_V2 = "REVISION_V2"
    const val MACRO = "MACRO"
}
