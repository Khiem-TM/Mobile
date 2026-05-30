package com.vitalai.ui.screens.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.core.error.AppErrorMapper
import com.vitalai.data.remote.model.HealthProfileDto
import com.vitalai.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoalsUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null,
    // The full profile is kept so a save preserves non-goal fields.
    val profile: HealthProfileDto? = null,
    // Editable form fields (strings for text inputs).
    val dailyCaloriesGoal: String = "",
    val proteinGoalG: String = "",
    val carbsGoalG: String = "",
    val fatGoalG: String = "",
    val waterGoalMl: String = "",
    val targetWeightKg: String = "",
    val weeklyRateKg: String = "",
    val goalType: String = ""
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            userRepository.getHealthProfile().onSuccess { p ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        profile = p,
                        dailyCaloriesGoal = p.dailyCaloriesGoal?.toFloatString() ?: p.caloriesGoal?.toFloatString() ?: "",
                        proteinGoalG = p.proteinGoalG?.toFloatString() ?: "",
                        carbsGoalG = p.carbsGoalG?.toFloatString() ?: "",
                        fatGoalG = p.fatGoalG?.toFloatString() ?: "",
                        waterGoalMl = p.waterGoalMl?.toString() ?: "",
                        targetWeightKg = p.targetWeightKg?.toFloatString() ?: p.weightGoalKg?.toFloatString() ?: "",
                        weeklyRateKg = p.weeklyRateKg?.toFloatString() ?: "",
                        goalType = p.goalType ?: ""
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(isLoading = false, error = AppErrorMapper.fromThrowable(e).userMessage)
                }
            }
        }
    }

    fun onDailyCalories(v: String) = _uiState.update { it.copy(dailyCaloriesGoal = v) }
    fun onProtein(v: String) = _uiState.update { it.copy(proteinGoalG = v) }
    fun onCarbs(v: String) = _uiState.update { it.copy(carbsGoalG = v) }
    fun onFat(v: String) = _uiState.update { it.copy(fatGoalG = v) }
    fun onWater(v: String) = _uiState.update { it.copy(waterGoalMl = v) }
    fun onTargetWeight(v: String) = _uiState.update { it.copy(targetWeightKg = v) }
    fun onWeeklyRate(v: String) = _uiState.update { it.copy(weeklyRateKg = v) }
    fun onGoalType(v: String) = _uiState.update { it.copy(goalType = v) }

    fun save() {
        val s = _uiState.value
        // Basic validation: values, if present, must be non-negative numbers.
        val numericFields = listOf(
            s.dailyCaloriesGoal, s.proteinGoalG, s.carbsGoalG, s.fatGoalG,
            s.waterGoalMl, s.targetWeightKg, s.weeklyRateKg
        )
        if (numericFields.any { it.isNotBlank() && (it.toFloatOrNull() == null || it.toFloat() < 0f) }) {
            _uiState.update { it.copy(error = "Giá trị mục tiêu không hợp lệ.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            // Preserve existing profile fields, override only the goal-related ones.
            val base = s.profile ?: HealthProfileDto()
            val updated = base.copy(
                dailyCaloriesGoal = s.dailyCaloriesGoal.toFloatOrNull(),
                caloriesGoal = s.dailyCaloriesGoal.toFloatOrNull() ?: base.caloriesGoal,
                proteinGoalG = s.proteinGoalG.toFloatOrNull(),
                carbsGoalG = s.carbsGoalG.toFloatOrNull(),
                fatGoalG = s.fatGoalG.toFloatOrNull(),
                waterGoalMl = s.waterGoalMl.toIntOrNull() ?: base.waterGoalMl,
                targetWeightKg = s.targetWeightKg.toFloatOrNull(),
                weeklyRateKg = s.weeklyRateKg.toFloatOrNull(),
                goalType = s.goalType.ifBlank { null }
            )
            userRepository.updateHealthProfile(updated).onSuccess { p ->
                _uiState.update { it.copy(isSaving = false, saveSuccess = true, profile = p) }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(isSaving = false, error = AppErrorMapper.fromThrowable(e).userMessage)
                }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}

/** Render a float without a trailing ".0" for whole numbers. */
private fun Float.toFloatString(): String =
    if (this % 1f == 0f) this.toInt().toString() else this.toString()
