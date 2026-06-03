package com.vitalai.ui.screens.workout.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.remote.model.AddExerciseRequest
import com.vitalai.data.remote.model.CreateWorkoutSessionDto
import com.vitalai.data.remote.model.ExerciseDto
import com.vitalai.data.remote.model.WorkoutSessionDto
import com.vitalai.data.repository.TrainingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ExerciseDetailUiState(
    val exercise: ExerciseDto? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ExerciseDetailViewModel @Inject constructor(
    private val repo: TrainingRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExerciseDetailUiState())
    val uiState: StateFlow<ExerciseDetailUiState> = _uiState.asStateFlow()

    // Called from ExerciseLibraryViewModel to share exercise data
    fun setExercise(exercise: ExerciseDto) {
        _uiState.value = _uiState.value.copy(exercise = exercise)
    }

    fun loadExercise(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            repo.getExerciseById(id).onSuccess { exercise ->
                _uiState.value = _uiState.value.copy(exercise = exercise, isLoading = false)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Không tìm thấy bài tập"
                )
            }
        }
    }

    fun addToTodaySession(request: AddExerciseRequest) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, error = null, saveSuccess = false)
            val today = LocalDate.now().toString()
            // Check if session exists today
            val sessionsResult = repo.getSessions(date = today)
            val sessions = sessionsResult.getOrDefault(emptyList())
            val sessionId = if (sessions.isNotEmpty()) {
                sessions.first().id
            } else {
                // Create new session for today
                val createResult = repo.createSession(
                    CreateWorkoutSessionDto(sessionDate = today, sessionName = null, details = emptyList())
                )
                createResult.getOrNull()?.id
            }
            if (sessionId == null) {
                _uiState.value = _uiState.value.copy(isSaving = false, error = "Không thể tạo buổi tập")
                return@launch
            }
            val addResult = repo.addExercise(sessionId, request)
            if (addResult.isSuccess) {
                _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
            } else {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = addResult.exceptionOrNull()?.message ?: "Lỗi thêm bài tập"
                )
            }
        }
    }

    fun clearSaveState() {
        _uiState.value = _uiState.value.copy(saveSuccess = false, error = null)
    }
}
