package com.vitalai.ui.screens.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.remote.model.ActivityLogDto
import com.vitalai.data.repository.TrainingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val trainingRepository: TrainingRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ActivityUiState())
    val uiState = _uiState.asStateFlow()

    private var noteDebounceJob: Job? = null

    init { loadAll() }

    private fun loadAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val today = _uiState.value.selectedDate
            // Load today's log and 7-day range in parallel
            launch {
                trainingRepository.getActivityLog(today).onSuccess { log ->
                    _uiState.update { it.copy(log = log, isLoading = false) }
                }.onFailure { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                }
            }
            launch { loadWeeklyLogs() }
        }
    }

    private suspend fun loadWeeklyLogs() {
        val toDate = LocalDate.now().toString()
        val fromDate = LocalDate.now().minusDays(6).toString()
        trainingRepository.getActivityLogRange(fromDate, toDate).onSuccess { logs ->
            _uiState.update { it.copy(weeklyLogs = logs) }
        }
    }

    fun selectDate(date: String) {
        _uiState.update { it.copy(selectedDate = date, log = null, isLoading = true) }
        viewModelScope.launch {
            trainingRepository.getActivityLog(date).onSuccess { log ->
                _uiState.update { it.copy(log = log, isLoading = false) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun addWater(deltaMl: Int) {
        val current = _uiState.value.log?.waterMl ?: 0
        val newVal = (current + deltaMl).coerceAtLeast(0)
        viewModelScope.launch {
            trainingRepository.updateWater(newVal).onSuccess { log ->
                _uiState.update { it.copy(log = log) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateSteps(steps: Int) {
        viewModelScope.launch {
            trainingRepository.updateSteps(steps).onSuccess { log ->
                _uiState.update { it.copy(log = log) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateWater(ml: Int) {
        viewModelScope.launch {
            trainingRepository.updateWater(ml).onSuccess { log ->
                _uiState.update { it.copy(log = log) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
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

    fun scheduleNoteUpdate(note: String) {
        noteDebounceJob?.cancel()
        noteDebounceJob = viewModelScope.launch {
            delay(1000L)
            val date = _uiState.value.selectedDate
            trainingRepository.updateNote(note, date).onSuccess { log ->
                _uiState.update { it.copy(log = log) }
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
        noteDebounceJob?.cancel()
    }
}
