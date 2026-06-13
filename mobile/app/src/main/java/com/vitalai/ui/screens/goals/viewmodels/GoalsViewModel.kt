package com.vitalai.ui.screens.goals.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.core.error.AppErrorMapper
import com.vitalai.data.remote.model.HealthProfileDto
import com.vitalai.data.repository.HealthProfileMissingException
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
    val stepGoal: String = "",
    val targetWeightKg: String = "",
    val weeklyRateKg: String = "",
    val goalType: String = "",
    val activityLevel: String = "",
    val tdee: Float? = null
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
                applyProfile(p, isDraft = false)
            }.onFailure { e ->
                if (e is HealthProfileMissingException) {
                    applyProfile(defaultDraftProfile(), isDraft = true)
                } else {
                    _uiState.update {
                        it.copy(isLoading = false, error = AppErrorMapper.fromThrowable(e).userMessage)
                    }
                }
            }
            // Refresh from network silently — GoalsViewModel intentionally does NOT
            // auto-apply updates to avoid overwriting form fields the user is editing
            userRepository.refreshHealthProfile()
        }
    }

    private fun applyProfile(p: HealthProfileDto, isDraft: Boolean) {
        _uiState.update {
            it.copy(
                isLoading = false,
                profile = p,
                error = if (isDraft) null else it.error,
                dailyCaloriesGoal = p.dailyCaloriesGoal?.toFloatString() ?: p.caloriesGoal?.toFloatString() ?: "",
                proteinGoalG = p.proteinGoalG?.toFloatString() ?: "",
                carbsGoalG = p.carbsGoalG?.toFloatString() ?: "",
                fatGoalG = p.fatGoalG?.toFloatString() ?: "",
                waterGoalMl = p.waterGoalMl?.toString() ?: "2500",
                stepGoal = p.stepGoal?.toString() ?: "10000",
                targetWeightKg = p.targetWeightKg?.toFloatString() ?: "",
                weeklyRateKg = p.weeklyRateKg?.toFloatString() ?: "",
                goalType = p.goalType ?: "maintain",
                activityLevel = p.activityLevel ?: "moderately_active",
                tdee = computeTdee(p, p.activityLevel ?: "moderately_active", p.goalType ?: "maintain")
            )
        }
    }

    private fun computeTdee(p: HealthProfileDto, activityLvl: String, goalType: String): Float? {
        val age = com.vitalai.ui.screens.metrics.calculateAge(p.birthDate)
        val bmr = com.vitalai.ui.screens.metrics.calculateBmr(p.gender, p.initialWeightKg, p.heightCm, age)
        val baseTdee = com.vitalai.ui.screens.metrics.calculateTdee(bmr, activityLvl) ?: return null
        
        val offset = when (goalType) {
            "lose_weight", "cutting" -> -500f
            "gain_weight", "bulking" -> 500f
            "gain_muscle" -> 300f
            else -> 0f
        }
        return baseTdee + offset
    }

    fun onDailyCalories(v: String) = _uiState.update { it.copy(dailyCaloriesGoal = v) }
    fun onProtein(v: String) = _uiState.update { it.copy(proteinGoalG = v) }
    fun onCarbs(v: String) = _uiState.update { it.copy(carbsGoalG = v) }
    fun onFat(v: String) = _uiState.update { it.copy(fatGoalG = v) }
    fun onWater(v: String) = _uiState.update { it.copy(waterGoalMl = v) }
    fun onStepGoal(v: String) = _uiState.update { it.copy(stepGoal = v) }
    fun onTargetWeight(v: String) = _uiState.update { it.copy(targetWeightKg = v) }
    fun onWeeklyRate(v: String) = _uiState.update { it.copy(weeklyRateKg = v) }

    fun onGoalType(v: String) = _uiState.update { state -> 
        applyTemplates(state.copy(goalType = v))
    }
    fun onActivityLevel(v: String) = _uiState.update { state -> 
        applyTemplates(state.copy(activityLevel = v))
    }

    private data class GoalTemplate(
        val pPct: Float, val cPct: Float, val fPct: Float, 
        val steps: Int, val water: Int, val weeklyRate: Float
    )

    private fun applyTemplates(state: GoalsUiState): GoalsUiState {
        val p = state.profile ?: defaultDraftProfile()
        val targetCals = computeTdee(p, state.activityLevel, state.goalType) ?: 2000f

        val tmpl = when (state.goalType) {
            "lose_weight" -> GoalTemplate(0.30f, 0.40f, 0.30f, 10000, 2500, 0.5f)
            "gain_weight" -> GoalTemplate(0.25f, 0.50f, 0.25f, 8000, 2500, 0.5f)
            "gain_muscle" -> GoalTemplate(0.35f, 0.45f, 0.20f, 8000, 3000, 0.2f)
            "improve_endurance" -> GoalTemplate(0.25f, 0.55f, 0.20f, 12000, 3500, 0f)
            "bulking" -> GoalTemplate(0.30f, 0.50f, 0.20f, 8000, 3000, 0.5f)
            "cutting" -> GoalTemplate(0.40f, 0.30f, 0.30f, 10000, 3000, 0.5f)
            else -> GoalTemplate(0.30f, 0.40f, 0.30f, 10000, 2500, 0f) // maintain
        }

        val pGrams = (targetCals * tmpl.pPct) / 4f
        val cGrams = (targetCals * tmpl.cPct) / 4f
        val fGrams = (targetCals * tmpl.fPct) / 9f

        val targetWeight = when {
            state.goalType in listOf("lose_weight", "cutting") -> (p.initialWeightKg ?: 65f) - 5f
            state.goalType in listOf("gain_weight", "gain_muscle", "bulking") -> (p.initialWeightKg ?: 65f) + 5f
            else -> p.initialWeightKg ?: 65f
        }

        return state.copy(
            tdee = targetCals,
            proteinGoalG = pGrams.toInt().toString(),
            carbsGoalG = cGrams.toInt().toString(),
            fatGoalG = fGrams.toInt().toString(),
            waterGoalMl = tmpl.water.toString(),
            stepGoal = tmpl.steps.toString(),
            targetWeightKg = targetWeight.toFloatString(),
            weeklyRateKg = tmpl.weeklyRate.toFloatString()
        )
    }

    fun save() {
        val s = _uiState.value
        // Basic validation: values, if present, must be non-negative numbers.
        val numericFields = listOf(
            s.dailyCaloriesGoal, s.proteinGoalG, s.carbsGoalG, s.fatGoalG,
            s.waterGoalMl, s.stepGoal, s.targetWeightKg, s.weeklyRateKg
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
                dailyCaloriesGoal = s.tdee ?: base.dailyCaloriesGoal,
                caloriesGoal = s.tdee ?: base.caloriesGoal,
                proteinGoalG = s.proteinGoalG.toFloatOrNull(),
                carbsGoalG = s.carbsGoalG.toFloatOrNull(),
                fatGoalG = s.fatGoalG.toFloatOrNull(),
                waterGoalMl = s.waterGoalMl.toIntOrNull() ?: base.waterGoalMl,
                stepGoal = s.stepGoal.toIntOrNull() ?: base.stepGoal ?: 10000,
                targetWeightKg = s.targetWeightKg.toFloatOrNull(),
                weeklyRateKg = s.weeklyRateKg.toFloatOrNull(),
                goalType = s.goalType.ifBlank { null },
                activityLevel = s.activityLevel.ifBlank { null }
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

private fun defaultDraftProfile(): HealthProfileDto = HealthProfileDto(
    birthDate = "1990-01-01",
    gender = "other",
    heightCm = 170f,
    initialWeightKg = 65f,
    activityLevel = "moderately_active",
    goalType = "maintain",
    waterGoalMl = 2500,
    stepGoal = 10000
)

/** Render a float without a trailing ".0" for whole numbers. */
private fun Float.toFloatString(): String =
    if (this % 1f == 0f) this.toInt().toString() else this.toString()
