package com.systemapp.daily.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.systemapp.daily.data.model.LoginResponse
import com.systemapp.daily.data.repository.AuthRepository
import com.systemapp.daily.utils.NetworkResult
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _loginResult = MutableLiveData<NetworkResult<LoginResponse>>()
    val loginResult: LiveData<NetworkResult<LoginResponse>> = _loginResult

    fun login(usuario: String, password: String) {
        if (usuario.isBlank() || password.isBlank()) {
            _loginResult.value = NetworkResult.Error("Usuario y contraseña son requeridos")
            return
        }

        _loginResult.value = NetworkResult.Loading
        viewModelScope.launch {
            _loginResult.value = repository.login(usuario, password)
        }
    }
}
