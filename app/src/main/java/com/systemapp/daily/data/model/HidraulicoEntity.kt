package com.systemapp.daily.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "censo_hidraulico",
    foreignKeys = [
        ForeignKey(
            entity = RevisionEntity::class,
            parentColumns = ["id_orden"],
            childColumns = ["id_revision"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("id_revision")]
)
data class HidraulicoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "id_revision")
    val idRevision: Int,

    @ColumnInfo(name = "tipo_punto")
    val tipoPunto: String,

    val cantidad: Int,

    val estado: String
)

object TipoPunto {
    const val GRIFO = "GRIFO"
    const val SANITARIO = "SANITARIO"
    const val DUCHA = "DUCHA"
    const val LAVAMANOS = "LAVAMANOS"
    const val LAVAPLATOS = "LAVAPLATOS"
    const val LAVADERO = "LAVADERO"
    const val TANQUE = "TANQUE"
    const val CALENTADOR = "CALENTADOR"
    const val OTRO = "OTRO"
}

object EstadoPunto {
    const val BUENO = "BUENO"
    const val MALO = "MALO"
}
