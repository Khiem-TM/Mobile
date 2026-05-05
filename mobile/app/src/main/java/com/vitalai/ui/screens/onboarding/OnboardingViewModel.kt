package com.vitalai.ui.screens.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.remote.UserApi
import com.vitalai.data.remote.model.HealthProfileDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class OnboardingState {
    object Idle : OnboardingState()
    object Loading : OnboardingState()
    object Success : OnboardingState()
    data class Error(val message: String) : OnboardingState()
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userApi: UserApi,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<OnboardingState>(OnboardingState.Idle)
    val uiState = _uiState.asStateFlow()

    // Using SavedStateHandle to survive process death
    var gender = savedStateHandle.get<String>("gender") ?: ""
        set(value) {
            field = value
            savedStateHandle["gender"] = value
        }

    var birthDate = savedStateHandle.get<String>("birthDate") ?: ""
        set(value) {
            field = value
            savedStateHandle["birthDate"] = value
        }

    var heightCm = savedStateHandle.get<Float>("heightCm") ?: 170f
        set(value) {
            field = value
            savedStateHandle["heightCm"] = value
        }

    var weightKg = savedStateHandle.get<Float>("weightKg") ?: 65f
        set(value) {
            field = value
            savedStateHandle["weightKg"] = value
        }

    var activityLevel = savedStateHandle.get<String>("activityLevel") ?: "moderately_active"
        set(value) {
            field = value
            savedStateHandle["activityLevel"] = value
        }

    var goalType = savedStateHandle.get<String>("goalType") ?: "maintain"
        set(value) {
            field = value
            savedStateHandle["goalType"] = value
        }

    fun submitProfile() {
        viewModelScope.launch {
            _uiState.value = OnboardingState.Loading
            try {
                val profile = HealthProfileDto(
                    birthDate = if (birthDate.isNotEmpty()) birthDate else null,
                    gender = gender,
                    heightCm = heightCm,
                    initialWeightKg = weightKg,
                    activityLevel = activityLevel,
                    goalType = goalType
                )
                val response = userApi.updateHealthProfile(profile)
                if (response.isSuccessful && response.body()?.success == true) {
                    _uiState.value = OnboardingState.Success
                } else {
                    _uiState.value = OnboardingState.Error("Không thể cập nhật hồ sơ: ${response.message()}")
                }
            } catch (e: Exception) {
                _uiState.value = OnboardingState.Error(e.message ?: "Đã xảy ra lỗi")
            }
        }
    }
}
