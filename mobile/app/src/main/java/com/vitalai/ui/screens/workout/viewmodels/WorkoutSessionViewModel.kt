package com.vitalai.ui.screens.workout.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.remote.model.WorkoutSessionDto
import com.vitalai.data.repository.TrainingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WorkoutSessionUiState(
    val session: WorkoutSessionDto? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class WorkoutSessionViewModel @Inject constructor(
    private val trainingRepository: TrainingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutSessionUiState())
    val uiState = _uiState.asStateFlow()

    private var sessionId: String = ""
    private var sessionDate: String = ""

    fun load(id: String, date: String) {
        sessionId = id
        sessionDate = date
        reload()
    }

    fun reload() {
        if (sessionId.isBlank() || sessionDate.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "Không tìm thấy buổi tập") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            trainingRepository.getSessions(date = sessionDate).fold(
                onSuccess = { sessions ->
                    val match = sessions.firstOrNull { it.id == sessionId }
                    _uiState.update {
                        it.copy(
                            session = match,
                            isLoading = false,
                            error = if (match == null) "Không tìm thấy buổi tập" else null
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message ?: "Lỗi tải buổi tập") }
                }
            )
        }
    }
}
