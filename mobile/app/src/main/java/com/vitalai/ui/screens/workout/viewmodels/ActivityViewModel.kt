package com.vitalai.ui.screens.workout.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.remote.model.ActivityLogDto
import com.vitalai.data.repository.TrainingRepository
import com.vitalai.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import java.time.LocalDate
import javax.inject.Inject

data class ActivityUiState(
    val selectedDate: String = LocalDate.now().toString(),
    val log: ActivityLogDto? = null,
    val weeklyLogs: List<ActivityLogDto> = emptyList(),
    val waterGoalMl: Int = 2500,
    val stepGoal: Int = 10000,
    val isLoading: Boolean = false,
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ActivityViewModel @Inject constructor(
    private val trainingRepository: TrainingRepository,
    private val userRepository: UserRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ActivityUiState())
    val uiState = _uiState.asStateFlow()

    private var noteSaveJob: Job? = null
    private var refreshJob: Job? = null
    private val selectedDateFlow = MutableStateFlow(LocalDate.now().toString())

    init {
        observeRoomFlows()
        refreshSelectedDate()
    }

    private fun observeRoomFlows() {
        viewModelScope.launch {
            selectedDateFlow.flatMapLatest { date ->
                trainingRepository.observeActivityLog(date)
            }.collect { log ->
                _uiState.update {
                    val initial = log == null && it.isRefreshing
                    it.copy(
                        selectedDate = selectedDateFlow.value,
                        log = log,
                        isInitialLoading = initial,
                        isLoading = initial,
                        error = if (log != null) null else it.error
                    )
                }
            }
        }

        viewModelScope.launch {
            selectedDateFlow.flatMapLatest { date ->
                val anchor = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now())
                trainingRepository.observeActivityLogsBetween(anchor.minusDays(6).toString(), anchor.toString())
            }.collect { logs ->
                _uiState.update { it.copy(weeklyLogs = logs) }
            }
        }

        viewModelScope.launch {
            userRepository.observeHealthProfile().collect { profile ->
                if (profile != null) {
                    _uiState.update {
                        it.copy(
                            waterGoalMl = profile.waterGoalMl ?: it.waterGoalMl,
                            stepGoal = profile.stepGoal ?: it.stepGoal
                        )
                    }
                }
            }
        }
    }

    fun selectDate(date: String) {
        selectedDateFlow.value = date
        _uiState.update {
            it.copy(
                selectedDate = date,
                log = null,
                isInitialLoading = true,
                isLoading = true,
                error = null
            )
        }
        refreshSelectedDate()
    }

    private fun refreshSelectedDate() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val date = selectedDateFlow.value
            val anchor = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now())
            val fromDate = anchor.minusDays(6).toString()
            val toDate = anchor.toString()
            _uiState.update {
                val initial = it.log == null
                it.copy(
                    isRefreshing = true,
                    isInitialLoading = initial,
                    isLoading = initial,
                    error = null
                )
            }
            supervisorScope {
                launch { runCatching { trainingRepository.refreshActivityLog(date) } }
                launch { runCatching { trainingRepository.refreshActivityLogRange(fromDate, toDate) } }
                launch { runCatching { userRepository.refreshHealthProfile() } }
            }
            _uiState.update { it.copy(isRefreshing = false, isInitialLoading = false, isLoading = false) }
        }
    }

    // Water/Steps optimistic: cập nhật UI ngay + đẩy hàng đợi đồng bộ nền (no await/refetch).
    fun addWater(deltaMl: Int) {
        val current = _uiState.value.log?.waterMl ?: 0
        val newVal = (current + deltaMl).coerceAtLeast(0)
        val date = _uiState.value.selectedDate
        _uiState.update { it.copy(log = (it.log ?: ActivityLogDto(logDate = date)).copy(waterMl = newVal)) }
        viewModelScope.launch { trainingRepository.enqueueWater(newVal, date) }
    }

    fun updateSteps(steps: Int) {
        val newVal = steps.coerceAtLeast(0)
        val date = _uiState.value.selectedDate
        _uiState.update { it.copy(log = (it.log ?: ActivityLogDto(logDate = date)).copy(steps = newVal)) }
        viewModelScope.launch { trainingRepository.enqueueSteps(newVal, date) }
    }

    fun updateWater(ml: Int) {
        val newVal = ml.coerceAtLeast(0)
        val date = _uiState.value.selectedDate
        _uiState.update { it.copy(log = (it.log ?: ActivityLogDto(logDate = date)).copy(waterMl = newVal)) }
        viewModelScope.launch { trainingRepository.enqueueWater(newVal, date) }
    }

    fun updateSleep(sleepHours: Float) {
        val date = _uiState.value.selectedDate
        _uiState.update { it.copy(log = (it.log ?: ActivityLogDto(logDate = date)).copy(sleepHours = sleepHours)) }
        viewModelScope.launch {
            runCatching { trainingRepository.enqueueSleep(sleepHours, date) }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun updateMood(mood: String) {
        val date = _uiState.value.selectedDate
        _uiState.update { it.copy(log = (it.log ?: ActivityLogDto(logDate = date)).copy(mood = mood)) }
        viewModelScope.launch {
            runCatching { trainingRepository.enqueueMood(mood, date) }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun saveNote(note: String) {
        noteSaveJob?.cancel()
        noteSaveJob = viewModelScope.launch {
            val date = _uiState.value.selectedDate
            val trimmed = note.trim()
            _uiState.update { it.copy(log = (it.log ?: ActivityLogDto(logDate = date)).copy(note = trimmed)) }
            runCatching { trainingRepository.enqueueNote(trimmed, date) }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun updateCaloriesBurned(calories: Float, activeMinutes: Int, notes: String? = null) {
        viewModelScope.launch {
            val date = _uiState.value.selectedDate
            trainingRepository.updateCaloriesBurned(calories, activeMinutes, date, notes).onSuccess { log ->
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
