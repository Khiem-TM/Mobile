package com.vitalai.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vitalai.navigation.Screen
import com.vitalai.ui.components.ArcGauge
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.theme.AppLine
import com.vitalai.ui.theme.AppSurface
import com.vitalai.ui.theme.AppSurface2
import com.vitalai.ui.theme.AmberContainer
import com.vitalai.ui.theme.AmberOnContainer
import com.vitalai.ui.theme.AmberTint
import com.vitalai.ui.theme.Ink400
import com.vitalai.ui.theme.Ink500
import com.vitalai.ui.theme.Ink700
import com.vitalai.ui.theme.Ink900
import com.vitalai.ui.theme.MacroCarbs
import com.vitalai.ui.theme.MacroFat
import com.vitalai.ui.theme.MacroProtein
import com.vitalai.ui.theme.MealTimeBg
import com.vitalai.ui.theme.MealTimeText
import com.vitalai.ui.theme.Mint100
import com.vitalai.ui.theme.Mint500
import com.vitalai.ui.theme.WaterBlue
import com.vitalai.ui.theme.WaterBlueTint
import com.vitalai.ui.theme.VitalRadius

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.isRefreshing,
        onRefresh = { viewModel.refresh() }
    )

    // Refresh on resume so edits made elsewhere (e.g. goals) reflect on the dashboard.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && uiState.dashboard != null) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullRefresh(pullRefreshState)
    ) {
        when {
            uiState.isLoading && uiState.dashboard == null -> LoadingState(
                modifier = Modifier.fillMaxSize()
            )
            uiState.error != null && uiState.dashboard == null -> ErrorState(
                message = uiState.error!!,
                onRetry = { viewModel.loadData() },
                modifier = Modifier.fillMaxSize()
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
            ) {
                item { HomeHeader(navController, uiState) }
                item { WeekStrip(uiState.selectedDate) { date -> viewModel.selectDate(date) } }
                item { DailyCaloriesCard(uiState) }
                item {
                    WaterAndActivityCards(
                        uiState = uiState,
                        onAddWater = { viewModel.addWater() },
                        onSetSteps = { viewModel.setSteps(it) },
                        onOpenActivityLog = {
                            navController.navigate(Screen.Activity(uiState.selectedDate))
                        }
                    )
                }
                item {
                    ActivityWeekSummaryCard(
                        uiState = uiState,
                        onDateClick = { date -> navController.navigate(Screen.Activity(date)) }
                    )
                }
                item { MealsSection(uiState.mealLogs, navController, uiState.selectedDate) }
                item { DashboardTrendCard(uiState) }
                item {
                    val streak = uiState.streaks?.loginStreak ?: 0
                    if (streak > 0) StreakBanner(streak)
                }
            }
        }
        PullRefreshIndicator(
            refreshing = uiState.isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
fun HomeHeader(navController: NavController, uiState: HomeUiState) {
    val userName = uiState.user?.displayName ?: "Bạn"
    val avatarUrl = uiState.user?.avatarUrl
    val dateLabel = remember(uiState.selectedDate) {
        try {
            val d = java.time.LocalDate.parse(uiState.selectedDate)
            d.format(java.time.format.DateTimeFormatter.ofPattern("EEE, dd MMMM", java.util.Locale("vi")))
        } catch (e: Exception) { uiState.selectedDate }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (avatarUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Mint100),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName.trim().firstOrNull()?.uppercase() ?: "B",
                        color = Mint500,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            } else {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(dateLabel, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Xin chào, $userName 👋", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                    .background(AppSurface)
                    .border(1.dp, AppLine, CircleShape)
                    .clickable { navController.navigate(Screen.Notifications) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Notifications, contentDescription = "Notification", tint = Ink900, modifier = Modifier.size(22.dp))
                if (uiState.unreadCount > 0) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Red).align(Alignment.TopEnd).offset((-10).dp, 10.dp))
                }
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Ink900),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = AppSurface)
            }
        }
    }
}

@Composable
fun WeekStrip(selectedDate: String, onDateSelected: (String) -> Unit) {
    val today = java.time.LocalDate.now()
    val selected = remember(selectedDate) {
        runCatching { java.time.LocalDate.parse(selectedDate) }.getOrDefault(today)
    }
    val days = remember(selected) {
        (-3..3).map { offset -> selected.plusDays(offset.toLong()) }
    }
    val dayNames = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEach { day ->
            val isSelected = day == selected
            val isToday = day == today
            val dayOfWeek = (day.dayOfWeek.value - 1) % 7
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(46.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(if (isSelected) Ink900 else AppSurface)
                    .border(
                        1.dp,
                        if (isSelected) Color.Transparent else if (isToday) Mint500 else AppLine,
                        RoundedCornerShape(30.dp)
                    )
                    .clickable { onDateSelected(day.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)) }
                    .padding(top = 10.dp)
            ) {
                Text(
                    text = dayNames[dayOfWeek],
                    fontSize = 12.sp,
                    color = if (isSelected) Color.White.copy(alpha = 0.7f) else Ink500,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${day.dayOfMonth}",
                    fontSize = 16.sp,
                    color = if (isSelected) AppSurface else Ink900,
                    fontWeight = FontWeight.Bold
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Mint500))
                }
            }
        }
    }
}

@Composable
fun DailyCaloriesCard(uiState: HomeUiState) {
    val d = uiState.dashboard
    val consumed = d?.caloriesConsumed?.toInt() ?: 0
    val goal = d?.calorieGoal ?: 2000
    val remaining = (goal - consumed).coerceAtLeast(0)
    val progress = if (goal > 0) (consumed.toFloat() / goal).coerceIn(0f, 1f) else 0f
    val isOnTrack = consumed <= goal

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        shape = RoundedCornerShape(VitalRadius.Xl),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = BorderStroke(1.dp, AppLine)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text("Calo hôm nay", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink900)
                    Text("Mục tiêu: $goal kcal", fontSize = 13.sp, color = Ink500)
                }
                val statusContainerColor = if (isOnTrack) Mint100 else MaterialTheme.colorScheme.errorContainer
                val statusContentColor = if (isOnTrack) Mint500 else MaterialTheme.colorScheme.error
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100))
                        .background(statusContainerColor)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = statusContentColor, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isOnTrack) "Đúng kế hoạch" else "Vượt mức", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = statusContentColor)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                ArcGauge(
                    value = progress,
                    label = "%,d".format(consumed),
                    sublabel = "kcal · $remaining còn lại",
                    size = 200.dp,
                    stroke = 18.dp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val carbsG = d?.carbsG ?: 0f; val carbsGoal = (d?.carbsGoal ?: 1f).coerceAtLeast(1f)
                val proteinG = d?.proteinG ?: 0f; val proteinGoal = (d?.proteinGoal ?: 1f).coerceAtLeast(1f)
                val fatG = d?.fatG ?: 0f; val fatGoal = (d?.fatGoal ?: 1f).coerceAtLeast(1f)
                MacroBar("Carbs", "${carbsG.toInt()}/${carbsGoal.toInt()}g", carbsG / carbsGoal, MacroCarbs)
                MacroBar("Protein", "${proteinG.toInt()}/${proteinGoal.toInt()}g", proteinG / proteinGoal, MacroProtein)
                MacroBar("Fat", "${fatG.toInt()}/${fatGoal.toInt()}g", fatG / fatGoal, MacroFat)
            }
        }
    }
}

@Composable
fun MacroBar(label: String, value: String, progress: Float, color: Color) {
    Column(modifier = Modifier.width(85.dp)) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink900)
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(100)).background(AppSurface2)
        ) {
            Box(modifier = Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(100)).background(color))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink900)
    }
}

@Composable
fun WaterAndActivityCards(
    uiState: HomeUiState,
    onAddWater: () -> Unit,
    onSetSteps: (Int) -> Unit,
    onOpenActivityLog: () -> Unit
) {
    val d = uiState.dashboard
    val waterMl = d?.waterMl ?: 0
    val waterGoalMl = (d?.waterGoalMl ?: 2500).coerceAtLeast(1)
    val waterCups = waterMl / 250
    val waterProgress = (waterMl.toFloat() / waterGoalMl).coerceIn(0f, 1f)
    val burnedKcal = d?.caloriesBurned?.toInt() ?: 0
    val steps = d?.steps ?: 0
    var showStepsDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Water — tap to add a 250ml cup
        Card(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onAddWater),
            shape = RoundedCornerShape(VitalRadius.Lg),
            colors = CardDefaults.cardColors(containerColor = AppSurface),
            border = BorderStroke(1.dp, AppLine)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(WaterBlueTint), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.LocalDrink, contentDescription = null, tint = WaterBlue, modifier = Modifier.size(20.dp))
                    }
                    Text("${waterMl / 1000f}L / ${waterGoalMl / 1000f}L", fontSize = 11.sp, color = Ink500, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Nước uống", fontSize = 13.sp, color = Ink500)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$waterCups", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink900)
                    Text(" ly", fontSize = 14.sp, color = Ink500, modifier = Modifier.padding(bottom = 3.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val filled = (waterProgress * 10).toInt().coerceIn(0, 10)
                    repeat(filled) { Box(modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(100)).background(WaterBlue)) }
                    repeat(10 - filled) { Box(modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(100)).background(AppSurface2)) }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = WaterBlue, modifier = Modifier.size(14.dp))
                    Text(" Chạm để thêm 250ml", fontSize = 11.sp, color = WaterBlue, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Activity — tap to enter today's step count
        Card(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = { showStepsDialog = true }),
            shape = RoundedCornerShape(VitalRadius.Lg),
            colors = CardDefaults.cardColors(containerColor = AppSurface),
            border = BorderStroke(1.dp, AppLine)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(AmberTint), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.FlashOn, contentDescription = null, tint = MacroCarbs, modifier = Modifier.size(20.dp))
                    }
                    Text("+$burnedKcal kcal", fontSize = 11.sp, color = Ink500, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Bước chân", fontSize = 13.sp, color = Ink500)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("%,d".format(steps), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink900)
                    Text(" bước", fontSize = 14.sp, color = Ink500, modifier = Modifier.padding(bottom = 3.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                val stepsProgress = (steps.toFloat() / 10000f).coerceIn(0f, 1f)
                Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(100)).background(AppSurface2)) {
                    Box(modifier = Modifier.fillMaxWidth(stepsProgress).fillMaxHeight().clip(RoundedCornerShape(100)).background(Mint500))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = MacroCarbs, modifier = Modifier.size(14.dp))
                    Text(" Chạm để cập nhật", fontSize = 11.sp, color = MacroCarbs, fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            "Nhật ký chi tiết",
            color = Mint500,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(VitalRadius.Pill))
                .clickable(onClick = onOpenActivityLog)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }

    if (showStepsDialog) {
        var stepsInput by remember { mutableStateOf(steps.takeIf { it > 0 }?.toString() ?: "") }
        AlertDialog(
            onDismissRequest = { showStepsDialog = false },
            title = { Text("Cập nhật số bước chân", color = Ink900) },
            text = {
                OutlinedTextField(
                    value = stepsInput,
                    onValueChange = { stepsInput = it.filter(Char::isDigit).take(6) },
                    singleLine = true,
                    label = { Text("Tổng số bước hôm nay") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onSetSteps(stepsInput.toIntOrNull() ?: 0)
                    showStepsDialog = false
                }) { Text("Lưu", color = Mint500) }
            },
            dismissButton = {
                TextButton(onClick = { showStepsDialog = false }) { Text("Hủy", color = Ink500) }
            },
            containerColor = AppSurface
        )
    }
}

@Composable
fun ActivityWeekSummaryCard(
    uiState: HomeUiState,
    onDateClick: (String) -> Unit
) {
    val selected = remember(uiState.selectedDate) {
        runCatching { java.time.LocalDate.parse(uiState.selectedDate) }.getOrDefault(java.time.LocalDate.now())
    }
    val days = remember(selected) { (6 downTo 0).map { selected.minusDays(it.toLong()) } }
    val waterGoal = (uiState.dashboard?.waterGoalMl ?: 2500).coerceAtLeast(1)
    val stepGoal = (uiState.dashboard?.stepGoal ?: 10000).coerceAtLeast(1)
    val logsByDate = remember(uiState.recentActivityLogs) {
        uiState.recentActivityLogs.associateBy { it.logDate }
    }
    val labels = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        shape = RoundedCornerShape(VitalRadius.Lg),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = BorderStroke(1.dp, AppLine)
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text("7 ngày gần đây", color = Ink900, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                days.forEach { day ->
                    Text(
                        labels[day.dayOfWeek.value - 1],
                        color = if (day == selected) Ink900 else Ink500,
                        fontSize = 12.sp,
                        fontWeight = if (day == selected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.width(34.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            ActivityGoalRow(
                days = days,
                logsByDate = logsByDate,
                color = WaterBlueTint,
                activeColor = WaterBlue,
                onDateClick = onDateClick
            ) { log -> (log?.waterMl ?: 0) >= waterGoal }
            Spacer(Modifier.height(7.dp))
            ActivityGoalRow(
                days = days,
                logsByDate = logsByDate,
                color = Mint100,
                activeColor = Mint500,
                onDateClick = onDateClick
            ) { log -> (log?.steps ?: 0) >= stepGoal }
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = AppLine)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                LegendDot(WaterBlueTint, "Nước")
                LegendDot(Mint100, "Bước chân")
            }
        }
    }
}

@Composable
private fun ActivityGoalRow(
    days: List<java.time.LocalDate>,
    logsByDate: Map<String, com.vitalai.data.remote.model.ActivityLogDto>,
    color: Color,
    activeColor: Color,
    onDateClick: (String) -> Unit,
    achieved: (com.vitalai.data.remote.model.ActivityLogDto?) -> Boolean
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEach { day ->
            val date = day.toString()
            val ok = achieved(logsByDate[date])
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (ok) color else Mint100.copy(alpha = 0.45f))
                    .clickable { onDateClick(date) },
                contentAlignment = Alignment.Center
            ) {
                Text(if (ok) "✓" else "−", color = if (ok) Ink900 else Ink400, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                if (ok) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp)
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(activeColor)
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.size(13.dp).clip(CircleShape).background(color))
        Text(label, color = Ink700, fontSize = 12.sp)
    }
}

@Composable
fun MealsSection(mealLogs: List<com.vitalai.data.remote.model.MealLogDto>, navController: NavController, selectedDate: String) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Bữa ăn hôm nay", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink900)
            Text(
                "Xem tất cả",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Mint500,
                modifier = Modifier.clickable { navController.navigate(Screen.Diary) }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            val breakfast = mealLogs.find { it.mealType.equals("breakfast", ignoreCase = true) }
            val lunch = mealLogs.find { it.mealType.equals("lunch", ignoreCase = true) }
            val snack = mealLogs.find { it.mealType.equals("snack", ignoreCase = true) }

            MealOverviewCard(
                mealType = "Breakfast",
                imageUrl = breakfast?.items?.firstOrNull()?.imageUrl,
                description = breakfast?.items?.takeIf { it.isNotEmpty() }?.joinToString(" · ") { it.foodName } ?: "Chưa có món ăn",
                time = "Bữa sáng",
                calories = breakfast?.totalCalories?.toInt() ?: 0,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MealOverviewCard(
                    mealType = "Lunch",
                    imageUrl = lunch?.items?.firstOrNull()?.imageUrl,
                    description = lunch?.items?.takeIf { it.isNotEmpty() }?.joinToString(" · ") { it.foodName } ?: "Chưa có món ăn",
                    time = "",
                    calories = lunch?.totalCalories?.toInt() ?: 0,
                    modifier = Modifier.weight(1f),
                    isSmall = true
                )
                MealOverviewCard(
                    mealType = "Snack",
                    imageUrl = snack?.items?.firstOrNull()?.imageUrl,
                    description = snack?.items?.takeIf { it.isNotEmpty() }?.joinToString(" · ") { it.foodName } ?: "Chưa có món ăn",
                    time = "",
                    calories = snack?.totalCalories?.toInt() ?: 0,
                    modifier = Modifier.weight(1f),
                    isSmall = true
                )
            }
        }
    }
}

@Composable
fun MealOverviewCard(
    mealType: String,
    imageUrl: String?,
    description: String,
    time: String,
    calories: Int,
    modifier: Modifier = Modifier,
    isSmall: Boolean = false
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(VitalRadius.Lg),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = BorderStroke(1.dp, AppLine)
    ) {
        Column {
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = mealType,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(if (isSmall) 90.dp else 140.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isSmall) 90.dp else 140.dp)
                        .background(AppSurface2),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🍽️", fontSize = if (isSmall) 26.sp else 34.sp)
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(mealType, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink900)
                if (!isSmall) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(description, fontSize = 12.sp, color = Ink500, maxLines = 1, modifier = Modifier.weight(1f))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.clip(RoundedCornerShape(100)).background(MealTimeBg).padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("⏱️", fontSize = 10.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(time, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MealTimeText)
                            }
                            Row(
                                modifier = Modifier.clip(RoundedCornerShape(100)).background(AmberContainer).padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.LocalDrink, contentDescription = null, tint = AmberOnContainer, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("$calories", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = AmberOnContainer)
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(description, fontSize = 12.sp, color = Ink500)
                }
            }
        }
    }
}

@Composable
fun DashboardTrendCard(uiState: HomeUiState) {
    val weekly = uiState.weeklyDashboard
    val monthly = uiState.monthlyDashboard
    if (weekly == null && monthly == null) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        shape = RoundedCornerShape(VitalRadius.Lg),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = BorderStroke(1.dp, AppLine)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Xu hướng", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink900)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                weekly?.let {
                    TrendMiniMetric(
                        label = "Tuần này",
                        value = "${it.nutrition.avgDailyCalories}",
                        sub = "kcal/ngày",
                        modifier = Modifier.weight(1f)
                    )
                    TrendMiniMetric(
                        label = "Bước TB",
                        value = "${it.activity.avgDailySteps}",
                        sub = "bước/ngày",
                        modifier = Modifier.weight(1f)
                    )
                }
                monthly?.let {
                    TrendMiniMetric(
                        label = "Tháng này",
                        value = "${it.training.workoutCount}",
                        sub = "buổi tập",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendMiniMetric(label: String, value: String, sub: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(VitalRadius.Md))
            .background(AppSurface2)
            .padding(12.dp)
    ) {
        Text(label, fontSize = 11.sp, color = Ink500)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink900)
        Text(sub, fontSize = 11.sp, color = Ink500)
    }
}

@Composable
fun StreakBanner(streak: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .clip(RoundedCornerShape(VitalRadius.Lg))
            .background(Ink900)
            .padding(20.dp)
            .clickable { /* navigate to streaks */ }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🏆", fontSize = 24.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Streak hiện tại", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                    Text("$streak ngày liên tục! 🔥", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Icon(Icons.Default.TrendingUp, contentDescription = "Arrow", tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}
