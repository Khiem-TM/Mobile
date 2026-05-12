package com.vitalai.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
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

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = { VitalBottomNavBar(navController = navController) },
        containerColor = Color(0xFFF9FAFB) // Light gray background
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
                item { WeekStrip() }
                item { DailyCaloriesCard() }
                item { WaterAndActivityCards() }
                item { MealsSection(uiState.mealLogs) }
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
    val userName = uiState.user?.displayName ?: "Davil"
    val avatarUrl = uiState.user?.avatarUrl ?: "https://i.pravatar.cc/150?img=11"
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar
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
                Text("Sun, 10 June", fontSize = 13.sp, color = Color.Gray)
                Text("Hello, $userName 👋", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Notification
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Color(0xFFF3F4F6), CircleShape)
                    .clickable { navController.navigate(Screen.Notifications) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Notifications, contentDescription = "Notification", tint = Color.Black, modifier = Modifier.size(22.dp))
                // Red dot
                if (uiState.unreadCount > 0) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Red).align(Alignment.TopEnd).offset((-10).dp, 10.dp))
                }
            }
            // Add Button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
            }
        }
    }
}

@Composable
fun WeekStrip() {
    val days = listOf(
        Pair("M", "8"),
        Pair("T", "9"),
        Pair("W", "10"), // Selected
        Pair("T", "11"),
        Pair("F", "12"),
        Pair("S", "13"),
        Pair("S", "14")
    )
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEachIndexed { index, (dayLabel, dateStr) ->
            val isSelected = index == 2 // 'W 10' is selected in UI
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(46.dp)
                    .height(68.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(if (isSelected) Color(0xFF1E293B) else Color.White)
                    .border(1.dp, if (isSelected) Color.Transparent else Color(0xFFE5E7EB), RoundedCornerShape(30.dp))
                    .padding(top = 10.dp)
            ) {
                Text(
                    text = dayLabel,
                    fontSize = 13.sp,
                    color = if (isSelected) Color(0xFF9CA3AF) else Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dateStr,
                    fontSize = 16.sp,
                    color = if (isSelected) Color.White else Color.Black,
                    fontWeight = FontWeight.Bold
                )
                if (isSelected) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF38C182)))
                }
            }
        }
    }
}

@Composable
fun DailyCaloriesCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF3F4F6))
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text("Daily Calories", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text("Target: 1,938 kcal", fontSize = 13.sp, color = Color.Gray)
                }
                
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100))
                        .background(Color(0xFFECFDF5))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = Color(0xFF047857), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("On track", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Circular Progress
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(200.dp)) {
                    // Background Arc
                    drawArc(
                        color = Color(0xFFF3F4F6),
                        startAngle = 140f,
                        sweepAngle = 260f,
                        useCenter = false,
                        style = Stroke(width = 45f, cap = StrokeCap.Round)
                    )
                    // Foreground Arc
                    drawArc(
                        color = Color(0xFF38C182),
                        startAngle = 140f,
                        sweepAngle = 180f, // Approx 1330/1938
                        useCenter = false,
                        style = Stroke(width = 45f, cap = StrokeCap.Round)
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset(y = (-10).dp)) {
                    Icon(Icons.Outlined.LocalDrink, contentDescription = null, tint = Color(0xFF38C182), modifier = Modifier.size(28.dp))
                    Text(
                        text = "1,330",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black
                    )
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = Color.DarkGray)) { append("kcal") }
                            append(" · 608 còn lại")
                        },
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Macros
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MacroBar(label = "Carbs", value = "140/240g", progress = 140f/240f, color = Color(0xFFF59E0B))
                MacroBar(label = "Protein", value = "68/110g", progress = 68f/110f, color = Color(0xFFEF4444))
                MacroBar(label = "Fat", value = "42/65g", progress = 42f/65f, color = Color(0xFF8B5CF6))
            }
        }
    }
}

@Composable
fun MacroBar(label: String, value: String, progress: Float, color: Color) {
    Column(modifier = Modifier.width(85.dp)) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(100)).background(Color(0xFFF3F4F6))
        ) {
            Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().clip(RoundedCornerShape(100)).background(color))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
    }
}

@Composable
fun WaterAndActivityCards() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Water
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFF3F4F6))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFEFF6FF)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.LocalDrink, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(20.dp))
                    }
                    Text("1.75L / 2.5L", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Nước uống", fontSize = 13.sp, color = Color.Gray)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("7", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(" ly", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 3.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Dotted progress
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(7) { Box(modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(100)).background(Color(0xFF3B82F6))) }
                    repeat(3) { Box(modifier = Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(100)).background(Color(0xFFF3F4F6))) }
                }
            }
        }

        // Activity
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFF3F4F6))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFFFFBEB)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.FlashOn, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                    }
                    Text("+412 kcal", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Đã đốt", fontSize = 13.sp, color = Color.Gray)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("52", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(" phút", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 3.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Bar progress
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.height(14.dp)) {
                    listOf(4, 6, 12, 14, 6, 14, 4).forEach { h ->
                        Box(modifier = Modifier.weight(1f).height(h.dp).clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp)).background(Color(0xFF34D399)))
                    }
                }
            }
        }
    }
}

@Composable
fun MealsSection(mealLogs: List<com.vitalai.data.remote.model.MealLogDto>) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Bữa ăn hôm nay", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Text("Xem tất cả", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF38C182))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            val breakfast = mealLogs.find { it.mealType.equals("breakfast", ignoreCase = true) }
            val lunch = mealLogs.find { it.mealType.equals("lunch", ignoreCase = true) }
            val snack = mealLogs.find { it.mealType.equals("snack", ignoreCase = true) }

            // Breakfast Card (Full width)
            MealOverviewCard(
                mealType = "Breakfast",
                imageUrl = "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=500&q=80",
                description = breakfast?.items?.joinToString(" · ") { it.foodName } ?: "Chưa có món ăn",
                time = "8:30 AM",
                calories = breakfast?.totalCalories?.toInt() ?: 0,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Lunch & Snack Cards (Half width)
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF3F4F6))
    ) {
        Column {
            AsyncImage(
                model = imageUrl,
                contentDescription = mealType,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(if (isSmall) 90.dp else 140.dp)
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Text(mealType, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                if (!isSmall) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(description, fontSize = 12.sp, color = Color.Gray, maxLines = 1, modifier = Modifier.weight(1f))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Time Badge
                            Row(
                                modifier = Modifier.clip(RoundedCornerShape(100)).background(Color(0xFFFCE7F3)).padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("⏱️", fontSize = 10.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(time, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFFBE185D))
                            }
                            // Calories Badge
                            Row(
                                modifier = Modifier.clip(RoundedCornerShape(100)).background(Color(0xFFFEF3C7)).padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.LocalDrink, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("$calories", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFFB45309))
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(description, fontSize = 12.sp, color = Color.Gray)
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
            .background(Color(0xFF229966)) // Dark Green
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

@Composable
fun MealImageCard(imageUrl: String) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.width(260.dp).height(140.dp)
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = "Meal",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
