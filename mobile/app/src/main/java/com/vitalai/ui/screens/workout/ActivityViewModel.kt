package com.vitalai.ui.screens.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.remote.model.ActivityLogDto
import com.vitalai.data.repository.TrainingRepository
import com.vitalai.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ActivityUiState(
    val selectedDate: String = LocalDate.now().toString(),
    val log: ActivityLogDto? = null,
    val weeklyLogs: List<ActivityLogDto> = emptyList(),
    val waterGoalMl: Int = 2500,
    val stepGoal: Int = 10000,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val trainingRepository: TrainingRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ActivityUiState())
    val uiState = _uiState.asStateFlow()

    private var noteSaveJob: Job? = null

    init { loadAll() }

    private fun loadAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val today = _uiState.value.selectedDate
            val logDeferred = async { trainingRepository.getActivityLog(today) }
            val profileDeferred = async { userRepository.getHealthProfile() }
            val logResult = logDeferred.await()
            val profile = profileDeferred.await().getOrNull()

            logResult.onSuccess { log ->
                _uiState.update {
                    it.copy(
                        log = log,
                        waterGoalMl = profile?.waterGoalMl ?: it.waterGoalMl,
                        stepGoal = profile?.stepGoal ?: it.stepGoal,
                        isLoading = false
                    )
                }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
            launch { loadWeeklyLogs() }
        }
    }

    private suspend fun loadWeeklyLogs() {
        val anchor = runCatching { LocalDate.parse(_uiState.value.selectedDate) }.getOrDefault(LocalDate.now())
        val toDate = anchor.toString()
        val fromDate = anchor.minusDays(6).toString()
        trainingRepository.getActivityLogRange(fromDate, toDate).onSuccess { logs ->
            _uiState.update { it.copy(weeklyLogs = logs) }
        }
    }

    fun selectDate(date: String) {
        _uiState.update { it.copy(selectedDate = date, log = null, isLoading = true) }
        viewModelScope.launch {
            val result = trainingRepository.getActivityLog(date)
            result.onSuccess { log ->
                _uiState.update { it.copy(log = log, isLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
            if (result.isSuccess) loadWeeklyLogs()
        }
    }

    fun addWater(deltaMl: Int) {
        val current = _uiState.value.log?.waterMl ?: 0
        val newVal = (current + deltaMl).coerceAtLeast(0)
        val date = _uiState.value.selectedDate
        viewModelScope.launch {
            val result = trainingRepository.updateWater(newVal, date)
            result.onSuccess { log ->
                _uiState.update { it.copy(log = log) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
            if (result.isSuccess) loadWeeklyLogs()
        }
    }

    fun updateSteps(steps: Int) {
        val date = _uiState.value.selectedDate
        viewModelScope.launch {
            val result = trainingRepository.updateSteps(steps, date)
            result.onSuccess { log ->
                _uiState.update { it.copy(log = log) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
            if (result.isSuccess) loadWeeklyLogs()
        }
    }

    fun updateWater(ml: Int) {
        val date = _uiState.value.selectedDate
        viewModelScope.launch {
            val result = trainingRepository.updateWater(ml, date)
            result.onSuccess { log ->
                _uiState.update { it.copy(log = log) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
            if (result.isSuccess) loadWeeklyLogs()
        }
    }

    fun updateSleep(sleepHours: Float) {
        val date = _uiState.value.selectedDate
        viewModelScope.launch {
            trainingRepository.updateSleep(sleepHours, date).onSuccess { log ->
                _uiState.update { it.copy(log = log) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateMood(mood: String) {
        val date = _uiState.value.selectedDate
        viewModelScope.launch {
            trainingRepository.updateMood(mood, date).onSuccess { log ->
                _uiState.update { it.copy(log = log) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun saveNote(note: String) {
        noteSaveJob?.cancel()
        noteSaveJob = viewModelScope.launch {
            val date = _uiState.value.selectedDate
            trainingRepository.updateNote(note.trim(), date).onSuccess { log ->
                _uiState.update { it.copy(log = log) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateCaloriesBurned(calories: Float, activeMinutes: Int, notes: String? = null) {
        viewModelScope.launch {
            trainingRepository.updateCaloriesBurned(calories, activeMinutes, exerciseNotes = notes).onSuccess { log ->
                _uiState.update { it.copy(log = log) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        noteSaveJob?.cancel()
    }
}
