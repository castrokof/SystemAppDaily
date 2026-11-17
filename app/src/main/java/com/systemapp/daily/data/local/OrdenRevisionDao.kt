package com.systemapp.daily.data.local

import androidx.room.*
import com.systemapp.daily.data.model.RevisionDto
import com.systemapp.daily.data.model.RevisionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrdenRevisionDao {

    /** Solo para órdenes nuevas: IGNORE nunca pisa (ni borra en cascada el censo de) una existente. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNuevas(revisiones: List<RevisionEntity>)

    @Update
    suspend fun update(revision: RevisionEntity)

    @Update
    suspend fun updateAll(revisiones: List<RevisionEntity>)

    @Query("SELECT * FROM ordenes_revision WHERE id_orden IN (:ids)")
    suspend fun getByIds(ids: List<Int>): List<RevisionEntity>

    /**
     * Guarda la descarga del servidor sin perder trabajo de campo:
     * nuevas se insertan, existentes solo actualizan el encabezado.
     */
    @Transaction
    suspend fun guardarDescarga(dtos: List<RevisionDto>): Int {
        val validos = dtos.filter { it.idOrden != null }
        if (validos.isEmpty()) return 0
        val existentes = validos.mapNotNull { it.idOrden }.chunked(500)
            .flatMap { getByIds(it) }.associateBy { it.idOrden }
        val nuevas = mutableListOf<RevisionEntity>()
        val actualizadas = mutableListOf<RevisionEntity>()
        for (dto in validos) {
            val local = existentes[dto.idOrden]
            if (local == null) dto.toEntity()?.let { nuevas += it }
            else actualizadas += dto.mergeInto(local)
        }
        if (nuevas.isNotEmpty()) insertNuevas(nuevas)
        if (actualizadas.isNotEmpty()) updateAll(actualizadas)
        return validos.size
    }

    @Query("SELECT * FROM ordenes_revision WHERE estado_orden = 'PENDIENTE' ORDER BY orden_manual ASC, id_orden ASC")
    fun getPendientes(): Flow<List<RevisionEntity>>

    @Query("SELECT * FROM ordenes_revision WHERE estado_orden = 'EJECUTADO' ORDER BY orden_manual ASC, fecha_cierre DESC")
    fun getEjecutados(): Flow<List<RevisionEntity>>

    @Query("SELECT * FROM ordenes_revision WHERE id_orden = :idOrden LIMIT 1")
    suspend fun getById(idOrden: Int): RevisionEntity?

    @Query("SELECT * FROM ordenes_revision WHERE id_orden = :idOrden LIMIT 1")
    fun observar(idOrden: Int): Flow<RevisionEntity?>

    @Query("SELECT * FROM ordenes_revision WHERE sincronizado = 0 AND estado_orden = 'EJECUTADO'")
    suspend fun getPendientesSincronizar(): List<RevisionEntity>

    @Query("UPDATE ordenes_revision SET sincronizado = 1 WHERE id_orden = :idOrden")
    suspend fun marcarSincronizado(idOrden: Int)

    @Query("UPDATE ordenes_revision SET acta_pdf_path = :path WHERE id_orden = :idOrden")
    suspend fun actualizarActa(idOrden: Int, path: String?)

    @Query("SELECT COUNT(*) FROM ordenes_revision WHERE estado_orden = 'PENDIENTE'")
    suspend fun contarPendientes(): Int

    @Query("SELECT COUNT(*) FROM ordenes_revision WHERE estado_orden = 'EJECUTADO'")
    suspend fun contarEjecutados(): Int

    @Query("DELETE FROM ordenes_revision")
    suspend fun deleteAll()

    @Query(
        "SELECT * FROM ordenes_revision WHERE estado_orden = 'PENDIENTE' AND (" + FILTRO + ") " +
            "ORDER BY orden_manual ASC, id_orden ASC"
    )
    fun buscarPendientes(query: String): Flow<List<RevisionEntity>>

    @Query(
        "SELECT * FROM ordenes_revision WHERE estado_orden = 'EJECUTADO' AND (" + FILTRO + ") " +
            "ORDER BY orden_manual ASC, fecha_cierre DESC"
    )
    fun buscarEjecutados(query: String): Flow<List<RevisionEntity>>

    @Query("UPDATE ordenes_revision SET orden_manual = :orden WHERE id_orden = :idOrden")
    suspend fun actualizarOrden(idOrden: Int, orden: Int)

    @Query("UPDATE ordenes_revision SET estado_orden = 'PENDIENTE', fecha_cierre = NULL, sincronizado = 0 WHERE id_orden = :idOrden")
    suspend fun retomar(idOrden: Int)

}

/** Búsqueda por código, N° revisión, nombre, dirección, medidor o id. */
private const val FILTRO = "codigo_predio LIKE '%' || :query || '%' " +
    "OR numero_revision LIKE '%' || :query || '%' " +
    "OR nombre_suscriptor LIKE '%' || :query || '%' " +
    "OR direccion LIKE '%' || :query || '%' " +
    "OR ref_medidor LIKE '%' || :query || '%' " +
    "OR CAST(id_orden AS TEXT) LIKE '%' || :query || '%'"
