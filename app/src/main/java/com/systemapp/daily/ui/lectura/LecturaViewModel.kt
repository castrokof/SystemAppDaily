package com.systemapp.daily.ui.lectura

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.systemapp.daily.data.model.CheckLecturaResponse
import com.systemapp.daily.data.repository.LecturaRepository
import com.systemapp.daily.utils.NetworkResult
import com.systemapp.daily.utils.SessionManager
import kotlinx.coroutines.launch

class LecturaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LecturaRepository(application)

    // Fotos capturadas (paths locales)
    private val _fotos = MutableLiveData<MutableList<String>>(mutableListOf())
    val fotos: LiveData<MutableList<String>> = _fotos

    // Verificación de permisos de lectura
    private val _checkResult = MutableLiveData<NetworkResult<CheckLecturaResponse>>()
    val checkResult: LiveData<NetworkResult<CheckLecturaResponse>> = _checkResult

    // Resultado del envío con estado de sync
    private val _syncStatus = MutableLiveData<LecturaRepository.SyncStatus?>()
    val syncStatus: LiveData<LecturaRepository.SyncStatus?> = _syncStatus

    // Estado de carga
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    fun agregarFoto(path: String) {
        val lista = _fotos.value ?: mutableListOf()
        lista.add(path)
        _fotos.value = lista
    }

    fun eliminarFoto(index: Int) {
        val lista = _fotos.value ?: mutableListOf()
        if (index in lista.indices) {
            lista.removeAt(index)
            _fotos.value = lista
        }
    }

    fun getCantidadFotos(): Int = _fotos.value?.size ?: 0

    fun checkPuedeLeer( macroId: Int) {
        _checkResult.value = NetworkResult.Loading
        viewModelScope.launch {
            val sessionManager = SessionManager(getApplication())
            val token = sessionManager.apiToken ?: ""
            _checkResult.value = repository.checkPuedeLeer( token, macroId)
        }
    }

    fun enviarLectura(
        token: String,
        macroId: Int,
        valorLectura: String,
        observacion: String?
    ) {
        val fotoPaths = _fotos.value ?: emptyList()

        _isLoading.value = true
        _syncStatus.value = null
        viewModelScope.launch {
            val result = repository.enviarLectura(
                apiToken = token,
                macroId = macroId,
                valorLectura = valorLectura,
                observacion = observacion,
                fotoPaths = fotoPaths
            )
            _isLoading.value = false
            _syncStatus.value = result
        }
    }
}
