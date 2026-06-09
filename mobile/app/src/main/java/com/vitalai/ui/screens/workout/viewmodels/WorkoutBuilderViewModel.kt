package com.vitalai.ui.screens.workout.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.remote.model.CreateWorkoutSessionDto
import com.vitalai.data.remote.model.ExerciseDto
import com.vitalai.data.remote.model.WorkoutDetailDto
import com.vitalai.data.repository.TrainingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.max
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
    // GYM fields
    val sets: MutableList<SetEntry> = mutableListOf(SetEntry(1)),
    // SPORT fields
    val durationMinutes: Int = 30,
    val intensityLevel: String = "MEDIUM",
    // CARDIO fields
    val distanceKm: Float = 5f,
    val avgSpeedKmh: Float = 10f
)

data class WorkoutBuilderUiState(
    val sessionName: String = "Buổi tập mới",
    val sessionNotes: String = "",
    val date: String = LocalDate.now().toString(),
    val exercises: List<BuilderExercise> = emptyList(),
    val elapsedSeconds: Int = 0,
    val isRunning: Boolean = false,
    val isSaving: Boolean = false,
    val savedSessionId: String? = null,
    val error: String? = null,
    val showExercisePicker: Boolean = false,
    val availableExercises: List<ExerciseDto> = emptyList(),
    val isLoadingExercises: Boolean = false
)

@HiltViewModel
class WorkoutBuilderViewModel @Inject constructor(
    private val trainingRepository: TrainingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutBuilderUiState())
    val uiState = _uiState.asStateFlow()

    private var timerJob: kotlinx.coroutines.Job? = null

    init {
        startTimer()
        observeExercises()
    }

    private fun observeExercises() {
        viewModelScope.launch {
            trainingRepository.observeExercises().collect { exercises ->
                _uiState.update { it.copy(availableExercises = exercises) }
            }
        }
    }

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

    fun setSessionNotes(notes: String) {
        _uiState.update { it.copy(sessionNotes = notes.take(500)) }
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
            updated.add(
                BuilderExercise(
                    exercise = exercise,
                    durationMinutes = exercise.defaultDurationMinutes ?: 30,
                    intensityLevel = exercise.defaultIntensityLevel ?: "MEDIUM",
                    sets = mutableListOf(
                        SetEntry(1, reps = (exercise.defaultReps ?: 10).toString(), kg = (exercise.defaultWeightKg ?: 0f).toString())
                    )
                )
            )
            state.copy(exercises = updated, showExercisePicker = false)
        }
    }

    fun updateSportEntry(idx: Int, durationMinutes: Int? = null, intensityLevel: String? = null) {
        _uiState.update { state ->
            val updated = state.exercises.toMutableList()
            if (idx in updated.indices) {
                val ex = updated[idx]
                updated[idx] = ex.copy(
                    durationMinutes = durationMinutes ?: ex.durationMinutes,
                    intensityLevel = intensityLevel ?: ex.intensityLevel
                )
            }
            state.copy(exercises = updated)
        }
    }

    fun updateCardioEntry(idx: Int, distanceKm: Float? = null, avgSpeedKmh: Float? = null) {
        _uiState.update { state ->
            val updated = state.exercises.toMutableList()
            if (idx in updated.indices) {
                val ex = updated[idx]
                updated[idx] = ex.copy(
                    distanceKm = distanceKm ?: ex.distanceKm,
                    avgSpeedKmh = avgSpeedKmh ?: ex.avgSpeedKmh
                )
            }
            state.copy(exercises = updated)
        }
    }

    fun setShowExercisePicker(show: Boolean) {
        _uiState.update { it.copy(showExercisePicker = show) }
        if (show) {
            viewModelScope.launch { trainingRepository.refreshExercises() }
        }
    }

    fun saveSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val state = _uiState.value
            val details = state.exercises.mapIndexed { index, builderExercise ->
                val ex = builderExercise.exercise
                when (ex.exerciseType) {
                    "GYM" -> {
                        val reps = builderExercise.sets.mapNotNull { it.reps.toIntOrNull() }
                        val weights = builderExercise.sets.mapNotNull { it.kg.toFloatOrNull() }
                        WorkoutDetailDto(
                            exerciseId = ex.id,
                            exerciseType = "GYM",
                            sets = builderExercise.sets.size,
                            repsPerSet = reps.takeIf { it.isNotEmpty() }?.average()?.toInt()?.coerceAtLeast(1),
                            weightKg = weights.takeIf { it.isNotEmpty() }?.average()?.toFloat(),
                            orderIndex = index
                        )
                    }
                    "CARDIO" -> {
                        WorkoutDetailDto(
                            exerciseId = ex.id,
                            exerciseType = "CARDIO",
                            distanceKm = builderExercise.distanceKm,
                            avgSpeedKmh = builderExercise.avgSpeedKmh,
                            sets = null, repsPerSet = null, weightKg = null,
                            orderIndex = index
                        )
                    }
                    else -> { // SPORT
                        WorkoutDetailDto(
                            exerciseId = ex.id,
                            exerciseType = "SPORT",
                            durationMinutes = builderExercise.durationMinutes,
                            intensityLevel = builderExercise.intensityLevel,
                            sets = null, repsPerSet = null, weightKg = null,
                            orderIndex = index
                        )
                    }
                }
            }
            val request = CreateWorkoutSessionDto(
                sessionDate = state.date,
                sessionName = state.sessionName,
                notes = state.sessionNotes.trim().ifBlank { null },
                details = details
            )
            trainingRepository.enqueueSession(request).fold(
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
