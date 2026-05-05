package com.vitalai.ui.screens.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vitalai.data.remote.model.MealLogDto
import com.vitalai.navigation.Screen
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.components.VitalBottomNavBar
import com.vitalai.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = { VitalBottomNavBar(navController = navController) },
        containerColor = AppBackground
    ) { padding ->
        when {
            uiState.isLoading && uiState.dashboard == null -> LoadingState(
                modifier = Modifier.padding(padding)
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item { HomeHeader(unreadCount = uiState.unreadCount, navController = navController) }
                item { WeekStrip(selectedDate = uiState.selectedDate, onDateSelected = viewModel::selectDate) }
                item {
                    val dash = uiState.dashboard
                    val consumed = dash?.caloriesConsumed ?: 0f
                    val goal = (dash?.calorieGoal ?: 2000).toFloat()
                    val remaining = (goal - consumed).coerceAtLeast(0f).toInt()
                    CalorieArcSection(consumed = consumed, goal = goal, remaining = remaining)
                }
                item {
                    val dash = uiState.dashboard
                    MacroSection(
                        carbsG = dash?.carbsG ?: 0f, carbsGoal = dash?.carbsGoal ?: 260f,
                        proteinG = dash?.proteinG ?: 0f, proteinGoal = dash?.proteinGoal ?: 130f,
                        fatG = dash?.fatG ?: 0f, fatGoal = dash?.fatGoal ?: 65f
                    )
                }
                item {
                    val waterMl = uiState.dashboard?.waterMl ?: 0
                    val waterGoalMl = uiState.dashboard?.waterGoalMl ?: 2000
                    val cups = (waterMl / 200).coerceIn(0, 10)
                    val goalCups = (waterGoalMl / 200).coerceIn(1, 10)
                    WaterTrackerRow(cups = cups, goalCups = goalCups)
                }
                item {
                    val streak = uiState.streaks?.loginStreak ?: 0
                    if (streak > 0) StreakBanner(streak = streak)
                }
                item {
                    Text(
                        text = "Bữa ăn hôm nay",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Ink900,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                val mealTypes = listOf("BREAKFAST" to "Sáng", "LUNCH" to "Trưa", "DINNER" to "Tối", "SNACK" to "Snack")
                items(mealTypes) { (type, label) ->
                    val log = uiState.mealLogs.find { it.mealType == type }
                    MealCard(
                        mealType = label,
                        mealLog = log,
                        onAddClick = { navController.navigate(Screen.SearchFood) },
                        onDeleteItem = { mealLogId, itemId -> viewModel.deleteMealItem(mealLogId, itemId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(unreadCount: Int, navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = "Xin chào 👋", style = MaterialTheme.typography.labelMedium, color = Ink500)
            Text(
                text = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("vi"))),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Ink900
            )
        }
        Box {
            IconButton(onClick = { navController.navigate(Screen.Notifications) }) {
                Icon(Icons.Default.Notifications, contentDescription = "Thông báo", tint = Ink700)
            }
            if (unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MacroProtein)
                        .align(Alignment.TopEnd)
                        .offset(x = (-2).dp, y = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun WeekStrip(selectedDate: String, onDateSelected: (String) -> Unit) {
    val today = LocalDate.now()
    val monday = today.minusDays((today.dayOfWeek.value - 1).toLong())
    val days = (0..6).map { monday.plusDays(it.toLong()) }
    val viDayNames = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppSurface)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(days.size) { i ->
            val day = days[i]
            val dateStr = day.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val isSelected = dateStr == selectedDate
            val isToday = day == today
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) Mint500 else Color.Transparent)
                    .clickable { onDateSelected(dateStr) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = viDayNames[i],
                    fontSize = 11.sp,
                    color = if (isSelected) Color.White else Ink500,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = day.dayOfMonth.toString(),
                    fontSize = 15.sp,
                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.White else Ink900
                )
                if (isToday && !isSelected) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(Mint500)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalorieArcSection(consumed: Float, goal: Float, remaining: Int) {
    val fraction = if (goal > 0) (consumed / goal).coerceIn(0f, 1f) else 0f
    val animatedFraction by animateFloatAsState(targetValue = fraction, animationSpec = tween(800), label = "arc")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(180.dp)
            ) {
                Canvas(modifier = Modifier.size(180.dp)) {
                    val strokeWidth = 18.dp.toPx()
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    drawArc(
                        color = Ink100,
                        startAngle = -225f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = Mint500,
                        startAngle = -225f,
                        sweepAngle = 270f * animatedFraction,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = remaining.toString(), fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Ink900)
                    Text(text = "kcal còn lại", fontSize = 12.sp, color = Ink500)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CalorieStatItem(label = "Nạp vào", value = consumed.toInt().toString(), color = Mint500)
                CalorieStatItem(label = "Mục tiêu", value = goal.toInt().toString(), color = Ink500)
            }
        }
    }
}

@Composable
private fun CalorieStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
        Text(text = label, fontSize = 12.sp, color = Ink500)
    }
}

@Composable
private fun MacroSection(
    carbsG: Float, carbsGoal: Float,
    proteinG: Float, proteinGoal: Float,
    fatG: Float, fatGoal: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MacroBarItem("Carbs", carbsG, carbsGoal, MacroCarbs)
            MacroBarItem("Protein", proteinG, proteinGoal, MacroProtein)
            MacroBarItem("Chất béo", fatG, fatGoal, MacroFat)
        }
    }
}

@Composable
private fun MacroBarItem(label: String, current: Float, goal: Float, color: Color) {
    val fraction by animateFloatAsState(
        targetValue = if (goal > 0) (current / goal).coerceIn(0f, 1f) else 0f,
        animationSpec = tween(600), label = "macro_$label"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(80.dp)) {
        Text(text = label, fontSize = 12.sp, color = Ink500)
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "${current.toInt()}g/${goal.toInt()}g", fontSize = 11.sp, color = Ink700)
    }
}

@Composable
private fun WaterTrackerRow(cups: Int, goalCups: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "💧", fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Nước uống", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Ink900)
                Text(text = "$cups/$goalCups ly", fontSize = 12.sp, color = Ink500)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(10) { i ->
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (i < cups) MacroWater else Ink200)
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakBanner(streak: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(Mint500, Mint700)))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🏆", fontSize = 28.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = "$streak ngày liên tục! 🔥", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                Text(text = "Tiếp tục phát huy nhé!", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun MealCard(
    mealType: String,
    mealLog: MealLogDto?,
    onAddClick: () -> Unit,
    onDeleteItem: (String, String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = mealType, fontWeight = FontWeight.SemiBold, color = Ink900)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (mealLog != null) {
                        Text(
                            text = "${mealLog.totalCalories.toInt()} kcal",
                            fontSize = 12.sp,
                            color = Mint600,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    IconButton(onClick = onAddClick, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Thêm", tint = Mint500, modifier = Modifier.size(20.dp))
                    }
                }
            }
            if (mealLog == null || mealLog.items.isEmpty()) {
                Text(
                    text = "Chưa có món ăn. Nhấn + để thêm.",
                    fontSize = 12.sp,
                    color = Ink500,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                mealLog.items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item.foodName, fontSize = 13.sp, color = Ink800, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(text = "${item.quantity}${item.servingUnit}", fontSize = 11.sp, color = Ink500)
                        }
                        Text(text = "${item.calories.toInt()} kcal", fontSize = 12.sp, color = Ink700)
                        IconButton(
                            onClick = { onDeleteItem(mealLog.id, item.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = MacroProtein, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

private val Ink800 = Color(0xFF1F2937)
private val Mint600 = Color(0xFF059669)
