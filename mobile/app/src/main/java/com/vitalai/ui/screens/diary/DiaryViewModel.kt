package com.vitalai.ui.screens.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.remote.model.MealLogDto
import com.vitalai.data.remote.model.MealLogSummaryDto
import com.vitalai.data.remote.model.UpdateMealLogItemRequest
import com.vitalai.data.repository.MealLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
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

/**
 * Offline-first: UI **observe** Room (source-of-truth). Mọi thao tác ghi cập nhật
 * Room ngay -> Flow tự phát -> UI đổi TỨC THÌ (không await network, không refetch).
 * `refresh()` chỉ kéo dữ liệu mới từ network vào Room (chạy nền).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DiaryViewModel @Inject constructor(
    private val mealLogRepository: MealLogRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    private val mealLogsFlow = _selectedDate.flatMapLatest { date ->
        mealLogRepository.observeMealLogs(date)
    }

    val uiState: StateFlow<DiaryUiState> = combine(
        _selectedDate, mealLogsFlow, _isLoading, _error
    ) { date, logs, loading, error ->
        DiaryUiState(
            mealLogs = logs,
            summary = computeSummary(logs),
            selectedDate = date,
            isLoading = loading,
            error = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiaryUiState())

    init { refresh() }

    private fun refresh() {
        val date = _selectedDate.value
        viewModelScope.launch {
            _isLoading.value = true
            mealLogRepository.refreshMealLogs(date)
            _isLoading.value = false
        }
    }

    fun loadMealLogs() = refresh()

    fun selectDate(date: String) {
        _selectedDate.value = date
        refresh()
    }

    /** Xóa optimistic: repo xóa khỏi Room ngay (Flow tự cập nhật) + đồng bộ nền. */
    fun deleteItem(mealLogId: String, itemId: String) {
        viewModelScope.launch { mealLogRepository.deleteItem(mealLogId, itemId) }
    }

    fun editItem(mealLogId: String, itemId: String, quantity: Float, servingUnit: String) {
        viewModelScope.launch {
            mealLogRepository.updateItem(
                mealLogId,
                itemId,
                UpdateMealLogItemRequest(quantity = quantity, servingUnit = servingUnit)
            )
        }
    }

    fun deleteMealLog(mealLogId: String) {
        viewModelScope.launch {
            mealLogRepository.deleteMealLog(mealLogId).onSuccess { refresh() }
        }
    }

    private fun computeSummary(logs: List<MealLogDto>): MealLogSummaryDto {
        val items = logs.flatMap { it.items }
        return MealLogSummaryDto(
            totalCalories = items.sumOf { it.calories.toDouble() }.toFloat(),
            totalCarbs = items.sumOf { it.carbsG.toDouble() }.toFloat(),
            totalProtein = items.sumOf { it.proteinG.toDouble() }.toFloat(),
            totalFat = items.sumOf { it.fatG.toDouble() }.toFloat()
        )
    }
}
