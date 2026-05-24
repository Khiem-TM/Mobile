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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.vitalai.data.remote.model.MealLogItemDto
import com.vitalai.navigation.Screen
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.components.MainTabScaffold
import com.vitalai.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DiaryScreen(
    navController: NavController,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var editingItem by remember { mutableStateOf<Triple<String, MealLogItemDto, String>?>(null) }

    MainTabScaffold(navController = navController) { padding ->
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
                        onItemClick = { mealLogId, item -> editingItem = Triple(mealLogId, item, item.servingUnit) },
                        onDeleteItem = viewModel::deleteItem,
                        onDeleteMeal = viewModel::deleteMealLog
                    )
                }
            }
        }
    }

    editingItem?.let { (mealLogId, item, _) ->
        var quantityText by remember(item.id) { mutableStateOf(item.quantity.toString()) }
        var unitText by remember(item.id) { mutableStateOf(item.servingUnit) }
        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = { Text("Chỉnh khẩu phần") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Số lượng") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = unitText,
                        onValueChange = { unitText = it },
                        label = { Text("Đơn vị") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val quantity = quantityText.toFloatOrNull()
                        if (quantity != null && quantity > 0f) {
                            viewModel.editItem(mealLogId, item.id, quantity, unitText.ifBlank { item.servingUnit })
                            editingItem = null
                        }
                    }
                ) { Text("Lưu") }
            },
            dismissButton = {
                TextButton(onClick = { editingItem = null }) { Text("Hủy") }
            }
        )
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
                .background(AppSurface2)
                .clickable { navController.popBackStack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Back", tint = Ink900)
        }
        Text("Nhật ký bữa ăn", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink900)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(AppSurface2)
                .clickable { navController.navigate(Screen.MealHistory) },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = Ink900)
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
            Icon(Icons.Default.ChevronLeft, contentDescription = "Prev", tint = Ink400)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (isToday) {
                Text("Hôm nay", fontSize = 12.sp, color = Ink500)
            }
            Text(dateText, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink900)
        }
        IconButton(onClick = onNextDate) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next", tint = Ink400)
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
        shape = RoundedCornerShape(VitalRadius.Xl),
        colors = CardDefaults.cardColors(containerColor = Mint50)
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
                        color = Mint700
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "kcal đã nạp",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Mint700,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "còn lại %,d / %,d kcal".format(remaining.toInt(), goal.toInt()),
                    fontSize = 13.sp,
                    color = Mint700.copy(alpha = 0.8f)
                )
            }
            
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
                Canvas(modifier = Modifier.size(56.dp)) {
                    drawArc(
                        color = Mint100,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 16f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = Mint500,
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
    onItemClick: (String, MealLogItemDto) -> Unit,
    onDeleteItem: (String, String) -> Unit,
    onDeleteMeal: (String) -> Unit
) {
    val totalCalories = mealLog?.totalCalories?.toInt() ?: 0
    val progress = if (totalCalories > 0) 1f else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(VitalRadius.Lg),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = BorderStroke(1.dp, AppLine)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink900)
                    Text(timeRange, fontSize = 12.sp, color = Ink500)
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (mealLog != null) {
                        IconButton(onClick = { onDeleteMeal(mealLog.id) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Xóa bữa", tint = MacroProtein, modifier = Modifier.size(18.dp))
                        }
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("$totalCalories", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink900)
                        Text(" kcal", fontSize = 12.sp, color = Ink500, modifier = Modifier.padding(bottom = 1.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.width(80.dp).height(4.dp).clip(RoundedCornerShape(100)).background(AppLine)) {
                        Box(modifier = Modifier.fillMaxWidth(progress).fillMaxHeight().clip(RoundedCornerShape(100)).background(Mint500))
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
                        .clickable { onItemClick(mealLog.id, item) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.imageUrl != null) {
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = item.foodName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(VitalRadius.Md))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(VitalRadius.Md))
                                .background(AppSurface2),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🍽️", fontSize = 20.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.foodName, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Ink900)
                        Text("${item.quantity} ${item.servingUnit}", fontSize = 13.sp, color = Ink500)
                    }
                    Text("${item.calories.toInt()} kcal", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink700)
                    IconButton(onClick = { onItemClick(mealLog.id, item) }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Sửa món", tint = Ink500, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { onDeleteItem(mealLog.id, item.id) }, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Xóa món", tint = MacroProtein, modifier = Modifier.size(18.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Add Button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(100))
                    .background(Mint50)
                    .clickable { onAddClick() }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm món", tint = Mint500, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Thêm món", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Mint500)
            }
        }
    }
}
