package com.systemapp.daily.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.systemapp.daily.data.model.Medidor
import com.systemapp.daily.data.repository.MacroRepository
import com.systemapp.daily.utils.NetworkResult
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val repository = MacroRepository()

    private val _medidores = MutableLiveData<NetworkResult<List<Medidor>>>()
    val medidores: LiveData<NetworkResult<List<Medidor>>> = _medidores

    fun cargarMedidores(usuario: String,apiToken: String) {

        _medidores.value = NetworkResult.Loading
        viewModelScope.launch {
            _medidores.value = repository.getMedidores(apiToken, usuario)
        }
    }
}
