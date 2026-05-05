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
import javax.inject.Inject

data class FoodUiState(
    val searchResults: List<FoodDto> = emptyList(),
    val allFoods: List<FoodDto> = emptyList(),
    val favorites: List<FoodDto> = emptyList(),
    val selectedFood: FoodDto? = null,
    val isLoading: Boolean = false,
    val isLoadingAll: Boolean = false,
    val isSearching: Boolean = false,
    val isAdding: Boolean = false,
    val addSuccess: Boolean = false,
    val error: String? = null,
    val query: String = "",
    val createSuccess: Boolean = false
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
                _uiState.update { it.copy(isLoading = false, error = err.message) }
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

    fun createFood(request: CreateFoodRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            foodRepository.createFood(request).onSuccess {
                _uiState.update { it.copy(isLoading = false, createSuccess = true) }
            }.onFailure { err ->
                _uiState.update { it.copy(isLoading = false, error = err.message) }
            }
        }
    }

    fun clearAddSuccess() {
        _uiState.update { it.copy(addSuccess = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
