package com.systemapp.daily.ui.revision

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.systemapp.daily.data.model.RevisionResponse
import com.systemapp.daily.data.repository.RevisionRepository
import com.systemapp.daily.utils.NetworkResult
import kotlinx.coroutines.launch

class RevisionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RevisionRepository(application)

    private val _envioResult = MutableLiveData<NetworkResult<RevisionResponse>>()
    val envioResult: LiveData<NetworkResult<RevisionResponse>> = _envioResult

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
        _envioResult.value = NetworkResult.Loading
        viewModelScope.launch {
            _envioResult.value = repository.enviarRevision(
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
        }
    }
}
