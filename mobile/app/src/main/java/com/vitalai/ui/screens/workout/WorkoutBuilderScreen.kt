package com.vitalai.ui.screens.workout

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vitalai.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutBuilderScreen(
    navController: NavController,
    viewModel: WorkoutBuilderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val dateFormatted = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("vi")))
    }

    val totalKcal = uiState.exercises.sumOf { ex ->
        (ex.sets.count { it.isDone } * ex.exercise.caloriesPerMin * 3).toDouble()
    }.toInt()

    val totalVolume = uiState.exercises.sumOf { ex ->
        ex.sets.sumOf { set -> (set.reps.toIntOrNull() ?: 0) * (set.kg.toFloatOrNull()?.toInt() ?: 0) }
    }

    Scaffold(
        containerColor = AppBackground,
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = AppSurface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("~$totalKcal kcal", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MacroProtein)
                        Text("${totalVolume}kg tổng KL", fontSize = 11.sp, color = Ink500)
                    }
                    Button(
                        onClick = viewModel::saveSession,
                        enabled = !uiState.isSaving,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Mint500)
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Lưu buổi tập", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppSurface)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng", tint = Ink700)
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(dateFormatted, fontSize = 12.sp, color = Ink500)
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Tùy chọn", tint = Ink700)
                    }
                }
            }

            // Session name
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    TextField(
                        value = uiState.sessionName,
                        onValueChange = viewModel::setSessionName,
                        textStyle = LocalTextStyle.current.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            color = Ink900
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Tên buổi tập...", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink300) }
                    )

                    // Timer
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BlinkingDot()
                        Text(
                            formatTime(uiState.elapsedSeconds),
                            fontSize = 16.sp,
                            color = Ink700,
                            fontWeight = FontWeight.Medium
                        )
                        Text("đang tập", fontSize = 13.sp, color = Ink500)
                    }
                }
            }

            // Exercises
            itemsIndexed(uiState.exercises) { exerciseIdx, builderEx ->
                ExerciseBlock(
                    builderExercise = builderEx,
                    exerciseIndex = exerciseIdx,
                    onAddSet = { viewModel.addSet(exerciseIdx) },
                    onUpdateSet = { setIdx, reps, kg, done ->
                        viewModel.updateSet(exerciseIdx, setIdx, reps, kg, done)
                    },
                    onRemove = { viewModel.removeExercise(exerciseIdx) }
                )
            }

            // Add exercise button
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(
                            width = 1.5.dp,
                            color = Ink200,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { viewModel.setShowExercisePicker(true) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Mint500)
                        Text("Thêm bài tập", color = Mint500, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    // Exercise picker bottom sheet
    if (uiState.showExercisePicker) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setShowExercisePicker(false) },
            sheetState = sheetState,
            containerColor = AppSurface
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Chọn bài tập", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Ink900)
                Spacer(Modifier.height(12.dp))
                mockExercises().forEach { ex ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.addExercise(ex) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Mint50),
                            contentAlignment = Alignment.Center
                        ) { Text("💪", fontSize = 18.sp) }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(ex.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Ink900)
                            Text(ex.muscleGroup, fontSize = 11.sp, color = Ink500)
                        }
                    }
                    HorizontalDivider(color = Ink200, thickness = 0.5.dp)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    // Navigate away on save
    if (uiState.savedSessionId != null) {
        LaunchedEffect(uiState.savedSessionId) {
            navController.popBackStack()
        }
    }
}

@Composable
private fun ExerciseBlock(
    builderExercise: BuilderExercise,
    exerciseIndex: Int,
    onAddSet: () -> Unit,
    onUpdateSet: (Int, String?, String?, Boolean?) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Exercise header
            Row(verticalAlignment = Alignment.CenterVertically) {
                val imgUrl = builderExercise.exercise.imageUrl
                    ?: "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?w=100"
                AsyncImage(
                    model = imgUrl,
                    contentDescription = builderExercise.exercise.name,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(builderExercise.exercise.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Ink900)
                    Text(
                        "${builderExercise.exercise.muscleGroup} • ${builderExercise.sets.size} sets",
                        fontSize = 11.sp, color = Ink500
                    )
                }
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Close, contentDescription = "Xóa", tint = Ink500, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            // Table header
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("SET", fontSize = 11.sp, color = Ink500, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                Text("REPS", fontSize = 11.sp, color = Ink500, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("KG", fontSize = 11.sp, color = Ink500, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Text("✓", fontSize = 11.sp, color = Ink500, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
            }
            HorizontalDivider(color = Ink200, modifier = Modifier.padding(vertical = 4.dp))

            // Sets
            builderExercise.sets.forEachIndexed { setIdx, set ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${set.setNumber}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink700,
                        modifier = Modifier.width(32.dp),
                        textAlign = TextAlign.Center
                    )
                    OutlinedTextField(
                        value = set.reps,
                        onValueChange = { onUpdateSet(setIdx, it, null, null) },
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 13.sp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Mint500,
                            unfocusedBorderColor = Ink200
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = set.kg,
                        onValueChange = { onUpdateSet(setIdx, null, it, null) },
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 13.sp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Mint500,
                            unfocusedBorderColor = Ink200
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Checkbox(
                        checked = set.isDone,
                        onCheckedChange = { onUpdateSet(setIdx, null, null, it) },
                        modifier = Modifier.width(40.dp),
                        colors = CheckboxDefaults.colors(checkedColor = Mint500)
                    )
                }
            }

            // Add set
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .border(width = 1.dp, color = Ink200, shape = RoundedCornerShape(8.dp))
                    .clickable(onClick = onAddSet)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Ink500, modifier = Modifier.size(14.dp))
                    Text("Thêm set", fontSize = 12.sp, color = Ink500)
                }
            }
        }
    }
}

@Composable
private fun BlinkingDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(MacroProtein.copy(alpha = alpha))
    )
}

private fun formatTime(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%02d:%02d:%02d".format(h, m, s)
    else "%02d:%02d".format(m, s)
}
