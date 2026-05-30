package com.vitalai.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.vitalai.data.remote.model.WorkoutSessionDto
import com.vitalai.navigation.Screen
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WorkoutScreen(
    navController: NavController,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Reload session history whenever the screen resumes (e.g. after saving a
    // workout in the builder and navigating back).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.loadData()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    WorkoutDashboard(
        uiState = uiState,
        onNavigateWorkoutBuilder = { navController.navigate(Screen.WorkoutBuilder) },
        onNavigateLibrary = { navController.navigate(Screen.ExerciseLibrary) },
        onNavigateMetrics = { navController.navigate(Screen.Metrics) },
        onSessionClick = { session ->
            navController.navigate(Screen.WorkoutSession(session.id, session.sessionDate))
        },
        onRetry = viewModel::loadData
    )
}

@Composable
private fun WorkoutDashboard(
    uiState: WorkoutUiState,
    onNavigateWorkoutBuilder: () -> Unit,
    onNavigateLibrary: () -> Unit,
    onNavigateMetrics: () -> Unit,
    onSessionClick: (WorkoutSessionDto) -> Unit,
    onRetry: () -> Unit
) {
    TrainScreen {
        when {
            uiState.isLoading -> LoadingState(modifier = Modifier.fillMaxSize())
            uiState.error != null -> ErrorState(
                message = uiState.error,
                onRetry = onRetry,
                modifier = Modifier.fillMaxSize()
            )
            else -> Column(Modifier.fillMaxSize()) {
                TrainHeader(
                    title = "Luyện tập",
                    large = true,
                    right = {
                        TrainRoundIconButton(
                            icon = Icons.Default.AccessibilityNew,
                            contentDescription = "Chỉ số cơ thể",
                            onClick = onNavigateMetrics
                        )
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
                        WeeklyCaloriesCard(uiState.weeklyChartData)
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
                                icon = Icons.Default.MonitorWeight,
                                label = "Chỉ số cơ thể",
                                tone = TrainTone.Slate,
                                onClick = onNavigateMetrics,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        RecentHistory(
                            sessions = uiState.recentSessions,
                            onSessionClick = onSessionClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StreakBanner(streak: Int, longestStreak: Int) {
    TrainCard(tone = TrainTone.Mint, padding = PaddingValues(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(15.dp)) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(TrainColors.Forest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = TrainColors.MintGlaze,
                    modifier = Modifier.size(27.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "Chuỗi $streak ngày liên tiếp",
                    color = TrainColors.Forest,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 22.sp
                )
                Text(
                    "Kỷ lục của bạn: $longestStreak ngày",
                    color = TrainColors.Forest.copy(alpha = 0.75f),
                    fontSize = 12.5.sp
                )
            }
        }
    }
}

@Composable
private fun TodaySummary(calories: Int, minutes: Int) {
    val today = LocalDate.now()
    val title = "Hôm nay · ${today.format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", Locale("vi")))}"
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("vi")) else it.toString() }
    Column {
        TrainSectionTitle(title, modifier = Modifier.padding(bottom = 11.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TrainStatTile(
                icon = Icons.Default.LocalFireDepartment,
                value = calories.toString(),
                label = "Kcal đã đốt",
                tone = TrainTone.Keylime,
                modifier = Modifier.weight(1f)
            )
            TrainStatTile(
                icon = Icons.Default.AccessTime,
                value = minutes.toString(),
                label = "Phút tập luyện",
                tone = TrainTone.Kiss,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun WeeklyCaloriesCard(data: List<DayCalorieData>) {
    val total = data.sumOf { it.calories.toDouble() }.toInt()
    val activeDays = data.count { it.hasSession }
    val maxIndex = data.indices.maxByOrNull { data[it].calories } ?: -1
    TrainCard(tone = TrainTone.Cream, padding = PaddingValues(18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text("Calories đốt · 7 ngày", color = TrainColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text("$total kcal", color = TrainColors.Charcoal, fontSize = 12.sp)
        }
        TrainBarChart(
            values = data.map { it.dayLabel to it.calories },
            highlightIndex = maxIndex,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "$activeDays/7 ngày có buổi tập trong tuần này",
            color = TrainColors.Charcoal.copy(alpha = 0.7f),
            fontSize = 11.5.sp,
            modifier = Modifier.padding(top = 14.dp)
        )
    }
}

@Composable
private fun QuickLinkCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tone: TrainTone,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TrainCard(
        modifier = modifier.height(92.dp),
        tone = tone,
        padding = PaddingValues(16.dp),
        onClick = onClick
    ) {
        Icon(icon, contentDescription = null, tint = TrainColors.Forest, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(10.dp))
        Text(label, color = TrainColors.Forest, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RecentHistory(sessions: List<WorkoutSessionDto>, onSessionClick: (WorkoutSessionDto) -> Unit) {
    Column {
        TrainSectionTitle("Lịch sử gần đây", modifier = Modifier.padding(bottom = 11.dp))
        if (sessions.isEmpty()) {
            TrainCard(tone = TrainTone.Keylime, padding = PaddingValues(22.dp)) {
                Text(
                    "Chưa có buổi tập gần đây",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = TrainColors.Forest,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                sessions.forEach { session ->
                    RecentSessionRow(session = session, onClick = { onSessionClick(session) })
                }
            }
        }
    }
}

@Composable
private fun RecentSessionRow(session: WorkoutSessionDto, onClick: () -> Unit) {
    val date = runCatching { LocalDate.parse(session.sessionDate) }.getOrNull()
    val day = date?.dayOfMonth?.toString()?.padStart(2, '0') ?: "--"
    val month = date?.monthValue?.toString()?.padStart(2, '0') ?: "--"
    TrainCard(tone = TrainTone.Cream, padding = PaddingValues(15.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TrainColors.KeylimeWash),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(day, color = TrainColors.Forest, fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 15.sp)
                    Text(month, color = TrainColors.Forest.copy(alpha = 0.7f), fontSize = 9.sp)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    session.name ?: "Buổi tập",
                    color = TrainColors.Ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${session.details.size} bài tập · ${session.totalDurationMinutes} phút",
                    color = TrainColors.Charcoal,
                    fontSize = 12.5.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    session.totalCaloriesBurned.toInt().toString(),
                    color = TrainColors.Forest,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("kcal", color = TrainColors.Charcoal, fontSize = 10.5.sp)
            }
        }
    }
}

