package com.vitalai.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.remote.model.DashboardDto
import com.vitalai.data.remote.model.DashboardMonthlyDto
import com.vitalai.data.remote.model.DashboardWeeklyDto
import com.vitalai.data.remote.model.ActivityLogDto
import com.vitalai.data.remote.model.MealLogDto
import com.vitalai.data.remote.model.StreakDto
import com.vitalai.data.remote.model.UserDto
import com.vitalai.data.repository.DashboardRepository
import com.vitalai.data.repository.MealLogRepository
import com.vitalai.data.repository.TrainingRepository
import com.vitalai.data.repository.UserRepository
import com.vitalai.core.error.AppErrorMapper
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
    val recentActivityLogs: List<ActivityLogDto> = emptyList(),
    val unreadCount: Int = 0,
    val user: UserDto? = null,
    val selectedDate: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val dashboardRepository: DashboardRepository,
    private val mealLogRepository: MealLogRepository,
    private val trainingRepository: TrainingRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData(isRefresh: Boolean = false) {
        val date = _uiState.value.selectedDate
        viewModelScope.launch {
            _uiState.update {
                if (isRefresh) it.copy(isRefreshing = true, error = null)
                else it.copy(isLoading = true, error = null)
            }
            val dashboardDeferred = async { dashboardRepository.getDashboard(date) }
            val streaksDeferred = async { dashboardRepository.getStreaks() }
            val mealLogsDeferred = async { mealLogRepository.getMealLogs(date) }
            val unreadDeferred = async { dashboardRepository.getUnreadCount() }
            val userDeferred = async { userRepository.getCurrentUser() }
            val selectedLocalDate = LocalDate.parse(date)
            val weekStart = selectedLocalDate.minusDays((selectedLocalDate.dayOfWeek.value - 1).toLong())
            val weeklyDeferred = async { dashboardRepository.getWeeklyDashboard(weekStart.toString()) }
            val monthlyDeferred = async { dashboardRepository.getMonthlyDashboard(selectedLocalDate.year, selectedLocalDate.monthValue) }
            val activityRangeDeferred = async {
                trainingRepository.getActivityLogRange(
                    selectedLocalDate.minusDays(6).toString(),
                    selectedLocalDate.toString()
                )
            }

            val dashboardResult = dashboardDeferred.await()
            val dashboard = dashboardResult.getOrNull()
            val streaks = streaksDeferred.await().getOrNull()
            val mealLogs = mealLogsDeferred.await().getOrElse { emptyList() }
            val unread = unreadDeferred.await().getOrElse { 0 }
            val user = userDeferred.await().getOrNull()
            val weekly = weeklyDeferred.await().getOrNull()
            val monthly = monthlyDeferred.await().getOrNull()
            val activityRange = activityRangeDeferred.await().getOrElse { emptyList() }

            // Only surface a blocking error when the core dashboard load failed and
            // we have nothing to show; otherwise render whatever loaded.
            val errorMessage = if (dashboard == null) {
                dashboardResult.exceptionOrNull()
                    ?.let { AppErrorMapper.fromThrowable(it).userMessage }
                    ?: "Không thể tải dữ liệu. Vui lòng thử lại."
            } else null

            _uiState.update {
                it.copy(
                    dashboard = dashboard ?: it.dashboard,
                    weeklyDashboard = weekly ?: it.weeklyDashboard,
                    monthlyDashboard = monthly ?: it.monthlyDashboard,
                    streaks = streaks ?: it.streaks,
                    mealLogs = mealLogs,
                    recentActivityLogs = activityRange,
                    unreadCount = unread,
                    user = user ?: it.user,
                    isLoading = false,
                    isRefreshing = false,
                    error = errorMessage
                )
            }
        }
    }

    fun refresh() = loadData(isRefresh = true)

    fun selectDate(date: String) {
        _uiState.update { it.copy(selectedDate = date) }
        loadData()
    }

    /** Adds [deltaMl] to today's water (repository call sets an absolute value). */
    fun addWater(deltaMl: Int = 250) {
        viewModelScope.launch {
            val current = _uiState.value.dashboard?.waterMl ?: 0
            val date = _uiState.value.selectedDate
            val result = dashboardRepository.addWater((current + deltaMl).coerceAtLeast(0), date)
            result.onSuccess { updatedDashboard ->
                _uiState.update { it.copy(dashboard = updatedDashboard) }
            }
            if (result.isSuccess) refreshActivityRange()
        }
    }

    /** Sets today's total step count to [steps]. */
    fun setSteps(steps: Int) {
        viewModelScope.launch {
            val date = _uiState.value.selectedDate
            val result = dashboardRepository.addSteps(steps, date)
            result.onSuccess { updatedDashboard ->
                _uiState.update { it.copy(dashboard = updatedDashboard) }
            }
            if (result.isSuccess) refreshActivityRange()
        }
    }

    private suspend fun refreshActivityRange() {
        val selectedLocalDate = LocalDate.parse(_uiState.value.selectedDate)
        trainingRepository.getActivityLogRange(
            selectedLocalDate.minusDays(6).toString(),
            selectedLocalDate.toString()
        ).onSuccess { logs ->
            _uiState.update { it.copy(recentActivityLogs = logs) }
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
