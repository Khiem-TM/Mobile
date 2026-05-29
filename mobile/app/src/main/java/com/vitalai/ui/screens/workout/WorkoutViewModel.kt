package com.vitalai.ui.screens.workout

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
        val fromDate = today.minusDays(6).format(ISO_FMT)

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val sessionsDeferred = async {
                    trainingRepository.getSessions(fromDate = fromDate, toDate = todayStr)
                }
                val activityLogDeferred = async {
                    trainingRepository.getActivityLog(todayStr)
                }
                val weeklyLogsDeferred = async {
                    trainingRepository.getActivityLogRange(fromDate, todayStr)
                }

                val sessions = sessionsDeferred.await().getOrElse { emptyList() }
                val todayActivityLog = activityLogDeferred.await().getOrNull()
                val weeklyLogs = weeklyLogsDeferred.await().getOrElse { emptyList() }

                // Build 7-day chart data (Mon → Sun labels based on DayOfWeek)
                val last7Days = (6 downTo 0).map { today.minusDays(it.toLong()) }
                val sessionsByDate = sessions.groupBy { it.sessionDate }

                val weeklyChartData = last7Days.map { day ->
                    val dayStr = day.format(ISO_FMT)
                    val daySessions = sessionsByDate[dayStr] ?: emptyList()
                    val totalCals = daySessions.sumOf { it.totalCaloriesBurned.toDouble() }.toFloat()
                    val dayOfWeek = day.dayOfWeek
                    val label = VN_DAY_LABELS[dayOfWeek.value - 1]
                    DayCalorieData(
                        dayLabel = label,
                        calories = totalCals,
                        hasSession = daySessions.isNotEmpty()
                    )
                }

                // Today's stats
                val todaySessions = sessionsByDate[todayStr] ?: emptyList()
                val todayCaloriesBurned = todaySessions.sumOf { it.totalCaloriesBurned.toDouble() }.toFloat()
                val todayDurationMinutes = todaySessions.sumOf { it.totalDurationMinutes }
                val hasSessionToday = todaySessions.isNotEmpty()

                // Recent exercise items — last 3 from most recent session
                val mostRecentSession = sessions.maxByOrNull { it.sessionDate }
                val recentExerciseItems = mostRecentSession?.details?.takeLast(3) ?: emptyList()
                val recentSessions = sessions
                    .sortedByDescending { it.sessionDate }
                    .take(4)

                // Compute workout streak (consecutive days with sessions going back from today)
                val sessionDates = sessions.map { it.sessionDate }.toSet()
                val streak = computeStreak(today, sessionDates)
                val longestStreak = computeLongestStreak(last7Days.map { it.format(ISO_FMT) }, sessionDates)

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

    // ── helpers ──────────────────────────────────────────────────────────────

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
