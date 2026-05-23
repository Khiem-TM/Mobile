package com.vitalai.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vitalai.navigation.Screen
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.components.VitalBottomNavBar
import com.vitalai.ui.theme.AmberContainer
import com.vitalai.ui.theme.AmberOnContainer
import com.vitalai.ui.theme.AmberTint
import com.vitalai.ui.theme.MacroCarbs
import com.vitalai.ui.theme.MacroFat
import com.vitalai.ui.theme.MacroProtein
import com.vitalai.ui.theme.MealTimeBg
import com.vitalai.ui.theme.MealTimeText
import com.vitalai.ui.theme.Slate900
import com.vitalai.ui.theme.WaterBlue
import com.vitalai.ui.theme.WaterBlueTint

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = { VitalBottomNavBar(navController = navController) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            uiState.isLoading && uiState.dashboard == null -> LoadingState(
                modifier = Modifier.padding(padding)
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
            ) {
                item { HomeHeader(navController, uiState) }
                item { WeekStrip(uiState.selectedDate) { date -> viewModel.selectDate(date) } }
                item { DailyCaloriesCard(uiState) }
                item { 
                    WaterAndActivityCards(
                        uiState = uiState,
                        onAddWater = { viewModel.addWater() },
                        onAddSteps = { viewModel.addSteps() }
                    ) 
                }
                item { MealsSection(uiState.mealLogs, navController, uiState.selectedDate) }
                item {
                    val streak = uiState.streaks?.loginStreak ?: 0
                    if (streak > 0) StreakBanner(streak)
                }
            }
        }
    }
}

@Composable
fun HomeHeader(navController: NavController, uiState: HomeUiState) {
    val userName = uiState.user?.displayName ?: "Bạn"
    val avatarUrl = uiState.user?.avatarUrl ?: "https://i.pravatar.cc/150?img=11"
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
            AsyncImage(
                model = avatarUrl,
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )
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
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                    .clickable { navController.navigate(Screen.Notifications) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Notifications, contentDescription = "Notification", tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
                if (uiState.unreadCount > 0) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Red).align(Alignment.TopEnd).offset((-10).dp, 10.dp))
                }
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Slate900),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = MaterialTheme.colorScheme.surface)
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
                    .background(if (isSelected) Slate900 else MaterialTheme.colorScheme.surface)
                    .border(
                        1.dp,
                        if (isSelected) Color.Transparent else if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(30.dp)
                    )
                    .clickable { onDateSelected(day.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)) }
                    .padding(top = 10.dp)
            ) {
                Text(
                    text = dayNames[dayOfWeek],
                    fontSize = 12.sp,
                    color = if (isSelected) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${day.dayOfMonth}",
                    fontSize = 16.sp,
                    color = if (isSelected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text("Calo hôm nay", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Mục tiêu: $goal kcal", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val statusContainerColor = if (isOnTrack) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
                val statusContentColor = if (isOnTrack) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
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
                val arcTrackColor = MaterialTheme.colorScheme.secondaryContainer
                val arcProgressColor = MaterialTheme.colorScheme.primary
                Canvas(modifier = Modifier.size(200.dp)) {
                    drawArc(color = arcTrackColor, startAngle = 140f, sweepAngle = 260f, useCenter = false, style = Stroke(width = 45f, cap = StrokeCap.Round))
                    drawArc(color = arcProgressColor, startAngle = 140f, sweepAngle = 260f * progress, useCenter = false, style = Stroke(width = 45f, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset(y = (-10).dp)) {
                    Icon(Icons.Outlined.FlashOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Text(text = "%,d".format(consumed), fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) { append("kcal") }
                            append(" · $remaining còn lại")
                        },
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(100)).background(MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Box(modifier = Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(100)).background(color))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WaterAndActivityCards(
    uiState: HomeUiState,
    onAddWater: () -> Unit,
    onAddSteps: () -> Unit
) {
    val d = uiState.dashboard
    val waterMl = d?.waterMl ?: 0
    val waterGoalMl = (d?.waterGoalMl ?: 2500).coerceAtLeast(1)
    val waterCups = waterMl / 250
    val waterProgress = (waterMl.toFloat() / waterGoalMl).coerceIn(0f, 1f)
    val burnedKcal = d?.caloriesBurned?.toInt() ?: 0
    val steps = d?.steps ?: 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Water
        Card(
            modifier = Modifier
                .weight(1f)
                .combinedClickable(
                    onClick = { /* Có thể điều hướng đến chi tiết */ },
                    onDoubleClick = onAddWater
                ),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(WaterBlueTint), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.LocalDrink, contentDescription = null, tint = WaterBlue, modifier = Modifier.size(20.dp))
                    }
                    Text("${waterMl / 1000f}L / ${waterGoalMl / 1000f}L", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Nước uống", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("$waterCups", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(" ly", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 3.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val filled = (waterProgress * 10).toInt().coerceIn(0, 10)
                    repeat(filled) { Box(modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(100)).background(WaterBlue)) }
                    repeat(10 - filled) { Box(modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(100)).background(MaterialTheme.colorScheme.secondaryContainer)) }
                }
            }
        }

        // Activity
        Card(
            modifier = Modifier
                .weight(1f)
                .combinedClickable(
                    onClick = { /* Có thể điều hướng đến chi tiết */ },
                    onDoubleClick = onAddSteps
                ),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(AmberTint), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.FlashOn, contentDescription = null, tint = MacroCarbs, modifier = Modifier.size(20.dp))
                    }
                    Text("+$burnedKcal kcal", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Bước chân", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("%,d".format(steps), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(" bước", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 3.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                val stepsProgress = (steps.toFloat() / 10000f).coerceIn(0f, 1f)
                Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(100)).background(MaterialTheme.colorScheme.secondaryContainer)) {
                    Box(modifier = Modifier.fillMaxWidth(stepsProgress).fillMaxHeight().clip(RoundedCornerShape(100)).background(MaterialTheme.colorScheme.primary))
                }
            }
        }
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
            Text("Bữa ăn hôm nay", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text(
                "Xem tất cả",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
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
                imageUrl = "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=500&q=80",
                description = breakfast?.items?.joinToString(" · ") { it.foodName } ?: "Chưa có món ăn",
                time = "8:30 AM",
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
                    imageUrl = "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=500&q=80",
                    description = "12:45 · ${lunch?.totalCalories?.toInt() ?: 0} kcal",
                    time = "",
                    calories = lunch?.totalCalories?.toInt() ?: 0,
                    modifier = Modifier.weight(1f),
                    isSmall = true
                )
                MealOverviewCard(
                    mealType = "Snack",
                    imageUrl = "https://images.unsplash.com/photo-1505253716362-afaea1d3d1af?w=500&q=80",
                    description = "15:20 · ${snack?.totalCalories?.toInt() ?: 0} kcal",
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
    imageUrl: String,
    description: String,
    time: String,
    calories: Int,
    modifier: Modifier = Modifier,
    isSmall: Boolean = false
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column {
            AsyncImage(
                model = imageUrl,
                contentDescription = mealType,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(if (isSmall) 90.dp else 140.dp)
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Text(mealType, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                if (!isSmall) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, modifier = Modifier.weight(1f))
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
                    Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun StreakBanner(streak: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.secondary)
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
