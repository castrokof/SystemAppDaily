package com.systemapp.daily.ui.revision_v2

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.systemapp.daily.data.local.AppDatabase
import com.systemapp.daily.data.model.RevisionEntity
import com.systemapp.daily.data.model.TipoSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class RevisionesListViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val dao = db.ordenRevisionDao()
    private val syncQueueDao = db.syncQueueDao()

    private val queryPendientes = MutableLiveData("")
    private val queryEjecutados = MutableLiveData("")

    val pendientes: LiveData<List<RevOrdenAdapter.RevItem>> = queryPendientes.switchMap { q ->
        val flow = if (q.isNullOrBlank()) dao.getPendientes() else dao.buscarPendientes(q)
        flow.map { list -> list.map { RevOrdenAdapter.RevItem(it) } }.flowOn(Dispatchers.IO).asLiveData()
    }

    /** Ejecutadas + estado de la cola en una sola consulta (antes era una consulta por ítem). */
    val ejecutados: LiveData<List<RevOrdenAdapter.RevItem>> = queryEjecutados.switchMap { q ->
        val flow = if (q.isNullOrBlank()) dao.getEjecutados() else dao.buscarEjecutados(q)
        flow.combine(syncQueueDao.observarEstados(TipoSync.REVISION_V2)) { revs, estados ->
            val porId = estados.associateBy { it.registroId }
            revs.map { RevOrdenAdapter.RevItem(it, porId[it.idOrden]?.estado) }
        }.flowOn(Dispatchers.IO).asLiveData()
    }

    fun buscarPendientes(q: String) { queryPendientes.value = q }
    fun buscarEjecutados(q: String) { queryEjecutados.value = q }

    fun retomarRevision(idOrden: Int) {
        viewModelScope.launch(Dispatchers.IO) { dao.retomar(idOrden) }
    }

    fun guardarOrdenManual(items: List<RevisionEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            items.forEachIndexed { index, rev -> dao.actualizarOrden(rev.idOrden, index) }
        }
    }
}
