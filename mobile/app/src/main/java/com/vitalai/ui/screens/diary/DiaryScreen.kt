package com.vitalai.ui.screens.diary

import androidx.compose.foundation.background
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    navController: NavController,
    viewModel: DiaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = {
                            val prev = LocalDate.parse(uiState.selectedDate).minusDays(1)
                            viewModel.selectDate(prev.format(DateTimeFormatter.ISO_LOCAL_DATE))
                        }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Ngày trước", tint = Ink700)
                        }
                        Text(
                            text = LocalDate.parse(uiState.selectedDate)
                                .format(DateTimeFormatter.ofPattern("EEE, d MMM", Locale("vi"))),
                            fontWeight = FontWeight.Bold,
                            color = Ink900,
                            fontSize = 16.sp
                        )
                        IconButton(onClick = {
                            val next = LocalDate.parse(uiState.selectedDate).plusDays(1)
                            viewModel.selectDate(next.format(DateTimeFormatter.ISO_LOCAL_DATE))
                        }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Ngày sau", tint = Ink700)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppSurface)
            )
        },
        bottomBar = { VitalBottomNavBar(navController = navController) },
        containerColor = AppBackground
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
                item {
                    val summary = uiState.summary
                    val consumed = summary?.totalCalories ?: 0f
                    val goal = 2000f
                    DiaryDaySummary(consumed = consumed, goal = goal)
                }
                val mealTypes = listOf(
                    "BREAKFAST" to "Bữa sáng",
                    "LUNCH" to "Bữa trưa",
                    "DINNER" to "Bữa tối",
                    "SNACK" to "Snack"
                )
                items(mealTypes) { (type, label) ->
                    val log = uiState.mealLogs.find { it.mealType == type }
                    DiaryMealSection(
                        label = label,
                        mealLog = log,
                        onAddClick = { navController.navigate(Screen.SearchFood(mealType = type, date = uiState.selectedDate)) },
                        onDeleteItem = { mealLogId, itemId -> viewModel.deleteItem(mealLogId, itemId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DiaryDaySummary(consumed: Float, goal: Float) {
    val fraction = if (goal > 0) (consumed / goal).coerceIn(0f, 1f) else 0f
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${consumed.toInt()} kcal đã nạp",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Ink900
                )
                Text(text = "Mục tiêu: ${goal.toInt()} kcal", fontSize = 13.sp, color = Ink500)
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = Mint500,
                    trackColor = Mint100
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
                CircularProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier.size(56.dp),
                    color = Mint500,
                    trackColor = Mint100,
                    strokeWidth = 5.dp
                )
                Text(
                    text = "${(fraction * 100).toInt()}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Mint500
                )
            }
        }
    }
}

@Composable
private fun DiaryMealSection(
    label: String,
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
                Column {
                    Text(text = label, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Ink900)
                    if (mealLog != null) {
                        Text(
                            text = "${mealLog.totalCalories.toInt()} / 500 kcal",
                            fontSize = 12.sp,
                            color = Mint600
                        )
                    }
                }
                IconButton(onClick = onAddClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Thêm",
                        tint = Mint500,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            if (mealLog != null && mealLog.items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                mealLog.items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Mint50)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.foodName,
                                fontSize = 13.sp,
                                color = Ink900,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(text = "${item.quantity} ${item.servingUnit}", fontSize = 11.sp, color = Ink500)
                        }
                        Text(text = "${item.calories.toInt()} kcal", fontSize = 12.sp, color = Ink700)
                        IconButton(
                            onClick = { onDeleteItem(mealLog.id, item.id) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Xóa",
                                tint = MacroProtein.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "+ Thêm món ăn",
                    fontSize = 13.sp,
                    color = Mint500,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }
    }
}

private val Mint600 = Color(0xFF059669)
