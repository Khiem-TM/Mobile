package com.vitalai.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vitalai.data.remote.model.ExerciseDto
import com.vitalai.data.remote.model.WorkoutSessionDto
import com.vitalai.navigation.Screen
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.theme.*

data class WorkoutCategory(val emoji: String, val name: String, val muscleGroup: String)

val workoutCategories = listOf(
    WorkoutCategory("🏋️", "Ngực", "CHEST"),
    WorkoutCategory("🔙", "Lưng", "BACK"),
    WorkoutCategory("🦵", "Chân", "LEGS"),
    WorkoutCategory("💪", "Tay", "ARMS"),
    WorkoutCategory("🔥", "Cardio", "CARDIO"),
    WorkoutCategory("🧘", "Core", "CORE")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    navController: NavController,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Luyện tập", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    TextButton(onClick = { navController.navigate(Screen.Activity) }) {
                        Text("Hoạt động", color = Mint500, fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppSurface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.WorkoutBuilder) },
                containerColor = Mint500,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Bắt đầu tập")
            }
        },
        containerColor = AppMutedBackground
    ) { padding ->
        when {
            uiState.isLoading -> LoadingState(modifier = Modifier.padding(padding))
            uiState.error != null -> ErrorState(
                message = uiState.error!!,
                onRetry = viewModel::loadData,
                modifier = Modifier.padding(padding)
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Today summary
                item {
                    val activity = uiState.activityLog
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clip(RoundedCornerShape(VitalRadius.Xl))
                            .background(Brush.horizontalGradient(listOf(Mint500, Mint700)))
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            WorkoutStat("🔥", "${activity?.caloriesBurned?.toInt() ?: 0}", "kcal")
                            WorkoutStat("⏱️", "${activity?.activeMinutes ?: 0}", "phút")
                            WorkoutStat("👟", "${activity?.steps ?: 0}", "bước")
                        }
                    }
                }

                // Quick actions
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { navController.navigate(Screen.ExerciseLibrary) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(VitalRadius.Md),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Mint500)
                        ) {
                            Text("Thư viện bài tập", color = Mint500, fontSize = 13.sp)
                        }
                        OutlinedButton(
                            onClick = { navController.navigate(Screen.WorkoutBuilder) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(VitalRadius.Md),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Mint500)
                        ) {
                            Text("Bắt đầu tập", color = Mint500, fontSize = 13.sp)
                        }
                    }
                }

                // Categories
                item {
                    Text(
                        "Danh mục",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Ink900,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                item {
                    CategoryGrid(
                        categories = workoutCategories,
                        selected = uiState.selectedMuscleGroup,
                        onSelect = viewModel::filterByMuscleGroup
                    )
                }

                // Exercises
                item {
                    Text(
                        "Bài tập gợi ý",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Ink900,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                if (uiState.exercises.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            Text("Chưa có bài tập nào", color = Ink500)
                        }
                    }
                } else {
                    items(uiState.exercises) { exercise ->
                        ExerciseItem(exercise = exercise)
                    }
                }

                // Today sessions
                if (uiState.sessions.isNotEmpty()) {
                    item {
                        Text(
                            "Phiên tập hôm nay",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Ink900,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(uiState.sessions) { session ->
                        SessionItem(session = session)
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutStat(emoji: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 24.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
        Text(unit, fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
    }
}

@Composable
private fun CategoryGrid(
    categories: List<WorkoutCategory>,
    selected: String?,
    onSelect: (String?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        for (row in categories.chunked(2)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { cat ->
                    val isSelected = selected == cat.muscleGroup
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(80.dp)
                            .clickable { onSelect(if (isSelected) null else cat.muscleGroup) },
                        shape = RoundedCornerShape(VitalRadius.Lg),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Mint500 else AppSurface
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Mint500 else AppLine),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(cat.emoji, fontSize = 24.sp)
                            Text(
                                cat.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color.White else Ink900
                            )
                        }
                    }
                }
                if (row.size < 2) Spacer(Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ExerciseItem(exercise: ExerciseDto) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(VitalRadius.Lg),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppLine),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Mint50),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = exercise.displayImageUrl,
                    contentDescription = exercise.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Ink900)
                Text(exercise.muscleGroup, fontSize = 12.sp, color = Ink500)
            }
            Text("${exercise.caloriesPerMin.toInt()} kcal/phút", fontSize = 12.sp, color = Mint500)
        }
    }
}

@Composable
private fun SessionItem(session: WorkoutSessionDto) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(VitalRadius.Lg),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppLine),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Mint100),
                contentAlignment = Alignment.Center
            ) { Text("🏃", fontSize = 20.sp) }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(session.sessionName ?: "Phiên tập", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Ink900)
                Text("${session.totalDurationMinutes} phút", fontSize = 12.sp, color = Ink500)
            }
            Text("${session.totalCaloriesBurned.toInt()} kcal", fontSize = 12.sp, color = Mint500, fontWeight = FontWeight.SemiBold)
        }
    }
}
