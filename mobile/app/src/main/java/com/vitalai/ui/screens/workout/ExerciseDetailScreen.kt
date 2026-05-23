package com.vitalai.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircle
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vitalai.data.remote.model.ExerciseDto
import coil.compose.AsyncImage
import com.vitalai.navigation.Screen
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    id: String,
    navController: NavController,
    viewModel: ExerciseLibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val exercise = uiState.exercises.firstOrNull { it.id == id }
        ?: uiState.filteredExercises.firstOrNull { it.id == id }

    ExerciseDetailScreenContent(
        exercise = exercise,
        isLoading = uiState.isLoading && exercise == null,
        errorMessage = uiState.error,
        isFavorite = exercise?.id in uiState.favorites,
        onRetry = { viewModel.loadExercises() },
        onBackClick = { navController.popBackStack() },
        onFavoriteToggle = { exercise?.let { viewModel.toggleFavorite(it.id) } },
        onAddToWorkoutClick = { navController.navigate(Screen.WorkoutBuilder) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreenContent(
    exercise: ExerciseDto?,
    isLoading: Boolean,
    errorMessage: String?,
    isFavorite: Boolean,
    onRetry: () -> Unit,
    onBackClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onAddToWorkoutClick: () -> Unit
) {
    if (isLoading) {
        LoadingState()
        return
    }

    if (exercise == null) {
        ErrorState(
            message = errorMessage ?: "Không tìm thấy bài tập",
            onRetry = onRetry
        )
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Cách thực hiện", "Mẹo tập đúng", "Biến thể")

    val heroImageUrl = exercise.imageAvtUrl

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(AppBackground)
    ) {
        // Hero image
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                AsyncImage(
                    model = heroImageUrl,
                    contentDescription = exercise.name,
                    modifier = Modifier.fillMaxSize().background(Mint50),
                    contentScale = ContentScale.Crop
                )
                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(0.3f), Color.Transparent, Color.Black.copy(0.4f))
                            )
                        )
                )
                // Top bar overlay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .statusBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(0.4f))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = Color.White)
                    }
                    Row {
                        IconButton(
                            onClick = onFavoriteToggle,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(0.4f))
                        ) {
                            Icon(
                                if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Yêu thích",
                                tint = if (isFavorite) MacroProtein else Color.White
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {},
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(0.4f))
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Tùy chọn", tint = Color.White)
                        }
                    }
                }
                // Play button center
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayCircle,
                        contentDescription = "Phát video",
                        tint = Color.White.copy(0.9f),
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
        }

        // Content
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                // Tags
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TagPill(exercise.muscleGroup, Mint500, Color.White)
                    TagPill(exercise.intensity?.let(::formatIntensity) ?: "moderate", MacroCarbs, Color.White)
                    exercise.equipment?.let { TagPill(it, Ink100, Ink700) }
                }
                Spacer(Modifier.height(12.dp))
                // Title
                Text(exercise.name, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Ink900)
                Spacer(Modifier.height(4.dp))
                Text(
                    exercise.description ?: "Chưa có mô tả",
                    fontSize = 14.sp, color = Ink500, lineHeight = 20.sp
                )
                Spacer(Modifier.height(16.dp))
                // Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard("MET", "${exercise.metValue}", "điểm", Mint500, Modifier.weight(1f))
                    StatCard("Năng lượng", "${(exercise.caloriesPerMin * 60).toInt()}", "kcal/h", MacroProtein, Modifier.weight(1f))
                    StatCard("Cường độ", exercise.intensity?.let(::formatIntensity) ?: "N/A", "", MacroCarbs, Modifier.weight(1f))
                }
            }
        }

        // Tabs
        item {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = AppSurface,
                contentColor = Mint500,
                edgePadding = 16.dp
            ) {
                tabs.forEachIndexed { i, title ->
                    Tab(
                        selected = selectedTab == i,
                        onClick = { selectedTab = i },
                        text = {
                            Text(
                                title, fontSize = 13.sp,
                                fontWeight = if (selectedTab == i) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }

        // Tab content
        item {
            when (selectedTab) {
                0 -> StepsContent(exercise.instructions)
                1 -> TipsContent(exercise.formTips)
                2 -> VariantsContent()
            }
        }

        // CTA
        item {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedIconButton(
                    onClick = onFavoriteToggle,
                    border = ButtonDefaults.outlinedButtonBorder,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Yêu thích",
                        tint = if (isFavorite) MacroProtein else Ink500
                    )
                }
                Button(
                    onClick = onAddToWorkoutClick,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Mint500)
                ) {
                    Text("Thêm vào buổi tập", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TagPill(text: String, bgColor: Color, textColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 11.sp, color = textColor, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun StatCard(label: String, value: String, unit: String, accentColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = accentColor)
            Text(unit, fontSize = 10.sp, color = Ink500)
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 11.sp, color = Ink700, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun StepsContent(instructions: String?) {
    val steps = remember(instructions) { splitExerciseText(instructions) }
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (steps.isEmpty()) {
            Text("Chưa có hướng dẫn", fontSize = 13.sp, color = Ink500)
        } else {
            steps.forEachIndexed { i, step ->
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Mint500),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${i + 1}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(step, fontSize = 13.sp, color = Ink700, lineHeight = 18.sp, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
    }
}

@Composable
private fun TipsContent(formTips: String?) {
    val tips = remember(formTips) { splitExerciseText(formTips) }
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (tips.isEmpty()) {
            Text("Chưa có mẹo", fontSize = 13.sp, color = Ink500)
        } else {
            tips.forEach { tip ->
                Row(verticalAlignment = Alignment.Top) {
                    Text("✓", color = Mint500, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp))
                    Text(tip, fontSize = 13.sp, color = Ink700, lineHeight = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun VariantsContent() {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Đang cập nhật...", fontSize = 13.sp, color = Ink500)
    }
}

private fun splitExerciseText(text: String?): List<String> {
    if (text.isNullOrBlank()) return emptyList()
    return text
        .replace(Regex("\\s+(?=\\d+[.)]\\s+)"), "\n")
        .lines()
        .map {
            it.trim()
                .replace(Regex("^\\d+[.)]\\s*"), "")
                .removePrefix("-")
                .removePrefix("•")
                .trim()
        }
        .filter { it.isNotBlank() }
}

private fun formatIntensity(intensity: String): String {
    return when (intensity.lowercase()) {
        "light" -> "Nhẹ"
        "moderate" -> "Vừa"
        "heavy" -> "Nặng"
        else -> intensity
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ExerciseDetailScreenPreview() {
    val mockExercise = ExerciseDto(
        id = "1",
        name = "Push Up",
        description = "Bài tập hít đất cơ bản giúp phát triển cơ ngực, vai và bắp tay sau.",
        primaryMuscleGroup = "CHEST",
        equipment = "Bodyweight",
        imageAvtUrl = null,
        imageUrl = null,
        intensity = "moderate",
        caloriesPerMin = 0.1f,
        metValue = 3.8f,
        instructions = "1. Bắt đầu ở tư thế plank cao.\n2. Hạ thân người xuống cho đến khi ngực gần chạm sàn.\n3. Đẩy người lên lại vị trí ban đầu.",
        formTips = "Giữ lưng thẳng trong suốt quá trình tập.\nKhông để hông bị chùng xuống."
    )

    VitalAITheme {
        ExerciseDetailScreenContent(
            exercise = mockExercise,
            isLoading = false,
            errorMessage = null,
            isFavorite = true,
            onRetry = {},
            onBackClick = {},
            onFavoriteToggle = {},
            onAddToWorkoutClick = {}
        )
    }
}
