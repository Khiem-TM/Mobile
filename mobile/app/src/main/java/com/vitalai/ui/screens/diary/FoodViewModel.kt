package com.vitalai.ui.screens.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.remote.model.AddMealItemRequest
import com.vitalai.data.remote.model.CreateFoodRequest
import com.vitalai.data.remote.model.FoodDto
import com.vitalai.data.repository.FoodRepository
import com.vitalai.data.repository.MealLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class FoodUiState(
    val searchResults: List<FoodDto> = emptyList(),
    val allFoods: List<FoodDto> = emptyList(),
    val customFoods: List<FoodDto> = emptyList(),
    val exploreFoods: List<FoodDto> = emptyList(),
    val favorites: List<FoodDto> = emptyList(),
    val selectedFood: FoodDto? = null,
    val isLoading: Boolean = false,
    val isLoadingAll: Boolean = false,
    val isSearching: Boolean = false,
    val isAdding: Boolean = false,
    val addSuccess: Boolean = false,
    val error: String? = null,
    val query: String = "",
    val createSuccess: Boolean = false,
    val isFavoriteUpdating: Boolean = false
)

@HiltViewModel
class FoodViewModel @Inject constructor(
    private val foodRepository: FoodRepository,
    private val mealLogRepository: MealLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FoodUiState())
    val uiState = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun search(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(400)
            _uiState.update { it.copy(isSearching = true) }
            foodRepository.searchFoods(query).onSuccess { page ->
                _uiState.update { it.copy(searchResults = page.items, isSearching = false) }
            }.onFailure { err ->
                _uiState.update { it.copy(isSearching = false, error = err.message) }
            }
        }
    }

    fun loadAllFoods() {
        if (_uiState.value.allFoods.isNotEmpty() || _uiState.value.isLoadingAll) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAll = true) }
            foodRepository.searchFoods("", page = 1, limit = 30).onSuccess { page ->
                _uiState.update { it.copy(allFoods = page.items, isLoadingAll = false) }
            }.onFailure {
                _uiState.update { it.copy(isLoadingAll = false) }
            }
        }
    }

    fun loadExploreFoods() {
        viewModelScope.launch {
            foodRepository.exploreFoods(limit = 20).onSuccess { page ->
                _uiState.update { it.copy(exploreFoods = page.items) }
            }
        }
    }

    fun loadCustomFoods() {
        viewModelScope.launch {
            foodRepository.getCustomFoods(limit = 50).onSuccess { page ->
                _uiState.update { it.copy(customFoods = page.items) }
            }
        }
    }

    fun loadFavorites() {
        viewModelScope.launch {
            foodRepository.getFavorites().onSuccess { list ->
                _uiState.update { it.copy(favorites = list) }
            }
        }
    }

    fun loadFoodById(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            foodRepository.getFoodById(id).onSuccess { food ->
                _uiState.update { it.copy(selectedFood = food, isLoading = false) }
            }.onFailure { err ->
                foodRepository.getCustomFoodById(id).onSuccess { food ->
                    _uiState.update { it.copy(selectedFood = food, isLoading = false) }
                }.onFailure {
                    _uiState.update { it.copy(isLoading = false, error = err.message) }
                }
            }
        }
    }

    fun addToMealLog(mealType: String, date: String, foodId: String, quantity: Float, servingUnit: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAdding = true, error = null) }
            val logs = mealLogRepository.getMealLogs(date).getOrElse { emptyList() }
            var mealLog = logs.find { it.mealType == mealType }
            if (mealLog == null) {
                val created = mealLogRepository.createMealLog(mealType, date)
                mealLog = created.getOrElse {
                    _uiState.update { it.copy(isAdding = false, error = "Không thể tạo bữa ăn") }
                    return@launch
                }
            }
            mealLogRepository.addItem(mealLog.id, AddMealItemRequest(foodId, quantity, servingUnit))
                .onSuccess {
                    _uiState.update { it.copy(isAdding = false, addSuccess = true) }
                }.onFailure { e ->
                    _uiState.update { it.copy(isAdding = false, error = e.message ?: "Lỗi thêm món ăn") }
                }
        }
    }

    fun selectFood(food: FoodDto) {
        _uiState.update { it.copy(selectedFood = food) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedFood = null) }
    }

    fun createFood(
        request: CreateFoodRequest,
        imageFile: File? = null,
        imageMimeType: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            foodRepository.createFood(request, imageFile, imageMimeType).onSuccess {
                // Refresh "Món của tôi" so the new food shows up immediately.
                loadCustomFoods()
                _uiState.update { it.copy(isLoading = false, createSuccess = true) }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, error = err.message ?: "Lỗi tạo món ăn") }
            }
        }
    }

    fun clearAddSuccess() {
        _uiState.update { it.copy(addSuccess = false) }
    }

    fun toggleFavorite(foodId: String) {
        val isCurrentlyFavorite = _uiState.value.favorites.any { it.id == foodId }
        val selected = _uiState.value.selectedFood
        viewModelScope.launch {
            _uiState.update { state ->
                val updatedFavorites = if (isCurrentlyFavorite) {
                    state.favorites.filterNot { it.id == foodId }
                } else {
                    selected?.let { state.favorites + it } ?: state.favorites
                }
                state.copy(favorites = updatedFavorites, isFavoriteUpdating = true, error = null)
            }

            foodRepository.toggleFavorite(foodId, !isCurrentlyFavorite).onSuccess {
                loadFavorites()
                _uiState.update { it.copy(isFavoriteUpdating = false) }
            }.onFailure { err ->
                _uiState.update { state ->
                    val revertedFavorites = if (isCurrentlyFavorite) {
                        selected?.let { state.favorites + it } ?: state.favorites
                    } else {
                        state.favorites.filterNot { it.id == foodId }
                    }
                    state.copy(
                        favorites = revertedFavorites,
                        isFavoriteUpdating = false,
                        error = err.message ?: "Lỗi cập nhật yêu thích"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
