package com.vitalai.ui.screens.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.remote.model.MealLogDto
import com.vitalai.data.remote.model.MealLogSummaryDto
import com.vitalai.data.repository.MealLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class DiaryUiState(
    val mealLogs: List<MealLogDto> = emptyList(),
    val summary: MealLogSummaryDto? = null,
    val selectedDate: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val mealLogRepository: MealLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiaryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadMealLogs()
    }

    fun loadMealLogs() {
        val date = _uiState.value.selectedDate
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val logsDeferred = async { mealLogRepository.getMealLogs(date) }
            val summaryDeferred = async { mealLogRepository.getMealSummary(date) }
            val logs = logsDeferred.await().getOrElse { emptyList() }
            val summary = summaryDeferred.await().getOrNull()
            _uiState.update { it.copy(mealLogs = logs, summary = summary, isLoading = false) }
        }
    }

    fun selectDate(date: String) {
        _uiState.update { it.copy(selectedDate = date) }
        loadMealLogs()
    }

    fun deleteItem(mealLogId: String, itemId: String) {
        viewModelScope.launch {
            mealLogRepository.deleteItem(mealLogId, itemId).onSuccess { loadMealLogs() }
        }
    }
}
