package com.vitalai.ui.screens.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vitalai.data.remote.UserApi
import com.vitalai.data.remote.model.OnboardingRequest
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
    private var _gender by mutableStateOf(savedStateHandle.get<String>("gender") ?: "male")
    var gender: String
        get() = _gender
        set(value) {
            _gender = value
            savedStateHandle["gender"] = value
        }

    private var _birthDate by mutableStateOf(savedStateHandle.get<String>("birthDate") ?: "1996-01-01")
    var birthDate: String
        get() = _birthDate
        set(value) {
            _birthDate = value
            savedStateHandle["birthDate"] = value
        }

    private var _heightCm by mutableStateOf(savedStateHandle.get<Float>("heightCm") ?: 170f)
    var heightCm: Float
        get() = _heightCm
        set(value) {
            _heightCm = value.coerceIn(50f, 300f)
            savedStateHandle["heightCm"] = _heightCm
        }

    private var _weightKg by mutableStateOf(savedStateHandle.get<Float>("weightKg") ?: 65f)
    var weightKg: Float
        get() = _weightKg
        set(value) {
            _weightKg = value.coerceIn(20f, 500f)
            savedStateHandle["weightKg"] = _weightKg
        }

    private var _activityLevel by mutableStateOf(savedStateHandle.get<String>("activityLevel") ?: "moderately_active")
    var activityLevel: String
        get() = _activityLevel
        set(value) {
            _activityLevel = value
            savedStateHandle["activityLevel"] = value
        }

    private var _goalType by mutableStateOf(savedStateHandle.get<String>("goalType") ?: "maintain")
    var goalType: String
        get() = _goalType
        set(value) {
            _goalType = value
            savedStateHandle["goalType"] = value
        }

    fun submitProfile() {
        if (_uiState.value is OnboardingState.Loading) return
        viewModelScope.launch {
            val validationError = validate()
            if (validationError != null) {
                _uiState.value = OnboardingState.Error(validationError)
                return@launch
            }
            _uiState.value = OnboardingState.Loading
            try {
                val request = OnboardingRequest(
                    birthDate = birthDate,
                    gender = gender,
                    heightCm = heightCm,
                    initialWeightKg = weightKg,
                    activityLevel = activityLevel,
                    goalType = goalType,
                    targetWeightKg = targetWeightForGoal(),
                    dietType = "balanced",
                    foodAllergies = emptyList()
                )
                val response = userApi.completeOnboarding(request)
                if (response.isSuccessful && response.body()?.success == true) {
                    _uiState.value = OnboardingState.Success
                } else {
                    val error = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                    _uiState.value = OnboardingState.Error(
                        error ?: "Không thể cập nhật hồ sơ (${response.code()}): ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = OnboardingState.Error(e.message ?: "Đã xảy ra lỗi")
            }
        }
    }

    fun isStepValid(step: Int): Boolean = when (step) {
        1 -> gender in setOf("male", "female", "other") && birthDate.take(4).toIntOrNull() in 1900..2020
        2 -> heightCm in 50f..300f && weightKg in 20f..500f
        3 -> goalType in setOf("lose_weight", "gain_weight", "gain_muscle", "improve_endurance", "bulking", "cutting", "maintain")
        4 -> activityLevel in setOf("sedentary", "lightly_active", "moderately_active", "very_active", "extra_active")
        else -> false
    }

    private fun validate(): String? {
        for (step in 1..4) {
            if (!isStepValid(step)) return "Thông tin bước $step chưa hợp lệ. Vui lòng kiểm tra lại."
        }
        return null
    }

    private fun targetWeightForGoal(): Float {
        val adjustment = when (goalType) {
            "lose_weight" -> -6f
            "gain_muscle" -> 4f
            else -> 0f
        }
        return (weightKg + adjustment).coerceIn(20f, 500f)
    }
}
