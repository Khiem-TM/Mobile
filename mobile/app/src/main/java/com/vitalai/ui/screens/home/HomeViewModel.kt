package com.vitalai.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.remote.model.DashboardDto
import com.vitalai.data.remote.model.MealLogDto
import com.vitalai.data.remote.model.StreakDto
import com.vitalai.data.repository.DashboardRepository
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

data class HomeUiState(
    val dashboard: DashboardDto? = null,
    val streaks: StreakDto? = null,
    val mealLogs: List<MealLogDto> = emptyList(),
    val unreadCount: Int = 0,
    val selectedDate: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    private val mealLogRepository: MealLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        val date = _uiState.value.selectedDate
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val dashboardDeferred = async { dashboardRepository.getDashboard(date) }
            val streaksDeferred = async { dashboardRepository.getStreaks() }
            val mealLogsDeferred = async { mealLogRepository.getMealLogs(date) }
            val unreadDeferred = async { dashboardRepository.getUnreadCount() }

            val dashboard = dashboardDeferred.await().getOrNull()
            val streaks = streaksDeferred.await().getOrNull()
            val mealLogs = mealLogsDeferred.await().getOrElse { emptyList() }
            val unread = unreadDeferred.await().getOrElse { 0 }

            _uiState.update {
                it.copy(
                    dashboard = dashboard,
                    streaks = streaks,
                    mealLogs = mealLogs,
                    unreadCount = unread,
                    isLoading = false
                )
            }
        }
    }

    fun selectDate(date: String) {
        _uiState.update { it.copy(selectedDate = date) }
        loadData()
    }

    fun deleteMealItem(mealLogId: String, itemId: String) {
        viewModelScope.launch {
            mealLogRepository.deleteItem(mealLogId, itemId).onSuccess {
                loadData()
            }
        }
    }
}
