package com.vitalai.ui.screens.workout.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vitalai.data.remote.model.SessionExerciseDto
import com.vitalai.data.remote.model.WorkoutSessionDto
import com.vitalai.ui.screens.workout.viewmodels.DayCalorieData
import com.vitalai.ui.screens.workout.viewmodels.WorkoutUiState
import com.vitalai.ui.theme.VitalAITheme
import java.time.LocalDate

@Preview(name = "Train Design Primitives", showBackground = true, widthDp = 360)
@Composable
private fun TrainDesignPrimitivesPreview() {
    TrainPreviewSurface {
        TrainHeader(
            title = "Luyện tập",
            large = true,
            right = {
                TrainRoundIconButton(
                    icon = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    onClick = {}
                )
            }
        )
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TrainCard(tone = TrainTone.Mint, padding = PaddingValues(18.dp)) {
                TrainDisplayTitle("Chuỗi 7 ngày liên tiếp")
                TrainSectionTitle("Kỷ lục của bạn: 14 ngày")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TrainStatTile(
                    icon = Icons.Default.LocalFireDepartment,
                    value = "926",
                    label = "Kcal đã đốt",
                    tone = TrainTone.Keylime,
                    modifier = Modifier.weight(1f)
                )
                TrainStatTile(
                    icon = Icons.Default.FitnessCenter,
                    value = "12",
                    label = "Bài tập",
                    tone = TrainTone.Kiss,
                    modifier = Modifier.weight(1f)
                )
            }
            TrainBarChart(
                values = previewWeekData().map { it.dayLabel to it.calories },
                highlightIndex = 1
            )
            TrainPrimaryButton(
                text = "Tiếp tục buổi tập hôm nay",
                icon = Icons.Default.Add,
                onClick = {}
            )
        }
    }
}

@Preview(name = "Dashboard Cards", showBackground = true, widthDp = 360)
@Composable
private fun WorkoutDashboardCardsPreview() {
    TrainPreviewSurface {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            StreakBanner(streak = 7, longestStreak = 14)
            TodaySummary(calories = 926, minutes = 135)
            WeeklyCaloriesCard(
                data = previewWeekData(),
                weekStart = LocalDate.of(2026, 6, 1),
                onPreviousWeek = {},
                onNextWeek = {}
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickLinkCard(
                    icon = Icons.Default.FitnessCenter,
                    label = "Thư viện bài tập",
                    tone = TrainTone.Keylime,
                    onClick = {},
                    modifier = Modifier.weight(1f)
                )
                QuickLinkCard(
                    icon = Icons.Default.LocalFireDepartment,
                    label = "Lịch sử tập luyện",
                    tone = TrainTone.Slate,
                    onClick = {},
                    modifier = Modifier.weight(1f)
                )
            }
            RecentHistory(sessions = previewSessions(), onSessionClick = {})
        }
    }
}

@Preview(name = "Workout Dashboard Screen", showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun WorkoutDashboardScreenPreview() {
    VitalAITheme {
        WorkoutDashboard(
            uiState = previewWorkoutUiState(),
            onNavigateWorkoutBuilder = {},
            onNavigateLibrary = {},
            onNavigateMetrics = {},
            onNavigateWorkoutHistory = {},
            onPreviousWeek = {},
            onNextWeek = {},
            onSessionClick = {},
            onRetry = {}
        )
    }
}

@Composable
private fun TrainPreviewSurface(content: @Composable () -> Unit) {
    VitalAITheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TrainColors.Cream)
        ) {
            content()
            Spacer(Modifier.height(18.dp))
        }
    }
}

private fun previewWorkoutUiState(): WorkoutUiState {
    return WorkoutUiState(
        workoutStreak = 7,
        longestStreak = 14,
        todayCaloriesBurned = 926f,
        todayDurationMinutes = 135,
        hasSessionToday = true,
        weeklyChartData = previewWeekData(),
        selectedWeekStart = LocalDate.of(2026, 6, 1),
        recentSessions = previewSessions()
    )
}

private fun previewWeekData(): List<DayCalorieData> {
    return listOf(
        DayCalorieData("T2", 0f, false),
        DayCalorieData("T3", 926f, true),
        DayCalorieData("T4", 0f, false),
        DayCalorieData("T5", 0f, false),
        DayCalorieData("T6", 0f, false),
        DayCalorieData("T7", 0f, false),
        DayCalorieData("CN", 0f, false)
    )
}

private fun previewSessions(): List<WorkoutSessionDto> {
    val details = List(12) { index ->
        SessionExerciseDto(
            id = "item-$index",
            exerciseId = "exercise-$index",
            durationMinutes = 10,
            caloriesBurned = 72f,
            exerciseType = "GYM"
        )
    }
    return listOf(
        WorkoutSessionDto(
            id = "session-1",
            sessionName = "Buổi tập",
            sessionDate = "2026-06-02",
            totalDurationMinutes = 135,
            totalCaloriesBurned = 926f,
            details = details
        ),
        WorkoutSessionDto(
            id = "session-2",
            sessionName = "Cardio nhẹ",
            sessionDate = "2026-06-01",
            totalDurationMinutes = 42,
            totalCaloriesBurned = 280f,
            details = details.take(4)
        )
    )
}
