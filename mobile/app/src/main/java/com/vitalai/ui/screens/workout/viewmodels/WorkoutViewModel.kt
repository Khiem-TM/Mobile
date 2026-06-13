package com.vitalai.ui.screens.workout.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vitalai.data.remote.model.ActivityLogDto
import com.vitalai.data.remote.model.SessionExerciseDto
import com.vitalai.data.remote.model.WorkoutSessionDto
import com.vitalai.data.repository.TrainingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

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
    val isInitialLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null
) {
    val hasVisibleContent: Boolean
        get() = recentSessions.isNotEmpty() ||
            weeklyChartData.any { it.hasSession || it.calories > 0f } ||
            todayActivityLog != null ||
            weeklyActivityLogs.isNotEmpty() ||
            hasSessionToday ||
            todayCaloriesBurned > 0f ||
            todayDurationMinutes > 0
}

@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val trainingRepository: TrainingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkoutUiState())
    val uiState = _uiState.asStateFlow()

    private var weekSessionsJob: Job? = null
    private var weekActivityJob: Job? = null
    private var refreshJob: Job? = null
    private var lastRefreshAtMs: Long = 0L

    companion object {
        private const val REFRESH_TTL_MS = 5 * 60 * 1000L
        private val ISO_FMT = DateTimeFormatter.ISO_LOCAL_DATE
        private val VN_DAY_LABELS = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
    }

    init {
        observeData()
        observeWeek(_uiState.value.selectedWeekStart)
        refresh(force = true)
    }

    private fun observeData() {
        val today = LocalDate.now()
        val todayStr = today.format(ISO_FMT)

        viewModelScope.launch {
            trainingRepository.observeRecentSessions(60).collect { historySessions ->
                val mostRecentSession = historySessions.maxByOrNull { it.sessionDate }
                val recentExerciseItems = mostRecentSession?.details?.takeLast(3) ?: emptyList()
                val recentSessions = historySessions
                    .sortedByDescending { it.sessionDate }
                    .take(4)

                val sessionDates = historySessions.map { it.sessionDate }.toSet()
                val streak = computeStreak(today, sessionDates)
                val longestStreak = computeLongestStreak(
                    historySessions.map { it.sessionDate }.distinct().sorted(),
                    sessionDates
                )

                _uiState.update {
                    val next = it.copy(
                        workoutStreak = streak,
                        longestStreak = longestStreak,
                        recentSessions = recentSessions,
                        recentExerciseItems = recentExerciseItems,
                        error = null
                    )
                    val initial = if (next.hasVisibleContent || !next.isRefreshing) false else next.isInitialLoading
                    next.copy(
                        isInitialLoading = initial,
                        isLoading = initial
                    )
                }
            }
        }

        viewModelScope.launch {
            trainingRepository.observeSessionsByDate(todayStr).collect { todaySessions ->
                _uiState.update {
                    it.copy(
                        todayCaloriesBurned = todaySessions.sumOf { session -> session.totalCaloriesBurned.toDouble() }.toFloat(),
                        todayDurationMinutes = todaySessions.sumOf { session -> session.totalDurationMinutes },
                        hasSessionToday = todaySessions.isNotEmpty()
                    )
                }
            }
        }

        viewModelScope.launch {
            trainingRepository.observeActivityLog(todayStr).collect { log ->
                _uiState.update { it.copy(todayActivityLog = log) }
            }
        }
    }

    fun loadData() {
        refresh(force = true)
    }

    fun refreshIfStale() {
        refresh(force = false)
    }

    fun refresh(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && lastRefreshAtMs != 0L && now - lastRefreshAtMs < REFRESH_TTL_MS) return
        if (refreshJob?.isActive == true) return

        val todayStr = LocalDate.now().format(ISO_FMT)
        val range = weekRange(_uiState.value.selectedWeekStart)
        refreshJob = viewModelScope.launch {
            _uiState.update {
                val initial = !it.hasVisibleContent
                it.copy(
                    isRefreshing = true,
                    isInitialLoading = initial,
                    isLoading = initial,
                    error = null
                )
            }
            supervisorScope {
                launch { runCatching { trainingRepository.refreshRecentSessions(60) } }
                launch { runCatching { trainingRepository.refreshSessions(range.first, range.second) } }
                launch { runCatching { trainingRepository.refreshActivityLog(todayStr) } }
                launch { runCatching { trainingRepository.refreshActivityLogRange(range.first, range.second) } }
            }
            lastRefreshAtMs = System.currentTimeMillis()
            _uiState.update { it.copy(isRefreshing = false, isInitialLoading = false, isLoading = false) }
        }
    }

    fun addWater(deltaMl: Int) {
        val current = _uiState.value.todayActivityLog?.waterMl ?: 0
        val newVal = (current + deltaMl).coerceAtLeast(0)
        viewModelScope.launch {
            runCatching { trainingRepository.enqueueWater(newVal) }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun updateSteps(steps: Int) {
        viewModelScope.launch {
            runCatching { trainingRepository.enqueueSteps(steps) }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun showPreviousWeek() {
        loadWeekChart(_uiState.value.selectedWeekStart.minusWeeks(1))
    }

    fun showNextWeek() {
        val currentWeekStart = LocalDate.now().minusDays((LocalDate.now().dayOfWeek.value - 1).toLong())
        val weekStart = _uiState.value.selectedWeekStart.plusWeeks(1).coerceAtMost(currentWeekStart)
        if (weekStart != _uiState.value.selectedWeekStart) {
            loadWeekChart(weekStart)
        }
    }

    private fun loadWeekChart(weekStart: LocalDate) {
        val range = weekRange(weekStart)
        _uiState.update { it.copy(selectedWeekStart = weekStart, error = null) }
        observeWeek(weekStart)
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            supervisorScope {
                launch { runCatching { trainingRepository.refreshSessions(range.first, range.second) } }
                launch { runCatching { trainingRepository.refreshActivityLogRange(range.first, range.second) } }
            }
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun observeWeek(weekStart: LocalDate) {
        val range = weekRange(weekStart)
        weekSessionsJob?.cancel()
        weekActivityJob?.cancel()

        weekSessionsJob = viewModelScope.launch {
            trainingRepository.observeSessionsBetween(range.first, range.second).collect { sessions ->
                _uiState.update {
                    it.copy(
                        weeklyChartData = buildWeekChartData(
                            weekStart,
                            sessions.groupBy { session -> session.sessionDate }
                        ),
                        error = null
                    )
                }
            }
        }

        weekActivityJob = viewModelScope.launch {
            trainingRepository.observeActivityLogsBetween(range.first, range.second).collect { logs ->
                _uiState.update { it.copy(weeklyActivityLogs = logs) }
            }
        }
    }

    private fun weekRange(weekStart: LocalDate): Pair<String, String> =
        weekStart.format(ISO_FMT) to weekStart.plusDays(6).format(ISO_FMT)

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
