package com.systemapp.daily.data.local

import androidx.room.*
import com.systemapp.daily.data.model.UserEntity

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity)

    @Query("SELECT * FROM usuarios WHERE id = :userId LIMIT 1")
    suspend fun getById(userId: Int): UserEntity?

    @Query("SELECT * FROM usuarios LIMIT 1")
    suspend fun getUsuarioActivo(): UserEntity?

    @Query("UPDATE usuarios SET firma_digital = :path WHERE id = :userId")
    suspend fun actualizarFirma(userId: Int, path: String)

    @Query("DELETE FROM usuarios")
    suspend fun deleteAll()
}
