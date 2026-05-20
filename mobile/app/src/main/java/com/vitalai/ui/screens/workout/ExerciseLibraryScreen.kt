package com.vitalai.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vitalai.data.remote.model.ExerciseDto
import com.vitalai.navigation.Screen
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.theme.*

private val muscleGroups = listOf(
    null to "Tất cả",
    "Ngực" to "Ngực",
    "Lưng" to "Lưng",
    "Chân" to "Chân",
    "Vai" to "Vai",
    "Tay" to "Tay",
    "Bụng" to "Bụng"
)

private val intensities = listOf(null to "Tất cả", "Nhẹ" to "Nhẹ", "Vừa" to "Vừa", "Nặng" to "Nặng")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseLibraryScreen(
    navController: NavController,
    viewModel: ExerciseLibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thư viện bài tập", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppSurface)
            )
        },
        containerColor = AppBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                placeholder = { Text("Tìm kiếm bài tập...", color = Ink500) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Ink500) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Mint500,
                    unfocusedBorderColor = Ink200,
                    focusedContainerColor = AppSurface,
                    unfocusedContainerColor = AppSurface
                ),
                singleLine = true
            )

            // Muscle group filters
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppSurface)
                    .padding(vertical = 6.dp)
            ) {
                items(muscleGroups) { (key, label) ->
                    val isSelected = uiState.selectedMuscleGroup == key
                    PillChip(
                        label = label,
                        isSelected = isSelected,
                        onClick = { viewModel.setMuscleFilter(if (isSelected) null else key) }
                    )
                }
            }

            // Intensity toggle row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                intensities.forEach { (key, label) ->
                    val isSelected = uiState.selectedIntensity == key
                    val color = when (key) {
                        "Nặng" -> MacroProtein
                        "Vừa" -> MacroCarbs
                        else -> if (isSelected) Mint500 else Ink500
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setIntensityFilter(if (isSelected) null else key) },
                        label = {
                            Text(label, fontSize = 12.sp,
                                color = if (isSelected) Color.White else color,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = color,
                            selectedLabelColor = Color.White,
                            containerColor = AppSurface
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true, selected = isSelected,
                            borderColor = color.copy(alpha = 0.4f),
                            selectedBorderColor = color
                        )
                    )
                }
            }

            when {
                uiState.isLoading -> LoadingState()
                uiState.error != null -> {
                    // Show mock exercises on error
                    ExerciseListContent(
                        exercises = mockExercises(),
                        favorites = uiState.favorites,
                        onExerciseClick = { navController.navigate(Screen.ExerciseDetail(it.id)) },
                        onFavoriteToggle = { viewModel.toggleFavorite(it) }
                    )
                }
                uiState.filteredExercises.isEmpty() -> {
                    ExerciseListContent(
                        exercises = mockExercises(),
                        favorites = uiState.favorites,
                        onExerciseClick = { navController.navigate(Screen.ExerciseDetail(it.id)) },
                        onFavoriteToggle = { viewModel.toggleFavorite(it) }
                    )
                }
                else -> ExerciseListContent(
                    exercises = uiState.filteredExercises,
                    favorites = uiState.favorites,
                    onExerciseClick = { navController.navigate(Screen.ExerciseDetail(it.id)) },
                    onFavoriteToggle = { viewModel.toggleFavorite(it) }
                )
            }
        }
    }
}

@Composable
private fun ExerciseListContent(
    exercises: List<ExerciseDto>,
    favorites: Set<String>,
    onExerciseClick: (ExerciseDto) -> Unit,
    onFavoriteToggle: (String) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        items(exercises) { exercise ->
            ExerciseCard(
                exercise = exercise,
                isFavorite = exercise.id in favorites,
                onClick = { onExerciseClick(exercise) },
                onFavoriteClick = { onFavoriteToggle(exercise.id) }
            )
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: ExerciseDto,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            val imageUrl = exercise.imageUrl
                ?: "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=200&q=80"
            AsyncImage(
                model = imageUrl,
                contentDescription = exercise.name,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        exercise.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Ink900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MuscleGroupPill(exercise.muscleGroup)
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${(exercise.caloriesPerMin * 60).toInt()} kcal/h",
                        fontSize = 11.sp,
                        color = MacroProtein,
                        fontWeight = FontWeight.Medium
                    )
                    if (exercise.equipment != null) {
                        Text("•", fontSize = 11.sp, color = Ink300)
                        Text(exercise.equipment, fontSize = 11.sp, color = Ink500)
                    }
                }
            }
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Yêu thích",
                    tint = if (isFavorite) MacroProtein else Ink300
                )
            }
        }
    }
}

@Composable
private fun MuscleGroupPill(muscle: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100))
            .background(Mint50)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(muscle, fontSize = 10.sp, color = Mint700, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PillChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100))
            .background(if (isSelected) Mint500 else AppSurface)
            .border(
                width = 1.dp,
                color = if (isSelected) Mint500 else Ink200,
                shape = RoundedCornerShape(100)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            label,
            fontSize = 13.sp,
            color = if (isSelected) Color.White else Ink700,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

internal fun mockExercises(): List<ExerciseDto> = listOf(
    ExerciseDto("1", "Bench Press", "Ngực", "Tạ đơn", "Bài tập ngực kinh điển", "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=200", 5.0f),
    ExerciseDto("2", "Squat", "Chân", "Tạ đòn", "Bài tập toàn thân hiệu quả", "https://images.unsplash.com/photo-1538805060514-97d9cc17730c?w=200", 6.0f),
    ExerciseDto("3", "Pull-up", "Lưng", "Thanh xà", "Bài tập lưng và tay", "https://images.unsplash.com/photo-1571019614242-c5c5dee9f50b?w=200", 4.5f),
    ExerciseDto("4", "Shoulder Press", "Vai", "Tạ đơn", "Tăng sức mạnh vai", null, 4.0f),
    ExerciseDto("5", "Plank", "Bụng", "Không cần", "Tăng sức bền lõi cơ", null, 3.5f),
    ExerciseDto("6", "Bicep Curl", "Tay", "Tạ đơn", "Tập cơ tay trước", null, 3.0f),
    ExerciseDto("7", "Deadlift", "Lưng", "Tạ đòn", "Bài tập toàn thân nặng", "https://images.unsplash.com/photo-1566241142559-40e1dab266c6?w=200", 7.0f),
    ExerciseDto("8", "Chạy bộ", "Cardio", "Không cần", "Cardio cơ bản", null, 8.0f)
)
