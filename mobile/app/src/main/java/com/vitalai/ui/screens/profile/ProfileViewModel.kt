package com.vitalai.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.remote.model.HealthProfileDto
import com.vitalai.data.remote.model.StreakDto
import com.vitalai.data.remote.model.UserDto
import com.vitalai.data.repository.AuthRepository
import com.vitalai.data.repository.DashboardRepository
import com.vitalai.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val user: UserDto? = null,
    val healthProfile: HealthProfileDto? = null,
    val streaks: StreakDto? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val dashboardRepository: DashboardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val userDeferred = async { userRepository.getCurrentUser() }
            val healthDeferred = async { userRepository.getHealthProfile() }
            val streaksDeferred = async { dashboardRepository.getStreaks() }

            val user = userDeferred.await().getOrNull()
            val health = healthDeferred.await().getOrNull()
            val streaks = streaksDeferred.await().getOrNull()

            _uiState.update {
                it.copy(
                    user = user,
                    healthProfile = health,
                    streaks = streaks,
                    isLoading = false
                )
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            authRepository.logout()
            _uiState.update { it.copy(isLoading = false) }
            onSuccess()
        }
    }
}
