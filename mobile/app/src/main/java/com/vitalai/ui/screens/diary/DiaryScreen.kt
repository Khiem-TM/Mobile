package com.vitalai.ui.screens.diary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vitalai.data.remote.model.MealLogDto
import com.vitalai.navigation.Screen
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.components.VitalBottomNavBar
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DiaryScreen(
    navController: NavController,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = { VitalBottomNavBar(navController = navController) },
        containerColor = Color(0xFFF9FAFB)
    ) { padding ->
        when {
            uiState.isLoading -> LoadingState(modifier = Modifier.padding(padding))
            uiState.error != null -> ErrorState(
                message = uiState.error!!,
                onRetry = viewModel::loadMealLogs,
                modifier = Modifier.padding(padding)
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Top Header
                item { DiaryHeader(navController) }
                // Date Switcher
                item { 
                    DateSwitcher(
                        selectedDateStr = uiState.selectedDate,
                        onPrevDate = {
                            val prev = LocalDate.parse(uiState.selectedDate).minusDays(1)
                            viewModel.selectDate(prev.format(DateTimeFormatter.ISO_LOCAL_DATE))
                        },
                        onNextDate = {
                            val next = LocalDate.parse(uiState.selectedDate).plusDays(1)
                            viewModel.selectDate(next.format(DateTimeFormatter.ISO_LOCAL_DATE))
                        }
                    )
                }
                // Summary Card
                item {
                    val summary = uiState.summary
                    val consumed = summary?.totalCalories ?: 0f
                    val goal = 1938f // Or from summary if available
                    val remaining = (goal - consumed).coerceAtLeast(0f)
                    DiarySummaryCard(consumed = consumed, goal = goal, remaining = remaining)
                }
                
                // Meal Sections
                val mealTypes = listOf(
                    Triple("BREAKFAST", "Sáng (Breakfast)", "7:00 - 9:30"),
                    Triple("LUNCH", "Trưa (Lunch)", "11:30 - 14:00"),
                    Triple("DINNER", "Tối (Dinner)", "18:00 - 20:00"),
                    Triple("SNACK", "Bữa phụ (Snack)", "Tuỳ chọn")
                )
                
                items(mealTypes) { (type, label, timeRange) ->
                    val log = uiState.mealLogs.find { it.mealType.equals(type, ignoreCase = true) }
                    DiaryMealSection(
                        label = label,
                        timeRange = timeRange,
                        mealLog = log,
                        onAddClick = { navController.navigate(Screen.SearchFood(mealType = type.lowercase(), date = uiState.selectedDate)) },
                        onItemClick = { /* Optional: Navigate to item details */ }
                    )
                }
            }
        }
    }
}

@Composable
fun DiaryHeader(navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFF3F4F6))
                .clickable { navController.popBackStack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = Color.Black)
        }
        Text("Nhật ký bữa ăn", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFF3F4F6))
                .clickable { /* Handle Search */ },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.Black)
        }
    }
}

@Composable
fun DateSwitcher(selectedDateStr: String, onPrevDate: () -> Unit, onNextDate: () -> Unit) {
    val date = LocalDate.parse(selectedDateStr)
    val today = LocalDate.now()
    val isToday = date.isEqual(today)
    
    val dateText = if (isToday) {
        date.format(DateTimeFormatter.ofPattern("EEE, d MMMM yyyy", Locale("vi")))
    } else {
        date.format(DateTimeFormatter.ofPattern("EEE, d MMMM yyyy", Locale("vi")))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevDate) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Prev", tint = Color.Gray)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isToday) {
                Text("Hôm nay", fontSize = 12.sp, color = Color.Gray)
            }
            Text(dateText, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
        IconButton(onClick = onNextDate) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = Color.Gray)
        }
    }
}

@Composable
fun DiarySummaryCard(consumed: Float, goal: Float, remaining: Float) {
    val progress = (consumed / goal).coerceIn(0f, 1f)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)) // Light greenish background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "%,d".format(consumed.toInt()),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF047857)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "kcal đã nạp",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF047857),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "còn lại %,d / %,d kcal".format(remaining.toInt(), goal.toInt()),
                    fontSize = 13.sp,
                    color = Color(0xFF047857).copy(alpha = 0.8f)
                )
            }
            
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
                Canvas(modifier = Modifier.size(56.dp)) {
                    drawArc(
                        color = Color(0xFFD1FAE5),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 16f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = Color(0xFF10B981),
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = 16f, cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

@Composable
fun DiaryMealSection(
    label: String,
    timeRange: String,
    mealLog: MealLogDto?,
    onAddClick: () -> Unit,
    onItemClick: () -> Unit
) {
    val totalCalories = mealLog?.totalCalories?.toInt() ?: 0
    val targetCalories = 484 // Mock target for demonstration
    val progress = (totalCalories.toFloat() / targetCalories.toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    Text(timeRange, fontSize = 12.sp, color = Color.Gray)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$totalCalories", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Text(" / $targetCalories kcal", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 1.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.width(80.dp).height(4.dp).clip(RoundedCornerShape(100)).background(Color(0xFFE5E7EB))) {
                        Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().clip(RoundedCornerShape(100)).background(Color(0xFF10B981)))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Food Items
            mealLog?.items?.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { onItemClick() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Item image placeholder (using solid color if no image available, but using AsyncImage here)
                    AsyncImage(
                        model = "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=100&q=80", // Using placeholder image for now
                        contentDescription = item.foodName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.foodName, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                        Text("${item.quantity} ${item.servingUnit}", fontSize = 13.sp, color = Color.Gray)
                    }
                    Text("${item.calories.toInt()} kcal", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Add Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(100))
                    .background(Color(0xFFECFDF5))
                    .clickable { onAddClick() }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm món", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Thêm món", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF10B981))
            }
        }
    }
}
