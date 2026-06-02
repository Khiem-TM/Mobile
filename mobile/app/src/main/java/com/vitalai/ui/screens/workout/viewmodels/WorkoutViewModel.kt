package com.vitalai.ui.screens.workout.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.remote.model.ActivityLogDto
import com.vitalai.data.remote.model.SessionExerciseDto
import com.vitalai.data.remote.model.WorkoutSessionDto
import com.vitalai.data.repository.TrainingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class DayCalorieData(
    val dayLabel: String,
    val calories: Float,
    val hasSession: Boolean
)

data class WorkoutUiState(
    val workoutStreak: Int = 0,
    val longestStreak: Int = 0,
    val todayCaloriesBurned: Float = 0f,
    val todayDurationMinutes: Int = 0,
    val hasSessionToday: Boolean = false,
    val weeklyChartData: List<DayCalorieData> = emptyList(),
    val selectedWeekStart: LocalDate = LocalDate.now().minusDays((LocalDate.now().dayOfWeek.value - 1).toLong()),
    val recentSessions: List<WorkoutSessionDto> = emptyList(),
    val recentExerciseItems: List<SessionExerciseDto> = emptyList(),
    val todayActivityLog: ActivityLogDto? = null,
    val weeklyActivityLogs: List<ActivityLogDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val trainingRepository: TrainingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState = _uiState.asStateFlow()

    companion object {
        private val ISO_FMT = DateTimeFormatter.ISO_LOCAL_DATE
        private val VN_DAY_LABELS = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
    }

    init {
        loadData()
    }

    fun loadData() {
        val today = LocalDate.now()
        val todayStr = today.format(ISO_FMT)
        val weekStart = _uiState.value.selectedWeekStart
        val weekRange = weekRange(weekStart)

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val sessionsDeferred = async {
                    trainingRepository.getSessions(fromDate = weekRange.first, toDate = weekRange.second)
                }
                val todaySessionsDeferred = async {
                    trainingRepository.getSessions(date = todayStr)
                }
                val recentSessionsDeferred = async {
                    trainingRepository.getSessions(limit = 60)
                }
                val activityLogDeferred = async {
                    trainingRepository.getActivityLog(todayStr)
                }
                val weeklyLogsDeferred = async {
                    trainingRepository.getActivityLogRange(weekRange.first, weekRange.second)
                }

                val sessions = sessionsDeferred.await().getOrElse { emptyList() }
                val todaySessions = todaySessionsDeferred.await().getOrElse { emptyList() }
                val historySessions = recentSessionsDeferred.await().getOrElse { emptyList() }
                val todayActivityLog = activityLogDeferred.await().getOrNull()
                val weeklyLogs = weeklyLogsDeferred.await().getOrElse { emptyList() }

                // Build current-week chart data in a stable Mon -> Sun order.
                val sessionsByDate = sessions.groupBy { it.sessionDate }

                val weeklyChartData = buildWeekChartData(weekStart, sessionsByDate)

                // Today's stats
                val todayCaloriesBurned = todaySessions.sumOf { it.totalCaloriesBurned.toDouble() }.toFloat()
                val todayDurationMinutes = todaySessions.sumOf { it.totalDurationMinutes }
                val hasSessionToday = todaySessions.isNotEmpty()

                // Recent exercise items — last 3 from most recent session
                val mostRecentSession = historySessions.maxByOrNull { it.sessionDate }
                val recentExerciseItems = mostRecentSession?.details?.takeLast(3) ?: emptyList()
                val recentSessions = historySessions
                    .sortedByDescending { it.sessionDate }
                    .take(4)

                // Compute workout streak (consecutive days with sessions going back from today)
                val sessionDates = historySessions.map { it.sessionDate }.toSet()
                val streak = computeStreak(today, sessionDates)
                val longestStreak = computeLongestStreak(
                    historySessions.map { it.sessionDate }.distinct().sorted(),
                    sessionDates
                )

                _uiState.update {
                    it.copy(
                        workoutStreak = streak,
                        longestStreak = longestStreak,
                        todayCaloriesBurned = todayCaloriesBurned,
                        todayDurationMinutes = todayDurationMinutes,
                        hasSessionToday = hasSessionToday,
                        weeklyChartData = weeklyChartData,
                        recentSessions = recentSessions,
                        recentExerciseItems = recentExerciseItems,
                        todayActivityLog = todayActivityLog,
                        weeklyActivityLogs = weeklyLogs,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Lỗi tải dữ liệu") }
            }
        }
    }

    fun addWater(deltaMl: Int) {
        val current = _uiState.value.todayActivityLog?.waterMl ?: 0
        val newVal = (current + deltaMl).coerceAtLeast(0)
        viewModelScope.launch {
            trainingRepository.updateWater(newVal).onSuccess { log ->
                _uiState.update { it.copy(todayActivityLog = log) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun updateSteps(steps: Int) {
        viewModelScope.launch {
            trainingRepository.updateSteps(steps).onSuccess { log ->
                _uiState.update { it.copy(todayActivityLog = log) }
            }.onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun showPreviousWeek() {
        val weekStart = _uiState.value.selectedWeekStart.minusWeeks(1)
        loadWeekChart(weekStart)
    }

    fun showNextWeek() {
        val currentWeekStart = LocalDate.now().minusDays((LocalDate.now().dayOfWeek.value - 1).toLong())
        val weekStart = _uiState.value.selectedWeekStart.plusWeeks(1).coerceAtMost(currentWeekStart)
        if (weekStart != _uiState.value.selectedWeekStart) {
            loadWeekChart(weekStart)
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun loadWeekChart(weekStart: LocalDate) {
        val range = weekRange(weekStart)
        viewModelScope.launch {
            trainingRepository.getSessions(fromDate = range.first, toDate = range.second).fold(
                onSuccess = { sessions ->
                    val chartData = buildWeekChartData(weekStart, sessions.groupBy { it.sessionDate })
                    _uiState.update {
                        it.copy(
                            selectedWeekStart = weekStart,
                            weeklyChartData = chartData,
                            error = null
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message ?: "Lỗi tải dữ liệu tuần") }
                }
            )
        }
    }

    private fun weekRange(weekStart: LocalDate): Pair<String, String> {
        return weekStart.format(ISO_FMT) to weekStart.plusDays(6).format(ISO_FMT)
    }

    private fun buildWeekChartData(
        weekStart: LocalDate,
        sessionsByDate: Map<String, List<WorkoutSessionDto>>
    ): List<DayCalorieData> {
        return (0..6).map { offset ->
            val day = weekStart.plusDays(offset.toLong())
            val dayStr = day.format(ISO_FMT)
            val daySessions = sessionsByDate[dayStr] ?: emptyList()
            val totalCals = daySessions.sumOf { it.totalCaloriesBurned.toDouble() }.toFloat()
            DayCalorieData(
                dayLabel = VN_DAY_LABELS[day.dayOfWeek.value - 1],
                calories = totalCals,
                hasSession = daySessions.isNotEmpty()
            )
        }
    }

    private fun computeStreak(today: LocalDate, sessionDates: Set<String>): Int {
        var streak = 0
        var day = today
        while (sessionDates.contains(day.format(ISO_FMT))) {
            streak++
            day = day.minusDays(1)
        }
        return streak
    }

    private fun computeLongestStreak(dates: List<String>, sessionDates: Set<String>): Int {
        var longest = 0
        var current = 0
        for (d in dates) {
            if (sessionDates.contains(d)) {
                current++
                if (current > longest) longest = current
            } else {
                current = 0
            }
        }
        return longest
    }
}
