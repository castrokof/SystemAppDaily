package com.systemapp.daily.data.local

import androidx.room.*
import com.systemapp.daily.data.model.RevisionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrdenRevisionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(revisiones: List<RevisionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(revision: RevisionEntity)

    @Update
    suspend fun update(revision: RevisionEntity)

    @Query("SELECT * FROM ordenes_revision WHERE estado_orden = 'PENDIENTE' ORDER BY id_orden ASC")
    fun getPendientes(): Flow<List<RevisionEntity>>

    @Query("SELECT * FROM ordenes_revision WHERE estado_orden = 'EJECUTADO' ORDER BY fecha_cierre DESC")
    fun getEjecutados(): Flow<List<RevisionEntity>>

    @Query("SELECT * FROM ordenes_revision WHERE id_orden = :idOrden LIMIT 1")
    suspend fun getById(idOrden: Int): RevisionEntity?

    @Query("SELECT * FROM ordenes_revision WHERE sincronizado = 0 AND estado_orden = 'EJECUTADO'")
    suspend fun getPendientesSincronizar(): List<RevisionEntity>

    @Query("UPDATE ordenes_revision SET sincronizado = 1 WHERE id_orden = :idOrden")
    suspend fun marcarSincronizado(idOrden: Int)

    @Query("SELECT COUNT(*) FROM ordenes_revision WHERE estado_orden = 'PENDIENTE'")
    suspend fun contarPendientes(): Int

    @Query("SELECT COUNT(*) FROM ordenes_revision WHERE estado_orden = 'EJECUTADO'")
    suspend fun contarEjecutados(): Int

    @Query("DELETE FROM ordenes_revision")
    suspend fun deleteAll()

    @Query("SELECT * FROM ordenes_revision WHERE codigo_predio LIKE '%' || :query || '%'")
    fun buscar(query: String): Flow<List<RevisionEntity>>

    @Query("SELECT * FROM ordenes_revision WHERE estado_orden = 'PENDIENTE' AND (codigo_predio LIKE '%' || :query || '%' OR motivo_revision LIKE '%' || :query || '%' OR nombre_atiende LIKE '%' || :query || '%') ORDER BY id_orden ASC")
    fun buscarPendientes(query: String): Flow<List<RevisionEntity>>

    @Query("SELECT * FROM ordenes_revision WHERE estado_orden = 'EJECUTADO' AND (codigo_predio LIKE '%' || :query || '%' OR motivo_revision LIKE '%' || :query || '%' OR nombre_atiende LIKE '%' || :query || '%') ORDER BY fecha_cierre DESC")
    fun buscarEjecutados(query: String): Flow<List<RevisionEntity>>

    @Query("UPDATE ordenes_revision SET estado_orden = 'PENDIENTE', fecha_cierre = NULL, sincronizado = 0 WHERE id_orden = :idOrden")
    suspend fun retomar(idOrden: Int)
}
