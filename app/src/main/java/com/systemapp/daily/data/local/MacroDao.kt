package com.systemapp.daily.data.local

import androidx.room.*
import com.systemapp.daily.data.model.MacroEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MacroDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(macros: List<MacroEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(macro: MacroEntity)

    @Update
    suspend fun update(macro: MacroEntity)

    @Query("SELECT * FROM macromedidores WHERE estado = 'PENDIENTE' ORDER BY id_orden ASC")
    fun getPendientes(): Flow<List<MacroEntity>>

    @Query("SELECT * FROM macromedidores WHERE estado = 'EJECUTADO' ORDER BY fecha_lectura DESC")
    fun getEjecutados(): Flow<List<MacroEntity>>

    @Query("SELECT * FROM macromedidores WHERE id_orden = :idOrden LIMIT 1")
    suspend fun getById(idOrden: Int): MacroEntity?

    @Query("SELECT * FROM macromedidores WHERE sincronizado = 0 AND estado = 'EJECUTADO'")
    suspend fun getPendientesSincronizar(): List<MacroEntity>

    @Query("UPDATE macromedidores SET sincronizado = 1 WHERE id_orden = :idOrden")
    suspend fun marcarSincronizado(idOrden: Int)

    @Query("SELECT COUNT(*) FROM macromedidores WHERE estado = 'PENDIENTE'")
    suspend fun contarPendientes(): Int

    @Query("SELECT COUNT(*) FROM macromedidores WHERE estado = 'EJECUTADO'")
    suspend fun contarEjecutados(): Int

    @Query("DELETE FROM macromedidores")
    suspend fun deleteAll()

    @Query("SELECT * FROM macromedidores WHERE codigo_macro LIKE '%' || :query || '%' OR ubicacion LIKE '%' || :query || '%'")
    fun buscar(query: String): Flow<List<MacroEntity>>
}
