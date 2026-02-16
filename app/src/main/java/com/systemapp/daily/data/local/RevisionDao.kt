package com.systemapp.daily.data.local

import androidx.room.*
import com.systemapp.daily.data.model.Revision

@Dao
interface RevisionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRevision(revision: Revision): Long

    @Query("SELECT * FROM revisiones WHERE sincronizado = 0 ORDER BY fecha DESC")
    suspend fun getRevisionesPendientes(): List<Revision>

    @Query("SELECT COUNT(*) FROM revisiones WHERE medidorId = :medidorId AND fecha LIKE :fecha || '%'")
    suspend fun contarRevisionesDelDia(medidorId: Int, fecha: String): Int

    @Update
    suspend fun updateRevision(revision: Revision)

    @Query("UPDATE revisiones SET sincronizado = 1 WHERE id = :revisionId")
    suspend fun marcarComoSincronizada(revisionId: Int)

    @Query("SELECT * FROM revisiones ORDER BY fecha DESC LIMIT 50")
    suspend fun getUltimasRevisiones(): List<Revision>
}
