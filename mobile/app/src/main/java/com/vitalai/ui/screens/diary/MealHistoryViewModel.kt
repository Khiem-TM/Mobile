package com.vitalai.ui.screens.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.remote.model.MealLogDto
import com.vitalai.data.repository.MealLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MealHistoryUiState(
    val fromDate: String = LocalDate.now().minusDays(6).toString(),
    val toDate: String = LocalDate.now().toString(),
    val mealLogs: List<MealLogDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class MealHistoryViewModel @Inject constructor(
    private val mealLogRepository: MealLogRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(MealHistoryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            mealLogRepository.getMealHistory(state.fromDate, state.toDate).fold(
                onSuccess = { logs -> _uiState.update { it.copy(mealLogs = logs, isLoading = false) } },
                onFailure = { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
            )
        }
    }

    fun setRange(fromDate: String, toDate: String) {
        _uiState.update { it.copy(fromDate = fromDate, toDate = toDate) }
        loadHistory()
    }
}
