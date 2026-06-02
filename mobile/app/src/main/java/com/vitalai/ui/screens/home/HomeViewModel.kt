package com.vitalai.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.remote.model.DashboardDto
import com.vitalai.data.remote.model.DashboardMonthlyDto
import com.vitalai.data.remote.model.DashboardWeeklyDto
import com.vitalai.data.remote.model.ActivityLogDto
import com.vitalai.data.remote.model.BodyMetricDto
import com.vitalai.data.remote.model.MealLogDto
import com.vitalai.data.remote.model.StreakDto
import com.vitalai.data.remote.model.UserDto
import com.vitalai.data.repository.BodyMetricsRepository
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
    val weightTrend: List<BodyMetricDto> = emptyList(),
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
    private val bodyMetricsRepository: BodyMetricsRepository,
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
            val weightTrendDeferred = async { bodyMetricsRepository.getPeriod("3months") }

            val dashboardResult = dashboardDeferred.await()
            val dashboard = dashboardResult.getOrNull()
            val streaks = streaksDeferred.await().getOrNull()
            val mealLogs = mealLogsDeferred.await().getOrElse { emptyList() }
            val unread = unreadDeferred.await().getOrElse { 0 }
            val user = userDeferred.await().getOrNull()
            val weekly = weeklyDeferred.await().getOrNull()
            val monthly = monthlyDeferred.await().getOrNull()
            val activityRange = activityRangeDeferred.await().getOrElse { emptyList() }
            val weightTrend = weightTrendDeferred.await().getOrNull()?.data.orEmpty()

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
                    weightTrend = weightTrend,
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

    /**
     * Adds [deltaMl] to today's water. Optimistic: cập nhật UI NGAY rồi đẩy lên
     * hàng đợi đồng bộ nền (SyncWorker tự gửi + retry). Không await network, không refetch.
     */
    fun addWater(deltaMl: Int = 250) {
        val current = _uiState.value.dashboard?.waterMl ?: 0
        val newVal = (current + deltaMl).coerceAtLeast(0)
        val date = _uiState.value.selectedDate
        _uiState.update { st -> st.copy(dashboard = st.dashboard?.copy(waterMl = newVal)) }
        viewModelScope.launch { trainingRepository.enqueueWater(newVal, date) }
    }

    /** Sets today's total step count to [steps]. Optimistic + đồng bộ nền. */
    fun setSteps(steps: Int) {
        val newVal = steps.coerceAtLeast(0)
        val date = _uiState.value.selectedDate
        _uiState.update { st -> st.copy(dashboard = st.dashboard?.copy(steps = newVal)) }
        viewModelScope.launch { trainingRepository.enqueueSteps(newVal, date) }
    }

    /** Xóa món optimistic: bỏ khỏi danh sách trên UI ngay + xóa Room/đồng bộ nền (no refetch). */
    fun deleteMealItem(mealLogId: String, itemId: String) {
        _uiState.update { st ->
            st.copy(
                mealLogs = st.mealLogs.map { ml ->
                    if (ml.id == mealLogId) ml.copy(items = ml.items.filterNot { it.id == itemId }) else ml
                }
            )
        }
        viewModelScope.launch { mealLogRepository.deleteItem(mealLogId, itemId) }
    }
}
