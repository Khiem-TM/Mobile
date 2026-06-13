package com.vitalai.ui.screens.workout.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.remote.model.WorkoutSessionDto
import com.vitalai.data.repository.TrainingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkoutSessionUiState(
    val session: WorkoutSessionDto? = null,
    val isLoading: Boolean = true,
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
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
    private var observeJob: Job? = null
    private var refreshJob: Job? = null

    fun load(id: String, date: String) {
        sessionId = id
        sessionDate = date
        observe()
        reload()
    }

    fun reload() {
        if (sessionId.isBlank() || sessionDate.isBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isInitialLoading = false,
                    isRefreshing = false,
                    error = "Không tìm thấy buổi tập"
                )
            }
            return
        }
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _uiState.update {
                val initial = it.session == null
                it.copy(
                    isRefreshing = true,
                    isInitialLoading = initial,
                    isLoading = initial,
                    error = null
                )
            }
            runCatching { trainingRepository.refreshSessions(sessionDate, sessionDate) }
                .onFailure { e ->
                    _uiState.update { state ->
                        state.copy(error = e.message.takeIf { state.session == null })
                    }
                }
            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    isInitialLoading = false,
                    isLoading = false,
                    error = it.error ?: if (it.session == null) "Không tìm thấy buổi tập" else null
                )
            }
        }
    }

    private fun observe() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            trainingRepository.observeSessionsByDate(sessionDate).collect { sessions ->
                val match = sessions.firstOrNull { it.id == sessionId }
                _uiState.update {
                    val initial = match == null && it.isRefreshing
                    it.copy(
                        session = match,
                        isInitialLoading = initial,
                        isLoading = initial,
                        error = if (match == null && !it.isRefreshing) "Không tìm thấy buổi tập" else null
                    )
                }
            }
        }
    }
}
