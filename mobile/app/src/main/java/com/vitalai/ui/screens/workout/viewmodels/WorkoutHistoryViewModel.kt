package com.vitalai.ui.screens.workout.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.remote.model.WorkoutSessionDto
import com.vitalai.data.repository.TrainingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkoutHistoryUiState(
    val sessions: List<WorkoutSessionDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class WorkoutHistoryViewModel @Inject constructor(
    private val trainingRepository: TrainingRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(WorkoutHistoryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observe()
        refresh()
    }

    private fun observe() {
        viewModelScope.launch {
            trainingRepository.observeRecentSessions(60).collect { sessions ->
                _uiState.update {
                    it.copy(
                        sessions = sessions.sortedByDescending { session -> session.sessionDate },
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }

    fun loadData() {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val today = LocalDate.now()
            runCatching {
                trainingRepository.refreshSessions(today.minusDays(60).toString(), today.toString())
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
