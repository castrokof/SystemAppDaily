package com.systemapp.daily.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "listas")
data class ListaEntity(
    @PrimaryKey
    val id: Int,

    @ColumnInfo(name = "tipo_lista")
    val tipoLista: String,

    val codigo: String,

    val descripcion: String
)

object TipoLista {
    const val MOTIVOS = "MOTIVOS"
    const val ESTADOS = "ESTADOS"
    const val ACOMETIDA = "ACOMETIDA"
    const val SELLOS = "SELLOS"
    const val TIPO_DOCUMENTO = "TIPO_DOCUMENTO"
    const val TIPO_PUNTO = "TIPO_PUNTO"
    const val SURTE = "SURTE"
}
