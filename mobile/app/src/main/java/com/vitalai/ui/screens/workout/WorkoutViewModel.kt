package com.vitalai.ui.screens.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.remote.model.ActivityLogDto
import com.vitalai.data.remote.model.ExerciseDto
import com.vitalai.data.remote.model.WorkoutSessionDto
import com.vitalai.data.repository.TrainingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class WorkoutUiState(
    val sessions: List<WorkoutSessionDto> = emptyList(),
    val exercises: List<ExerciseDto> = emptyList(),
    val activityLog: ActivityLogDto? = null,
    val selectedMuscleGroup: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val trainingRepository: TrainingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val sessionsDeferred = async { trainingRepository.getSessions(today) }
            val exercisesDeferred = async { trainingRepository.getExercises(null) }
            val activityDeferred = async { trainingRepository.getActivityLog(today) }
            _uiState.update {
                it.copy(
                    sessions = sessionsDeferred.await().getOrElse { emptyList() },
                    exercises = exercisesDeferred.await().getOrElse { emptyList() },
                    activityLog = activityDeferred.await().getOrNull(),
                    isLoading = false
                )
            }
        }
    }

    fun filterByMuscleGroup(group: String?) {
        _uiState.update { it.copy(selectedMuscleGroup = group) }
        viewModelScope.launch {
            trainingRepository.getExercises(group).onSuccess { list ->
                _uiState.update { it.copy(exercises = list) }
            }
        }
    }
}
