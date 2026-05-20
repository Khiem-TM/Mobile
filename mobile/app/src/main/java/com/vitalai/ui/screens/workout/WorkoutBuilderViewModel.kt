package com.vitalai.ui.screens.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.remote.model.CreateSessionRequest
import com.vitalai.data.remote.model.ExerciseDto
import com.vitalai.data.repository.TrainingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class SetEntry(
    val setNumber: Int,
    val reps: String = "10",
    val kg: String = "0",
    val isDone: Boolean = false
)

data class BuilderExercise(
    val exercise: ExerciseDto,
    val sets: MutableList<SetEntry> = mutableListOf(SetEntry(1))
)

data class WorkoutBuilderUiState(
    val sessionName: String = "Buổi tập mới",
    val date: String = LocalDate.now().toString(),
    val exercises: List<BuilderExercise> = emptyList(),
    val elapsedSeconds: Int = 0,
    val isRunning: Boolean = false,
    val isSaving: Boolean = false,
    val savedSessionId: String? = null,
    val error: String? = null,
    val showExercisePicker: Boolean = false
)

@HiltViewModel
class WorkoutBuilderViewModel @Inject constructor(
    private val trainingRepository: TrainingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutBuilderUiState())
    val uiState = _uiState.asStateFlow()

    private var timerJob: kotlinx.coroutines.Job? = null

    init { startTimer() }

    fun startTimer() {
        _uiState.update { it.copy(isRunning = true) }
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
            }
        }
    }

    fun setSessionName(name: String) {
        _uiState.update { it.copy(sessionName = name) }
    }

    fun addSet(exerciseIdx: Int) {
        _uiState.update { state ->
            val updated = state.exercises.toMutableList()
            if (exerciseIdx in updated.indices) {
                val ex = updated[exerciseIdx]
                val newSets = ex.sets.toMutableList()
                newSets.add(SetEntry(newSets.size + 1))
                updated[exerciseIdx] = ex.copy(sets = newSets)
            }
            state.copy(exercises = updated)
        }
    }

    fun updateSet(exerciseIdx: Int, setIdx: Int, reps: String? = null, kg: String? = null, isDone: Boolean? = null) {
        _uiState.update { state ->
            val updated = state.exercises.toMutableList()
            if (exerciseIdx in updated.indices) {
                val ex = updated[exerciseIdx]
                val newSets = ex.sets.toMutableList()
                if (setIdx in newSets.indices) {
                    val set = newSets[setIdx]
                    newSets[setIdx] = set.copy(
                        reps = reps ?: set.reps,
                        kg = kg ?: set.kg,
                        isDone = isDone ?: set.isDone
                    )
                    updated[exerciseIdx] = ex.copy(sets = newSets)
                }
            }
            state.copy(exercises = updated)
        }
    }

    fun removeExercise(idx: Int) {
        _uiState.update { state ->
            val updated = state.exercises.toMutableList()
            if (idx in updated.indices) updated.removeAt(idx)
            state.copy(exercises = updated)
        }
    }

    fun addExercise(exercise: ExerciseDto) {
        _uiState.update { state ->
            val updated = state.exercises.toMutableList()
            updated.add(BuilderExercise(exercise = exercise))
            state.copy(exercises = updated, showExercisePicker = false)
        }
    }

    fun setShowExercisePicker(show: Boolean) {
        _uiState.update { it.copy(showExercisePicker = show) }
    }

    fun saveSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val state = _uiState.value
            val request = CreateSessionRequest(
                name = state.sessionName,
                date = state.date
            )
            trainingRepository.createSession(request).fold(
                onSuccess = { session ->
                    timerJob?.cancel()
                    _uiState.update { it.copy(isSaving = false, savedSessionId = session.id, isRunning = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(isSaving = false, error = e.message) }
                }
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
