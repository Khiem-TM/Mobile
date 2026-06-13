package com.vitalai.ui.screens.workout.viewmodels

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
    val selectedType: String? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val favorites: Set<String> = emptySet()
)

@HiltViewModel
class ExerciseLibraryViewModel @Inject constructor(
    private val trainingRepository: TrainingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExerciseLibraryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeExercises()
        refreshInBackground()
    }

    private fun observeExercises() {
        viewModelScope.launch {
            trainingRepository.observeExercises().collect { allExercises ->
                val combined = listOf(LOTTIE_TEST_EXERCISE) + allExercises
                _uiState.update { state ->
                    val favorites = combined.filter { it.isFavorite }.map { it.id }.toSet()
                    val newState = state.copy(
                        exercises = combined,
                        favorites = favorites,
                        isInitialLoading = false,
                        isLoading = false
                    )
                    newState.copy(filteredExercises = applyLocalFilters(newState))
                }
            }
        }
    }

    private fun refreshInBackground() {
        viewModelScope.launch {
            _uiState.update {
                val initial = it.exercises.isEmpty()
                it.copy(
                    isRefreshing = true,
                    isInitialLoading = initial,
                    isLoading = initial
                )
            }
            trainingRepository.refreshExercises(force = false)
            trainingRepository.refreshFavoriteExercises()
            _uiState.update { it.copy(isRefreshing = false, isInitialLoading = false, isLoading = false) }
        }
    }

    fun loadExercises() {
        _uiState.update { state ->
            state.copy(filteredExercises = applyLocalFilters(state))
        }
    }

    fun selectType(type: String?) {
        _uiState.update { state ->
            val newState = state.copy(selectedType = type)
            newState.copy(filteredExercises = applyLocalFilters(newState))
        }
    }

    fun setMuscleFilter(group: String?) {
        _uiState.update { state ->
            val newState = state.copy(selectedMuscleGroup = group)
            newState.copy(filteredExercises = applyLocalFilters(newState))
        }
    }

    fun setIntensityFilter(intensity: String?) {
        _uiState.update { state ->
            val newState = state.copy(selectedIntensity = intensity)
            newState.copy(filteredExercises = applyLocalFilters(newState))
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            val newState = state.copy(searchQuery = query)
            newState.copy(filteredExercises = applyLocalFilters(newState))
        }
    }

    fun toggleFavorite(exerciseId: String) {
        viewModelScope.launch {
            val isCurrentlyFavorite = exerciseId in _uiState.value.favorites
            // Optimistic update
            _uiState.update { state ->
                val newFavorites = if (isCurrentlyFavorite) state.favorites - exerciseId
                                   else state.favorites + exerciseId
                state.copy(favorites = newFavorites)
            }
            val result = if (isCurrentlyFavorite) trainingRepository.removeFavorite(exerciseId)
                         else trainingRepository.addFavorite(exerciseId)
            result.onFailure { e ->
                // Revert on failure
                _uiState.update { state ->
                    val reverted = if (isCurrentlyFavorite) state.favorites + exerciseId
                                   else state.favorites - exerciseId
                    state.copy(favorites = reverted, error = e.message ?: "Lỗi cập nhật yêu thích")
                }
            }
        }
    }

    private fun applyLocalFilters(state: ExerciseLibraryUiState): List<ExerciseDto> {
        return state.exercises
            .filter { ex ->
                (state.selectedType == null || ex.exerciseType.equals(state.selectedType, ignoreCase = true)
                    || ex.category.equals(state.selectedType, ignoreCase = true))
                && (state.selectedMuscleGroup == null
                    || ex.primaryMuscleGroup.equals(state.selectedMuscleGroup, ignoreCase = true)
                    || ex.muscleGroupRaw.equals(state.selectedMuscleGroup, ignoreCase = true))
                && (state.selectedIntensity == null
                    || ex.difficultyLevel.equals(state.selectedIntensity, ignoreCase = true))
                && (state.searchQuery.isBlank()
                    || ex.name.contains(state.searchQuery, ignoreCase = true))
            }
            .sortedByDescending { it.favoritesCount }
    }
}
