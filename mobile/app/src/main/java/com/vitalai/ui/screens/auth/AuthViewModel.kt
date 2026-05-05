package com.vitalai.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.remote.model.RegisterRequest
import com.vitalai.data.remote.model.LoginRequest
import com.vitalai.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val userName: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState = _authState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authRepository.login(LoginRequest(email, password))
                .onSuccess { _authState.value = AuthState.Success(it.user.displayName) }
                .onFailure { _authState.value = AuthState.Error(it.message ?: "Đã xảy ra lỗi") }
        }
    }

    fun register(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authRepository.register(RegisterRequest(email, password, displayName))
                .onSuccess { _authState.value = AuthState.Success(it.user.displayName) }
                .onFailure { _authState.value = AuthState.Error(it.message ?: "Đã xảy ra lỗi") }
        }
    }

    fun handleGoogleError(message: String) {
        _authState.value = AuthState.Error(message)
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authRepository.loginWithGoogle(idToken)
                .onSuccess { _authState.value = AuthState.Success(it.user.displayName) }
                .onFailure { _authState.value = AuthState.Error(it.message ?: "Đăng nhập Google thất bại") }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
