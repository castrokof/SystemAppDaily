package com.systemapp.daily.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usuarios")
data class UserEntity(
    @PrimaryKey
    val id: Int,

    val nombre: String,

    @ColumnInfo(name = "firma_digital")
    val firmaDigital: String? = null,

    @ColumnInfo(name = "token_sesion")
    val tokenSesion: String,

    @ColumnInfo(name = "fecha_expiracion")
    val fechaExpiracion: String? = null
)
