package com.vitalai.ui.screens.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.remote.model.ActivityLogDto
import com.vitalai.data.repository.TrainingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ActivityUiState(
    val log: ActivityLogDto? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ActivityViewModel @Inject constructor(
    private val trainingRepository: TrainingRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ActivityUiState())
    val uiState = _uiState.asStateFlow()

    init { loadData() }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val date = LocalDate.now().toString()
            trainingRepository.getActivityLog(date).fold(
                onSuccess = { log -> _uiState.update { it.copy(log = log, isLoading = false) } },
                onFailure = { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
            )
        }
    }

    fun updateSteps(steps: Int) {
        viewModelScope.launch {
            trainingRepository.updateSteps(steps).fold(
                onSuccess = { log -> _uiState.update { it.copy(log = log) } },
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
            )
        }
    }

    fun updateWater(ml: Int) {
        viewModelScope.launch {
            trainingRepository.updateWater(ml).fold(
                onSuccess = { log -> _uiState.update { it.copy(log = log) } },
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
            )
        }
    }

    fun updateCaloriesBurned(calories: Float, activeMinutes: Int, notes: String? = null) {
        viewModelScope.launch {
            trainingRepository.updateCaloriesBurned(calories, activeMinutes, exerciseNotes = notes).fold(
                onSuccess = { log -> _uiState.update { it.copy(log = log) } },
                onFailure = { e -> _uiState.update { it.copy(error = e.message) } }
            )
        }
    }
}
