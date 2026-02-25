package com.systemapp.daily.ui.revision

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.systemapp.daily.data.repository.RevisionRepository
import kotlinx.coroutines.launch

class RevisionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RevisionRepository(application)

    // Resultado del envío con estado de sync
    private val _syncStatus = MutableLiveData<RevisionRepository.SyncStatus?>()
    val syncStatus: LiveData<RevisionRepository.SyncStatus?> = _syncStatus

    // Estado de carga
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun enviarRevision(
        apiToken: String,
        medidorId: Int,
        refMedidor: String,
        suscriptor: String?,
        direccion: String?,
        checklistJson: String,
        observacion: String?,
        latitud: Double?,
        longitud: Double?,
        fotoPaths: List<String>,
        usuario: String
    ) {
        _isLoading.value = true
        _syncStatus.value = null
        viewModelScope.launch {
            val result = repository.enviarRevision(
                apiToken = apiToken,
                medidorId = medidorId,
                refMedidor = refMedidor,
                suscriptor = suscriptor,
                direccion = direccion,
                checklistJson = checklistJson,
                observacion = observacion,
                latitud = latitud,
                longitud = longitud,
                fotoPaths = fotoPaths,
                usuario = usuario
            )
            _isLoading.value = false
            _syncStatus.value = result
        }
    }
}
