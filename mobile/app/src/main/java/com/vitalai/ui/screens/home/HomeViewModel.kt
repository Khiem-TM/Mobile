package com.vitalai.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.remote.model.DashboardDto
import com.vitalai.data.remote.model.DashboardMonthlyDto
import com.vitalai.data.remote.model.DashboardWeeklyDto
import com.vitalai.data.remote.model.MealLogDto
import com.vitalai.data.remote.model.StreakDto
import com.vitalai.data.remote.model.UserDto
import com.vitalai.data.repository.DashboardRepository
import com.vitalai.data.repository.MealLogRepository
import com.vitalai.data.repository.UserRepository
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
    val weeklyDashboard: DashboardWeeklyDto? = null,
    val monthlyDashboard: DashboardMonthlyDto? = null,
    val streaks: StreakDto? = null,
    val mealLogs: List<MealLogDto> = emptyList(),
    val unreadCount: Int = 0,
    val user: UserDto? = null,
    val selectedDate: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    private val mealLogRepository: MealLogRepository,
    private val userRepository: UserRepository
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
            val userDeferred = async { userRepository.getCurrentUser() }
            val selectedLocalDate = LocalDate.parse(date)
            val weekStart = selectedLocalDate.minusDays((selectedLocalDate.dayOfWeek.value - 1).toLong())
            val weeklyDeferred = async { dashboardRepository.getWeeklyDashboard(weekStart.toString()) }
            val monthlyDeferred = async { dashboardRepository.getMonthlyDashboard(selectedLocalDate.year, selectedLocalDate.monthValue) }

            val dashboard = dashboardDeferred.await().getOrNull()
            val streaks = streaksDeferred.await().getOrNull()
            val mealLogs = mealLogsDeferred.await().getOrElse { emptyList() }
            val unread = unreadDeferred.await().getOrElse { 0 }
            val user = userDeferred.await().getOrNull()
            val weekly = weeklyDeferred.await().getOrNull()
            val monthly = monthlyDeferred.await().getOrNull()

            _uiState.update {
                it.copy(
                    dashboard = dashboard,
                    weeklyDashboard = weekly,
                    monthlyDashboard = monthly,
                    streaks = streaks,
                    mealLogs = mealLogs,
                    unreadCount = unread,
                    user = user,
                    isLoading = false
                )
            }
        }
    }

    fun selectDate(date: String) {
        _uiState.update { it.copy(selectedDate = date) }
        loadData()
    }

    fun addWater(ml: Int = 250) {
        viewModelScope.launch {
            dashboardRepository.addWater(ml).onSuccess { updatedDashboard ->
                _uiState.update { it.copy(dashboard = updatedDashboard) }
            }
        }
    }

    fun addSteps(steps: Int = 500) {
        viewModelScope.launch {
            dashboardRepository.addSteps(steps).onSuccess { updatedDashboard ->
                _uiState.update { it.copy(dashboard = updatedDashboard) }
            }
        }
    }

    fun deleteMealItem(mealLogId: String, itemId: String) {
        viewModelScope.launch {
            mealLogRepository.deleteItem(mealLogId, itemId).onSuccess {
                loadData()
            }
        }
    }
}
