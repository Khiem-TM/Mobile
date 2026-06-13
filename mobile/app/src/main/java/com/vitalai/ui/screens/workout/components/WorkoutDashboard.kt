package com.vitalai.ui.screens.workout.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.screens.workout.viewmodels.WorkoutUiState

@Composable
fun WorkoutDashboard(
    uiState: WorkoutUiState,
    onNavigateWorkoutBuilder: () -> Unit,
    onNavigateLibrary: () -> Unit,
    onNavigateMetrics: () -> Unit,
    onNavigateWorkoutHistory: () -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onRetry: () -> Unit,
    onBackClick: () -> Unit
) {
    TrainScreen {
        when {
            uiState.isInitialLoading -> LoadingState(modifier = Modifier.fillMaxSize())
            uiState.error != null && !uiState.hasVisibleContent -> ErrorState(
                message = uiState.error,
                onRetry = onRetry,
                modifier = Modifier.fillMaxSize()
            )
            else -> Column(Modifier.fillMaxSize()) {
                com.vitalai.ui.components.VitalScreenHeader(
                    title = "Luyện tập",
                    onBackClick = onBackClick,
                    actions = {
                        TrainRoundIconButton(
                            icon = Icons.Default.AccessibilityNew,
                            contentDescription = "Chỉ số cơ thể",
                            onClick = onNavigateMetrics,
                            size = 32.dp,
                            background = TrainColors.Forest,
                            tint = Color.White,
                            border = null
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(14.dp))
                    }
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item {
                        StreakBanner(
                            streak = uiState.workoutStreak,
                            longestStreak = uiState.longestStreak
                        )
                    }
                    item {
                        TodaySummary(
                            calories = uiState.todayCaloriesBurned.toInt(),
                            minutes = uiState.todayDurationMinutes
                        )
                    }
                    item {
                        WeeklyCaloriesCard(
                            data = uiState.weeklyChartData,
                            weekStart = uiState.selectedWeekStart,
                            onPreviousWeek = onPreviousWeek,
                            onNextWeek = onNextWeek
                        )
                    }
                    item {
                        TrainPrimaryButton(
                            text = if (uiState.hasSessionToday) "Tiếp tục buổi tập hôm nay" else "Bắt đầu buổi tập hôm nay",
                            icon = Icons.Default.Add,
                            onClick = onNavigateWorkoutBuilder
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            QuickLinkCard(
                                icon = Icons.Default.FitnessCenter,
                                label = "Thư viện bài tập",
                                tone = TrainTone.Keylime,
                                onClick = onNavigateLibrary,
                                modifier = Modifier.weight(1f)
                            )
                            QuickLinkCard(
                                icon = Icons.Default.History,
                                label = "Lịch sử tập luyện",
                                tone = TrainTone.Slate,
                                onClick = onNavigateWorkoutHistory,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
