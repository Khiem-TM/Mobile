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
import java.io.File
import javax.inject.Inject

data class ProfileUiState(
    val user: UserDto? = null,
    val healthProfile: HealthProfileDto? = null,
    val streaks: StreakDto? = null,
    val isLoading: Boolean = false,
    val isLoggingOut: Boolean = false,
    val isUpdatingProfile: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val updateProfileError: String? = null
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
        observeRoomFlows()
        loadData()
    }

    private fun observeRoomFlows() {
        viewModelScope.launch {
            userRepository.observeCurrentUser().collect { user ->
                _uiState.update { it.copy(user = user) }
            }
        }
        viewModelScope.launch {
            userRepository.observeHealthProfile().collect { profile ->
                _uiState.update { it.copy(healthProfile = profile) }
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val streaksDeferred = async { dashboardRepository.getStreaks() }
            userRepository.refreshCurrentUser()
            userRepository.refreshHealthProfile()
            _uiState.update {
                it.copy(
                    streaks = streaksDeferred.await().getOrNull(),
                    isLoading = false
                )
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        if (_uiState.value.isLoggingOut) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true) }
            authRepository.logout()
            _uiState.update { it.copy(isLoggingOut = false) }
            onSuccess()
        }
    }

    fun updateProfile(displayName: String, avatarUrl: String, onSuccess: () -> Unit) {
        val currentUser = _uiState.value.user ?: return
        val trimmedName = displayName.trim()
        val trimmedAvatarUrl = avatarUrl.trim()

        if (trimmedName.length !in 2..100) {
            _uiState.update { it.copy(updateProfileError = "Tên hiển thị phải từ 2 đến 100 ký tự") }
            return
        }

        val changedName = trimmedName.takeIf { it != currentUser.displayName }
        val changedAvatarUrl = trimmedAvatarUrl
            .takeIf { it.isNotBlank() }
            ?.takeIf { it != currentUser.avatarUrl.orEmpty() }

        if (changedName == null && changedAvatarUrl == null) {
            _uiState.update { it.copy(updateProfileError = null) }
            onSuccess()
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingProfile = true, updateProfileError = null) }
            userRepository.updateProfile(
                userId = currentUser.id,
                displayName = changedName,
                avatarUrl = changedAvatarUrl
            ).onSuccess {
                // Room flow will auto-update user state
                _uiState.update { it.copy(isUpdatingProfile = false, updateProfileError = null) }
                onSuccess()
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isUpdatingProfile = false,
                        updateProfileError = error.message ?: "Không thể cập nhật hồ sơ"
                    )
                }
            }
        }
    }

    fun clearUpdateProfileError() {
        _uiState.update { it.copy(updateProfileError = null) }
    }

    fun setUpdateProfileError(message: String) {
        _uiState.update { it.copy(updateProfileError = message) }
    }

    fun uploadAvatar(file: File, mimeType: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingAvatar = true, updateProfileError = null) }
            userRepository.uploadAvatar(file, mimeType)
                .onSuccess {
                    // Room flow will auto-update user state
                    _uiState.update { it.copy(isUploadingAvatar = false, updateProfileError = null) }
                    onSuccess()
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isUploadingAvatar = false,
                            updateProfileError = error.message ?: "Không thể tải ảnh avatar"
                        )
                    }
                }
        }
    }
}
