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
        loadExercises()
        loadFavorites()
    }

    fun loadExercises() {
        viewModelScope.launch {
            val state = _uiState.value
            _uiState.update { it.copy(isLoading = true, error = null) }
            trainingRepository.getExercises(
                type = state.selectedType,
                name = state.searchQuery.takeIf { it.isNotBlank() },
                muscleGroup = state.selectedMuscleGroup,
                difficultyLevel = state.selectedIntensity
            ).fold(
                onSuccess = { list ->
                    _uiState.update { s ->
                        s.copy(exercises = list, isLoading = false).let { updated ->
                            updated.copy(filteredExercises = applyLocalFilters(updated))
                        }
                    }
                },
                onFailure = { e -> _uiState.update { it.copy(error = e.message, isLoading = false) } }
            )
        }
    }

    fun selectType(type: String?) {
        _uiState.update { it.copy(selectedType = type) }
        loadExercises()
    }

    fun setMuscleFilter(group: String?) {
        _uiState.update { it.copy(selectedMuscleGroup = group) }
        loadExercises()
    }

    fun setIntensityFilter(intensity: String?) {
        _uiState.update { it.copy(selectedIntensity = intensity) }
        loadExercises()
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadExercises()
    }

    fun toggleFavorite(exerciseId: String) {
        viewModelScope.launch {
            val isCurrentlyFavorite = exerciseId in _uiState.value.favorites
            _uiState.update { state ->
                val newFavorites = if (isCurrentlyFavorite) {
                    state.favorites - exerciseId
                } else {
                    state.favorites + exerciseId
                }
                state.copy(favorites = newFavorites)
            }

            val result = if (isCurrentlyFavorite) {
                trainingRepository.removeFavorite(exerciseId)
            } else {
                trainingRepository.addFavorite(exerciseId)
            }

            result.onFailure { e ->
                _uiState.update { state ->
                    val revertedFavorites = if (isCurrentlyFavorite) {
                        state.favorites + exerciseId
                    } else {
                        state.favorites - exerciseId
                    }
                    state.copy(
                        favorites = revertedFavorites,
                        error = e.message ?: "Lỗi cập nhật yêu thích"
                    )
                }
            }
        }
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            trainingRepository.getFavorites().onSuccess { favorites ->
                _uiState.update { state ->
                    state.copy(favorites = favorites.map { it.id }.toSet())
                }
            }
        }
    }

    // Server handles type/name/muscle/difficulty; keep local sort for stable UI.
    private fun applyLocalFilters(state: ExerciseLibraryUiState): List<ExerciseDto> {
        return state.exercises.sortedByDescending { it.favoritesCount }
    }
}
