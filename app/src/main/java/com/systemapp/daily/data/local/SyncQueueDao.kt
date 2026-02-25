package com.systemapp.daily.data.local

import androidx.room.*
import com.systemapp.daily.data.model.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncQueueEntity): Long

    @Update
    suspend fun update(item: SyncQueueEntity)

    @Delete
    suspend fun delete(item: SyncQueueEntity)

    @Query("SELECT * FROM sync_queue WHERE estado = 'PENDIENTE' OR estado = 'ERROR' ORDER BY fecha_creacion ASC")
    suspend fun getPendientes(): List<SyncQueueEntity>

    @Query("SELECT * FROM sync_queue ORDER BY fecha_creacion DESC")
    fun getAll(): Flow<List<SyncQueueEntity>>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE estado = 'PENDIENTE' OR estado = 'ERROR'")
    fun contarPendientes(): Flow<Int>

    @Query("SELECT COUNT(*) FROM sync_queue WHERE estado = 'PENDIENTE' OR estado = 'ERROR'")
    suspend fun contarPendientesSync(): Int

    @Query("UPDATE sync_queue SET estado = :estado, intentos = intentos + 1, fecha_ultimo_intento = :fecha, error_mensaje = :error WHERE id = :id")
    suspend fun actualizarEstado(id: Int, estado: String, fecha: String, error: String? = null)

    @Query("DELETE FROM sync_queue WHERE estado = 'ENVIADO'")
    suspend fun limpiarEnviados()

    @Query("SELECT * FROM sync_queue WHERE tipo = :tipo AND registro_id = :registroId LIMIT 1")
    suspend fun buscarPorRegistro(tipo: String, registroId: Int): SyncQueueEntity?

    @Query("SELECT estado FROM sync_queue WHERE tipo = :tipo AND registro_id = :registroId LIMIT 1")
    suspend fun getEstadoSync(tipo: String, registroId: Int): String?
}
