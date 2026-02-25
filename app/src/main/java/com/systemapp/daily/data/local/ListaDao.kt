package com.systemapp.daily.data.local

import androidx.room.*
import com.systemapp.daily.data.model.ListaEntity

@Dao
interface ListaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(listas: List<ListaEntity>)

    @Query("SELECT * FROM listas WHERE tipo_lista = :tipo ORDER BY descripcion ASC")
    suspend fun getByTipo(tipo: String): List<ListaEntity>

    @Query("SELECT * FROM listas ORDER BY tipo_lista, descripcion")
    suspend fun getAll(): List<ListaEntity>

    @Query("SELECT * FROM listas WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): ListaEntity?

    @Query("SELECT DISTINCT tipo_lista FROM listas ORDER BY tipo_lista")
    suspend fun getTiposDisponibles(): List<String>

    @Query("DELETE FROM listas")
    suspend fun deleteAll()
}
