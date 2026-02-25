package com.systemapp.daily.data.local

import androidx.room.*
import com.systemapp.daily.data.model.HidraulicoEntity

@Dao
interface HidraulicoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: HidraulicoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<HidraulicoEntity>)

    @Update
    suspend fun update(item: HidraulicoEntity)

    @Delete
    suspend fun delete(item: HidraulicoEntity)

    @Query("SELECT * FROM censo_hidraulico WHERE id_revision = :idRevision ORDER BY tipo_punto ASC")
    suspend fun getByRevision(idRevision: Int): List<HidraulicoEntity>

    @Query("DELETE FROM censo_hidraulico WHERE id_revision = :idRevision")
    suspend fun deleteByRevision(idRevision: Int)

    @Query("DELETE FROM censo_hidraulico")
    suspend fun deleteAll()
}
