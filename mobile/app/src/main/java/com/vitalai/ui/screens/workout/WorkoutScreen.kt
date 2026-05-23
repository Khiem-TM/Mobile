package com.vitalai.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FrontHand
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vitalai.data.remote.model.ActivityLogDto
import com.vitalai.data.remote.model.ExerciseDto
import com.vitalai.data.remote.model.WorkoutSessionDto
import com.vitalai.navigation.Screen
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.theme.*

data class WorkoutCategory(
    val icon: ImageVector,
    val name: String,
    val muscleGroup: String,
    val subtitle: String,
    val tintColor: Color
)

val workoutCategories = listOf(
    WorkoutCategory(Icons.Default.FitnessCenter, "Ngực", "CHEST", "Phát triển thân trên", Color(0xFFE53935)),
    WorkoutCategory(Icons.Default.AccessibilityNew, "Lưng", "BACK", "Cơ xô & kéo", Color(0xFF1E88E5)),
    WorkoutCategory(Icons.Default.DirectionsRun, "Chân", "LEGS", "Sức mạnh thân dưới", Color(0xFF43A047)),
    WorkoutCategory(Icons.Default.FrontHand, "Tay", "ARMS", "Bắp tay trước & sau", Color(0xFFFDD835)),
    WorkoutCategory(Icons.Default.Favorite, "Cardio", "CARDIO", "Đốt mỡ, tim mạch", Color(0xFFE91E63)),
    WorkoutCategory(Icons.Default.SelfImprovement, "Core", "CORE", "Cơ bụng săn chắc", Color(0xFF8E24AA))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreen(
    navController: NavController,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    WorkoutScreenContent(
        uiState = uiState,
        onBackClick = { navController.popBackStack() },
        onActivityClick = { navController.navigate(Screen.Activity) },
        onStartWorkoutClick = { navController.navigate(Screen.WorkoutBuilder) },
        onLibraryClick = { navController.navigate(Screen.ExerciseLibrary) },
        onFilterByMuscleGroup = viewModel::filterByMuscleGroup,
        onRetry = viewModel::loadData
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScreenContent(
    uiState: WorkoutUiState,
    onBackClick: () -> Unit,
    onActivityClick: () -> Unit,
    onStartWorkoutClick: () -> Unit,
    onLibraryClick: () -> Unit,
    onFilterByMuscleGroup: (String?) -> Unit,
    onRetry: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Luyện tập", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    TextButton(onClick = onActivityClick) {
                        Text("Hoạt động", color = Mint500, fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppSurface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onStartWorkoutClick,
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
                onRetry = onRetry,
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
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Hôm nay đã đốt", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                                Surface(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = androidx.compose.foundation.shape.CircleShape
                                ) {
                                    Text(
                                        text = "🔥 12 ngày",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            // Main Value Row
                            Row(
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Text(
                                    text = "${activity?.caloriesBurned?.toInt() ?: 0}",
                                    color = Color.White,
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "kcal · ${activity?.activeMinutes ?: 0} phút",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            // Divider & Footer Row
                            HorizontalDivider(color = Color.White.copy(alpha = 0.2f), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text("${activity?.steps ?: 0}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Text("steps", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${activity?.waterMl ?: 0} ml", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Text("water", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${uiState.sessions.size}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Text("sessions", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Quick actions
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Card 1
                        Card(
                            onClick = onLibraryClick,
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = AppSurface),
                            shape = RoundedCornerShape(VitalRadius.Xl),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AppLine),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Mint50),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.MenuBook,
                                            contentDescription = null,
                                            tint = Mint500,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Text("A", fontSize = 12.sp, color = Ink500, fontWeight = FontWeight.Medium)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Thư viện bài tập", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink900)
                                Text("Danh sách bài tập", fontSize = 12.sp, color = Ink500)
                            }
                        }

                        // Card 2
                        Card(
                            onClick = onStartWorkoutClick,
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = AppSurface),
                            shape = RoundedCornerShape(VitalRadius.Xl),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AppLine),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFE3F2FD)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color(0xFF1976D2),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Text("B", fontSize = 12.sp, color = Ink500, fontWeight = FontWeight.Medium)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Bắt đầu tập", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink900)
                                Text("Khởi động ngay", fontSize = 12.sp, color = Ink500)
                            }
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
                        onSelect = onFilterByMuscleGroup
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
                            .height(145.dp)
                            .clickable { onSelect(if (isSelected) null else cat.muscleGroup) },
                        shape = RoundedCornerShape(VitalRadius.Xl),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) cat.tintColor.copy(alpha = 0.05f) else AppSurface
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, if (isSelected) cat.tintColor else AppLine),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            // Top-Start Icon
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(cat.tintColor.copy(alpha = 0.15f))
                                    .align(Alignment.TopStart),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = null,
                                    tint = cat.tintColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            
                            // Bottom-Start Texts
                            Column(modifier = Modifier.align(Alignment.BottomStart)) {
                                Text(
                                    text = cat.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Ink900
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = cat.subtitle,
                                    fontSize = 13.sp,
                                    color = Ink500,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
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
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Ink900)
                Spacer(modifier = Modifier.height(2.dp))
                Text(exercise.muscleGroup, fontSize = 13.sp, color = Ink500)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${exercise.caloriesPerMin.toInt()}", fontSize = 19.sp, color = Mint500, fontWeight = FontWeight.Bold)
                Text("kcal/phút", fontSize = 12.sp, color = Ink500)
            }
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Mint100),
                contentAlignment = Alignment.Center
            ) { Text("🏃", fontSize = 20.sp) }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(session.sessionName ?: "Phiên tập", fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Ink900)
                Spacer(modifier = Modifier.height(2.dp))
                Text("06:30 · ${session.totalDurationMinutes} min", fontSize = 13.sp, color = Ink500)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${session.totalCaloriesBurned.toInt()}", fontSize = 19.sp, color = Mint500, fontWeight = FontWeight.Bold)
                Text("kcal", fontSize = 12.sp, color = Ink500)
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun WorkoutScreenPreview() {
    val mockState = WorkoutUiState(
        sessions = listOf(
            WorkoutSessionDto(
                id = "1",
                sessionName = "Morning Cardio",
                sessionDate = "2026-05-13",
                totalDurationMinutes = 35,
                totalCaloriesBurned = 320f,
                details = emptyList()
            )
        ),
        exercises = listOf(
            ExerciseDto(
                id = "1",
                name = "Chạy bộ",
                primaryMuscleGroup = "CARDIO",
                equipment = null,
                description = null,
                imageAvtUrl = null,
                caloriesPerMin = 10f
            ),
            ExerciseDto(
                id = "2",
                name = "Push Up",
                primaryMuscleGroup = "CHEST",
                equipment = null,
                description = null,
                imageAvtUrl = null,
                caloriesPerMin = 8f
            )
        ),
        activityLog = ActivityLogDto(
            id = "log-1",
            logDate = "2026-05-23",
            steps = 8240,
            caloriesBurned = 320f,
            activeMinutes = 45,
            waterMl = 0
        ),
        selectedMuscleGroup = "CARDIO",
        isLoading = false,
        error = null
    )

    VitalAITheme {
        WorkoutScreenContent(
            uiState = mockState,
            onBackClick = {},
            onActivityClick = {},
            onStartWorkoutClick = {},
            onLibraryClick = {},
            onFilterByMuscleGroup = {},
            onRetry = {}
        )
    }
}

