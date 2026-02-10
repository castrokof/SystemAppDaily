package com.systemapp.daily.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.systemapp.daily.data.model.Macro
import com.systemapp.daily.data.repository.MacroRepository
import com.systemapp.daily.utils.NetworkResult
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val repository = MacroRepository()

    private val _macros = MutableLiveData<NetworkResult<List<Macro>>>()
    val macros: LiveData<NetworkResult<List<Macro>>> = _macros

    fun cargarMacros(token: String) {
        _macros.value = NetworkResult.Loading
        viewModelScope.launch {
            _macros.value = repository.getMacros(token)
        }
    }
}
