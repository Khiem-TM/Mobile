package com.vitalai.ui.screens.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.remote.model.ExerciseDto
import com.vitalai.data.repository.TrainingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExerciseLibraryUiState(
    val exercises: List<ExerciseDto> = emptyList(),
    val filteredExercises: List<ExerciseDto> = emptyList(),
    val selectedMuscleGroup: String? = null,
    val selectedIntensity: String? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val favorites: Set<String> = emptySet()
)

@HiltViewModel
class ExerciseLibraryViewModel @Inject constructor(
    private val trainingRepository: TrainingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExerciseLibraryUiState())
    val uiState = _uiState.asStateFlow()

    init { loadExercises() }

    fun loadExercises(muscleGroup: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            trainingRepository.getExercises(muscleGroup).fold(
                onSuccess = { list ->
                    _uiState.update { state ->
                        state.copy(exercises = list, isLoading = false).let { s ->
                            s.copy(filteredExercises = applyFilters(s))
                        }
                    }
                },
                onFailure = { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
            )
        }
    }

    fun setMuscleFilter(group: String?) {
        _uiState.update { state ->
            val updated = state.copy(selectedMuscleGroup = group)
            updated.copy(filteredExercises = applyFilters(updated))
        }
        loadExercises(group)
    }

    fun setIntensityFilter(intensity: String?) {
        _uiState.update { state ->
            val updated = state.copy(selectedIntensity = intensity)
            updated.copy(filteredExercises = applyFilters(updated))
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            val updated = state.copy(searchQuery = query)
            updated.copy(filteredExercises = applyFilters(updated))
        }
    }

    fun toggleFavorite(exerciseId: String) {
        _uiState.update { state ->
            val newFavs = if (exerciseId in state.favorites)
                state.favorites - exerciseId
            else
                state.favorites + exerciseId
            state.copy(favorites = newFavs)
        }
    }

    private fun applyFilters(state: ExerciseLibraryUiState): List<ExerciseDto> {
        var list = state.exercises
        if (!state.searchQuery.isBlank()) {
            list = list.filter { it.name.contains(state.searchQuery, ignoreCase = true) }
        }
        if (state.selectedMuscleGroup != null) {
            list = list.filter { it.muscleGroup.equals(state.selectedMuscleGroup, ignoreCase = true) }
        }
        return list
    }
}
