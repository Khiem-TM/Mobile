package com.vitalai.ui.screens.workout.screens

import com.vitalai.ui.screens.workout.components.*
import com.vitalai.ui.screens.workout.viewmodels.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vitalai.data.remote.model.ExerciseDto
import com.vitalai.navigation.Screen
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.theme.BottomSheetGrabber

private val exerciseTypes = listOf(
    null to "Tất cả",
    "GYM" to "GYM",
    "SPORT" to "SPORT",
    "CARDIO" to "CARDIO"
)

private val muscleGroups = listOf(
    null to "Tất cả",
    "CHEST" to "Ngực",
    "BACK" to "Lưng",
    "LEGS" to "Chân",
    "SHOULDERS" to "Vai",
    "ARMS" to "Tay",
    "FULL_BODY" to "Toàn thân",
    "CARDIO" to "Chạy bộ",
    "CYCLING" to "Đạp xe"
)

private val difficultyFilters = listOf(
    null to "Tất cả",
    "BEGINNER" to "Dễ",
    "INTERMEDIATE" to "Vừa",
    "ADVANCED" to "Khó"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseLibraryScreen(
    navController: NavController,
    viewModel: ExerciseLibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    ExerciseLibraryScreenContent(
        uiState = uiState,
        onBackClick = { navController.popBackStack() },
        onSearchQueryChange = viewModel::setSearchQuery,
        onTypeSelect = viewModel::selectType,
        onMuscleFilterSelect = viewModel::setMuscleFilter,
        onDifficultySelect = viewModel::setIntensityFilter,
        onExerciseClick = { navController.navigate(Screen.ExerciseDetail(it.id)) },
        onFavoriteToggle = { viewModel.toggleFavorite(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseLibraryScreenContent(
    uiState: ExerciseLibraryUiState,
    onBackClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onTypeSelect: (String?) -> Unit,
    onMuscleFilterSelect: (String?) -> Unit,
    onDifficultySelect: (String?) -> Unit,
    onExerciseClick: (ExerciseDto) -> Unit,
    onFavoriteToggle: (String) -> Unit
) {
    var tab by remember { mutableStateOf("all") }
    var filterOpen by remember { mutableStateOf(false) }
    val activeFilterCount = listOf(uiState.selectedMuscleGroup, uiState.selectedIntensity).count { it != null }
    val list = remember(uiState.filteredExercises, uiState.favorites, tab) {
        val base = if (tab == "favorites") {
            uiState.filteredExercises.filter { it.id in uiState.favorites }
        } else {
            uiState.filteredExercises
        }
        base.sortedByDescending { it.favoritesCount }
    }

    TrainScreen {
        Column(Modifier.fillMaxSize()) {
            TrainHeader(title = "Thư viện bài tập", large = true, onBack = onBackClick)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TrainColors.Cream)
                    .padding(start = 18.dp, end = 18.dp, top = 12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SearchBox(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (activeFilterCount > 0) TrainColors.Forest else TrainColors.KeylimeWash)
                            .border(
                                1.dp,
                                if (activeFilterCount > 0) TrainColors.Forest else TrainColors.Border,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { filterOpen = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Bộ lọc",
                            tint = if (activeFilterCount > 0) TrainColors.Cream else TrainColors.Forest
                        )
                        if (activeFilterCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = (-7).dp, y = 7.dp)
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(TrainColors.Cardio),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(activeFilterCount.toString(), color = TrainColors.Cream, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 12.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    exerciseTypes.forEach { (key, label) ->
                        Box(Modifier.weight(1f)) {
                            TrainChip(
                                text = label,
                                selected = uiState.selectedType == key,
                                grow = true,
                                onClick = { onTypeSelect(key) }
                            )
                        }
                    }
                }
                ActiveFiltersRow(
                    selectedMuscle = uiState.selectedMuscleGroup,
                    selectedDifficulty = uiState.selectedIntensity,
                    onClearMuscle = { onMuscleFilterSelect(null) },
                    onClearDifficulty = { onDifficultySelect(null) },
                    onClearAll = {
                        onMuscleFilterSelect(null)
                        onDifficultySelect(null)
                    }
                )
                HorizontalDivider(color = TrainColors.Border)
            }

            when {
                uiState.isInitialLoading -> LoadingState(modifier = Modifier.weight(1f))
                uiState.error != null -> EmptyState(message = uiState.error)
                list.isEmpty() -> EmptyState(message = "Không tìm thấy bài tập phù hợp")
                else -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        TrainSectionTitle(
                            text = if (tab == "favorites") "Bài tập yêu thích" else if (uiState.searchQuery.isNotBlank() || uiState.selectedType != null || uiState.selectedMuscleGroup != null || uiState.selectedIntensity != null) "${list.size} kết quả" else "Phổ biến nhất",
                            modifier = Modifier.padding(bottom = 1.dp)
                        )
                    }
                    items(list, key = { it.id }) { exercise ->
                        ExerciseRow(
                            exercise = exercise,
                            isFavorite = exercise.id in uiState.favorites,
                            showTypeBadge = uiState.selectedType == null,
                            onClick = { onExerciseClick(exercise) },
                            onFavoriteToggle = { onFavoriteToggle(exercise.id) }
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(TrainColors.Cream)
                .border(1.dp, TrainColors.Border)
                .navigationBarsPadding()
                .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 10.dp)
        ) {
            TrainSegmented(
                options = listOf("all" to "Tất cả bài tập", "favorites" to "♥ Yêu thích"),
                selected = tab,
                onSelect = { tab = it },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (filterOpen) {
            FilterSheet(
                selectedMuscle = uiState.selectedMuscleGroup,
                selectedDifficulty = uiState.selectedIntensity,
                onDismiss = { filterOpen = false },
                onApply = { muscle, diff ->
                    onMuscleFilterSelect(muscle)
                    onDifficultySelect(diff)
                    filterOpen = false
                }
            )
        }
    }
}

@Composable
private fun SearchBox(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(TrainColors.KeylimeWash)
            .border(1.dp, TrainColors.Border.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
            .padding(start = 13.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(TrainColors.Cream.copy(alpha = 0.82f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = TrainColors.Forest, modifier = Modifier.size(17.dp))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            cursorBrush = androidx.compose.ui.graphics.SolidColor(TrainColors.Forest),
            textStyle = LocalTextStyle.current.copy(
                color = TrainColors.Ink,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Medium
            ),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isBlank()) {
                        Text(
                            "Tìm kiếm bài tập",
                            color = TrainColors.Charcoal.copy(alpha = 0.52f),
                            fontSize = 14.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
            }
        )
        if (value.isNotBlank()) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(TrainColors.Cream.copy(alpha = 0.82f))
                    .clickable { onValueChange("") },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Xóa tìm kiếm",
                    tint = TrainColors.Charcoal.copy(alpha = 0.72f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun ActiveFiltersRow(
    selectedMuscle: String?,
    selectedDifficulty: String?,
    onClearMuscle: () -> Unit,
    onClearDifficulty: () -> Unit,
    onClearAll: () -> Unit
) {
    if (selectedMuscle == null && selectedDifficulty == null) return
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        selectedMuscle?.let {
            ActiveChip(labelFor(muscleGroups, it), onClearMuscle)
        }
        selectedDifficulty?.let {
            ActiveChip(labelFor(difficultyFilters, it), onClearDifficulty)
        }
        Text(
            "Xóa lọc",
            modifier = Modifier.clickable(onClick = onClearAll).padding(horizontal = 4.dp, vertical = 6.dp),
            color = TrainColors.Charcoal,
            fontSize = 12.5.sp
        )
    }
}

@Composable
private fun ActiveChip(text: String, onClear: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(TrainColors.MintGlaze)
            .padding(start = 13.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text, color = TrainColors.Forest, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(TrainColors.Forest.copy(alpha = 0.15f))
                .clickable(onClick = onClear),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Close, contentDescription = null, tint = TrainColors.Forest, modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
private fun EmptyState(message: String?) {
    Box(Modifier.fillMaxSize().padding(30.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Search, contentDescription = null, tint = TrainColors.MintGlaze, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(14.dp))
            Text(message ?: "Không tìm thấy bài tập phù hợp", color = TrainColors.Charcoal, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ExerciseRow(
    exercise: ExerciseDto,
    isFavorite: Boolean,
    showTypeBadge: Boolean,
    onClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    val displayedCount = (exercise.favoritesCount + if (isFavorite && !exercise.isFavorite) 1 else if (!isFavorite && exercise.isFavorite) -1 else 0).coerceAtLeast(0)
    TrainCard(tone = TrainTone.Cream, padding = PaddingValues(12.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            TrainExerciseThumb(exercise = exercise, size = 56.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    exercise.name,
                    color = TrainColors.Ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    if (showTypeBadge) {
                        TrainTypeBadge(exercise.exerciseType)
                    }
                    Text(exercise.category ?: exercise.muscleGroup, color = TrainColors.Charcoal, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("·", color = TrainColors.Charcoal.copy(alpha = 0.45f), fontSize = 12.sp)
                    DifficultyBadge(exercise.difficultyLevel)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onFavoriteToggle).padding(4.dp)) {
                Icon(
                    if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Yêu thích",
                    tint = if (isFavorite) TrainColors.Cardio else TrainColors.Charcoal,
                    modifier = Modifier.size(20.dp)
                )
                Text(displayedCount.toString(), color = TrainColors.Charcoal, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun DifficultyBadge(difficulty: String?) {
    val (background, foreground) = when (difficulty?.uppercase()) {
        "BEGINNER" -> TrainColors.GymBg to TrainColors.Forest
        "ADVANCED" -> TrainColors.CardioBg to TrainColors.Cardio
        else -> TrainColors.SportBg to TrainColors.Sport
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = difficultyLabel(difficulty),
            color = foreground,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(
    selectedMuscle: String?,
    selectedDifficulty: String?,
    onDismiss: () -> Unit,
    onApply: (String?, String?) -> Unit
) {
    var muscle by remember(selectedMuscle) { mutableStateOf(selectedMuscle) }
    var difficulty by remember(selectedDifficulty) { mutableStateOf(selectedDifficulty) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        contentWindowInsets = { WindowInsets(0) },
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp)
                    .size(width = 40.dp, height = 5.dp)
                    .clip(CircleShape)
                    .background(BottomSheetGrabber)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 21.dp, end = 21.dp, bottom = 28.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text("Bộ lọc", color = TrainColors.Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Column {
                TrainSectionTitle("Nhóm cơ / loại", modifier = Modifier.padding(bottom = 11.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    muscleGroups.forEach { (key, label) ->
                        TrainChip(text = label, selected = muscle == key, onClick = { muscle = if (muscle == key) null else key })
                    }
                }
            }
            Column {
                TrainSectionTitle("Độ khó", modifier = Modifier.padding(bottom = 11.dp))
                TrainSegmented(
                    options = difficultyFilters,
                    selected = difficulty,
                    onSelect = { difficulty = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = {
                        muscle = null
                        difficulty = null
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TrainColors.KeylimeWash, contentColor = TrainColors.Forest),
                    shape = RoundedCornerShape(TrainButtonRadius),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) { Text("Đặt lại", fontWeight = FontWeight.Medium) }
                TrainPrimaryButton(
                    text = "Áp dụng",
                    modifier = Modifier.weight(2f),
                    onClick = { onApply(muscle, difficulty) }
                )
            }
        }
    }
}

private fun labelFor(options: List<Pair<String?, String>>, key: String): String =
    options.firstOrNull { it.first == key }?.second ?: key

private fun difficultyLabel(value: String?): String = when (value?.uppercase()) {
    "BEGINNER" -> "Dễ"
    "INTERMEDIATE" -> "Vừa"
    "ADVANCED" -> "Khó"
    else -> value ?: "Vừa"
}
