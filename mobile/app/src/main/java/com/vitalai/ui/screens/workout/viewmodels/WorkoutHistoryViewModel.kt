package com.vitalai.ui.screens.workout.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.remote.model.WorkoutSessionDto
import com.vitalai.data.repository.TrainingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            trainingRepository.getSessions(limit = 60).fold(
                onSuccess = { sessions ->
                    _uiState.update {
                        it.copy(
                            sessions = sessions.sortedByDescending { session -> session.sessionDate },
                            isLoading = false
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Không tải được lịch sử tập luyện"
                        )
                    }
                }
            )
        }
    }
}
