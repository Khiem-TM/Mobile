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
import kotlinx.coroutines.Job
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
    private var observeExerciseJob: Job? = null

    fun loadExercise(id: String) {
        if (id == LOTTIE_TEST_EXERCISE_ID) {
            _uiState.value = ExerciseDetailUiState(exercise = LOTTIE_TEST_EXERCISE, isLoading = false)
            return
        }
        _uiState.value = ExerciseDetailUiState(isLoading = true)
        observeExerciseJob?.cancel()
        observeExerciseJob = viewModelScope.launch {
            repo.observeExercise(id).collect { cached ->
                if (cached != null) {
                    _uiState.value = _uiState.value.copy(
                        exercise = cached,
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
        viewModelScope.launch {
            repo.getExerciseById(id).onSuccess { exercise ->
                _uiState.value = _uiState.value.copy(exercise = exercise, isLoading = false)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = if (_uiState.value.exercise == null) {
                        e.message ?: "Không tìm thấy bài tập"
                    } else {
                        null
                    }
                )
            }
        }
    }

    fun toggleFavorite() {
        val exercise = _uiState.value.exercise ?: return
        val nextFavorite = !exercise.isFavorite
        _uiState.value = _uiState.value.copy(exercise = exercise.copy(isFavorite = nextFavorite))
        viewModelScope.launch {
            val result = if (nextFavorite) repo.addFavorite(exercise.id) else repo.removeFavorite(exercise.id)
            result.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    exercise = _uiState.value.exercise?.copy(isFavorite = exercise.isFavorite),
                    error = error.message ?: "Không thể cập nhật yêu thích"
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
