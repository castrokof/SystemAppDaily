package com.systemapp.daily.data.local

import androidx.room.*
import com.systemapp.daily.data.model.Lectura

/**
 * DAO para operaciones de lectura en la base de datos local.
 */
@Dao
interface LecturaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLectura(lectura: Lectura): Long

    @Query("SELECT * FROM lecturas WHERE sincronizado = 0 ORDER BY fecha DESC")
    suspend fun getLecturasPendientes(): List<Lectura>

    @Query("SELECT * FROM lecturas WHERE macroId = :macroId AND fecha LIKE :fecha || '%' ORDER BY fecha DESC")
    suspend fun getLecturasDelDia(macroId: Int, fecha: String): List<Lectura>

    @Query("SELECT COUNT(*) FROM lecturas WHERE macroId = :macroId AND fecha LIKE :fecha || '%'")
    suspend fun contarLecturasDelDia(macroId: Int, fecha: String): Int

    @Update
    suspend fun updateLectura(lectura: Lectura)

    @Query("UPDATE lecturas SET sincronizado = 1 WHERE id = :lecturaId")
    suspend fun marcarComoSincronizada(lecturaId: Int)

    @Query("SELECT * FROM lecturas ORDER BY fecha DESC LIMIT 50")
    suspend fun getUltimasLecturas(): List<Lectura>
}
