package com.systemapp.daily.ui.lectura

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.systemapp.daily.data.model.CheckLecturaResponse
import com.systemapp.daily.data.model.LecturaResponse
import com.systemapp.daily.data.repository.LecturaRepository
import com.systemapp.daily.utils.NetworkResult
import kotlinx.coroutines.launch

class LecturaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LecturaRepository(application)

    // Fotos capturadas (paths locales)
    private val _fotos = MutableLiveData<MutableList<String>>(mutableListOf())
    val fotos: LiveData<MutableList<String>> = _fotos

    // Verificación de permisos de lectura
    private val _checkResult = MutableLiveData<NetworkResult<CheckLecturaResponse>>()
    val checkResult: LiveData<NetworkResult<CheckLecturaResponse>> = _checkResult

    // Resultado del envío de lectura
    private val _envioResult = MutableLiveData<NetworkResult<LecturaResponse>>()
    val envioResult: LiveData<NetworkResult<LecturaResponse>> = _envioResult

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

    fun checkPuedeLeer(token: String, macroId: Int) {
        _checkResult.value = NetworkResult.Loading
        viewModelScope.launch {
            _checkResult.value = repository.checkPuedeLeer(token, macroId)
        }
    }

    fun enviarLectura(
        token: String,
        macroId: Int,
        valorLectura: String,
        observacion: String?
    ) {
        val fotoPaths = _fotos.value ?: emptyList()

        _envioResult.value = NetworkResult.Loading
        viewModelScope.launch {
            _envioResult.value = repository.enviarLectura(
                token = token,
                macroId = macroId,
                valorLectura = valorLectura,
                observacion = observacion,
                fotoPaths = fotoPaths
            )
        }
    }
}
