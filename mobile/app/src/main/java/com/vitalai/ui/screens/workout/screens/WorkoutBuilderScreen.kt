package com.vitalai.ui.screens.workout.screens

import com.vitalai.ui.screens.workout.components.*
import com.vitalai.ui.screens.workout.viewmodels.*

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.*
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vitalai.data.remote.model.ExerciseDto
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun WorkoutBuilderScreen(
    navController: NavController,
    viewModel: WorkoutBuilderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Saved → confirm with a toast, then return to the Train dashboard (which
    // reloads its history on resume). Keeps the transition intentional, not abrupt.
    LaunchedEffect(uiState.savedSessionId) {
        if (uiState.savedSessionId != null) {
            Toast.makeText(context, "Đã lưu buổi tập 💪", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
    }

    // Surface save failures instead of silently staying on the form.
    LaunchedEffect(uiState.error) {
        uiState.error?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
    }

    WorkoutBuilderScreenContent(
        uiState = uiState,
        onBackClick = { navController.popBackStack() },
        onSaveSession = viewModel::saveSession,
        onSessionNameChange = viewModel::setSessionName,
        onSessionNotesChange = viewModel::setSessionNotes,
        onAddSet = viewModel::addSet,
        onUpdateSet = viewModel::updateSet,
        onRemoveExercise = viewModel::removeExercise,
        onShowExercisePicker = viewModel::setShowExercisePicker,
        onAddExercise = viewModel::addExercise,
        onUpdateSport = viewModel::updateSportEntry,
        onUpdateCardio = viewModel::updateCardioEntry
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutBuilderScreenContent(
    uiState: WorkoutBuilderUiState,
    onBackClick: () -> Unit,
    onSaveSession: () -> Unit,
    onSessionNameChange: (String) -> Unit,
    onSessionNotesChange: (String) -> Unit,
    onAddSet: (Int) -> Unit,
    onUpdateSet: (Int, Int, String?, String?, Boolean?) -> Unit,
    onRemoveExercise: (Int) -> Unit,
    onShowExercisePicker: (Boolean) -> Unit,
    onAddExercise: (ExerciseDto) -> Unit,
    onUpdateSport: (Int, Int?, String?) -> Unit = { _, _, _ -> },
    onUpdateCardio: (Int, Float?, Float?) -> Unit = { _, _, _ -> }
) {
    val dateFormatted = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM", Locale("vi")))
    }
    val totalMinutes = uiState.exercises.sumOf { estimateDuration(it) }
    val totalKcal = uiState.exercises.sumOf { estimateCalories(it) }

    TrainScreen {
        Column(Modifier.fillMaxSize()) {
            TrainHeader(
                title = "Buổi tập $dateFormatted",
                onBack = onBackClick,
                right = {
                    TrainRoundIconButton(
                        icon = Icons.Default.Delete,
                        contentDescription = "Xóa",
                        onClick = onBackClick
                    )
                }
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = uiState.sessionName,
                        onValueChange = onSessionNameChange,
                        placeholder = { Text("Đặt tên buổi tập", color = TrainColors.Charcoal.copy(alpha = 0.5f), fontSize = 24.sp) },
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TrainColors.Forest,
                            fontFamily = com.vitalai.ui.theme.VitalDisplayFontFamily
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TrainColors.Forest,
                            unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedContainerColor = TrainColors.Cream,
                            unfocusedContainerColor = TrainColors.Cream,
                            cursorColor = TrainColors.Forest
                        ),
                        singleLine = true
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TrainStatTile(
                            icon = Icons.Default.LocalFireDepartment,
                            value = totalKcal.toString(),
                            label = "Kcal đã đốt",
                            tone = TrainTone.Mint,
                            modifier = Modifier.weight(1f)
                        )
                        TrainStatTile(
                            icon = Icons.Default.AccessTime,
                            value = totalMinutes.toString(),
                            label = "Phút thời gian",
                            tone = TrainTone.Kiss,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Column {
                        TrainSectionTitle(
                            text = "Danh sách bài tập" + if (uiState.exercises.isNotEmpty()) " (${uiState.exercises.size})" else "",
                            modifier = Modifier.padding(bottom = 11.dp)
                        )
                        if (uiState.exercises.isEmpty()) {
                            EmptyWorkoutCard()
                        }
                    }
                }
                itemsIndexed(uiState.exercises) { index, builderExercise ->
                    SessionExerciseCard(
                        builderExercise = builderExercise,
                        index = index,
                        onAddSet = { onAddSet(index) },
                        onUpdateSet = { setIdx, reps, kg, done -> onUpdateSet(index, setIdx, reps, kg, done) },
                        onUpdateSport = { duration, intensity -> onUpdateSport(index, duration, intensity) },
                        onUpdateCardio = { distance, speed -> onUpdateCardio(index, distance, speed) },
                        onRemove = { onRemoveExercise(index) }
                    )
                }
                item {
                    TrainGhostButton(
                        text = "Thêm bài tập",
                        icon = Icons.Default.Add,
                        onClick = { onShowExercisePicker(true) }
                    )
                }
                item {
                    Column {
                        TrainSectionTitle("Ghi chú buổi tập", modifier = Modifier.padding(bottom = 11.dp))
                        OutlinedTextField(
                            value = uiState.sessionNotes,
                            onValueChange = onSessionNotesChange,
                            placeholder = { Text("Cảm nhận, mục tiêu, điều chỉnh...") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TrainColors.Forest,
                                unfocusedBorderColor = TrainColors.Border,
                                focusedContainerColor = TrainColors.Cream,
                                unfocusedContainerColor = TrainColors.Cream
                            )
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TrainColors.Cream)
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                TrainPrimaryButton(
                    text = if (uiState.isSaving) "Đang lưu..." else "Lưu buổi tập",
                    icon = Icons.Default.Check,
                    enabled = !uiState.isSaving && uiState.exercises.isNotEmpty(),
                    onClick = onSaveSession
                )
            }
        }

        if (uiState.showExercisePicker) {
            ExercisePickerSheet(
                isLoading = uiState.isLoadingExercises,
                exercises = uiState.availableExercises,
                onDismiss = { onShowExercisePicker(false) },
                onAddExercise = onAddExercise
            )
        }
    }
}

@Composable
private fun EmptyWorkoutCard() {
    TrainCard(tone = TrainTone.Keylime, padding = PaddingValues(28.dp)) {
        Icon(
            Icons.Default.Add,
            contentDescription = null,
            tint = TrainColors.Forest.copy(alpha = 0.55f),
            modifier = Modifier.align(Alignment.CenterHorizontally).size(34.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Chưa có bài tập nào",
            modifier = Modifier.fillMaxWidth(),
            color = TrainColors.Forest,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            "Thêm bài tập đầu tiên để bắt đầu",
            modifier = Modifier.fillMaxWidth(),
            color = TrainColors.Charcoal,
            fontSize = 12.5.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun SessionExerciseCard(
    builderExercise: BuilderExercise,
    index: Int,
    onAddSet: () -> Unit,
    onUpdateSet: (Int, String?, String?, Boolean?) -> Unit,
    onUpdateSport: (Int?, String?) -> Unit,
    onUpdateCardio: (Float?, Float?) -> Unit,
    onRemove: () -> Unit
) {
    val exercise = builderExercise.exercise
    var menuOpen by remember { mutableStateOf(false) }
    TrainCard(tone = TrainTone.Cream, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            Box(
                modifier = Modifier.size(26.dp).clip(CircleShape).background(TrainColors.KeylimeWash),
                contentAlignment = Alignment.Center
            ) {
                Text("${index + 1}", color = TrainColors.Forest, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            TrainExerciseThumb(exercise, size = 44.dp, radius = 11.dp)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(exercise.name, color = TrainColors.Ink, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    TrainTypeBadge(exercise.exerciseType)
                }
                Text(itemLine(builderExercise), color = TrainColors.Charcoal, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${estimateDuration(builderExercise)} phút · ${estimateCalories(builderExercise)} kcal", color = TrainColors.Forest, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Box {
                TrainRoundIconButton(
                    icon = Icons.Default.MoreHoriz,
                    contentDescription = "Tùy chọn",
                    onClick = { menuOpen = true },
                    background = TrainColors.Cream,
                    tint = TrainColors.Charcoal,
                    size = 34.dp
                )
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Chỉnh sửa") },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = TrainColors.Forest) },
                        onClick = { menuOpen = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Xóa bài tập", color = TrainColors.Cardio) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = TrainColors.Cardio) },
                        onClick = {
                            menuOpen = false
                            onRemove()
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        when (exercise.exerciseType.uppercase()) {
            "GYM" -> GymBuilderControls(builderExercise, onUpdateSet, onAddSet)
            "CARDIO" -> CardioBuilderControls(builderExercise, onUpdateCardio)
            else -> SportBuilderControls(builderExercise, onUpdateSport)
        }
    }
}

@Composable
private fun GymBuilderControls(
    builderExercise: BuilderExercise,
    onUpdateSet: (Int, String?, String?, Boolean?) -> Unit,
    onAddSet: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("SET", color = TrainColors.Charcoal, fontSize = 11.sp, modifier = Modifier.width(34.dp))
            Text("REPS", color = TrainColors.Charcoal, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text("KG", color = TrainColors.Charcoal, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text("✓", color = TrainColors.Charcoal, fontSize = 11.sp, modifier = Modifier.width(44.dp))
        }
        builderExercise.sets.forEachIndexed { setIndex, set ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(set.setNumber.toString(), color = TrainColors.Forest, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(26.dp))
                CompactNumberField(set.reps, Modifier.weight(1f)) { onUpdateSet(setIndex, it, null, null) }
                CompactNumberField(set.kg, Modifier.weight(1f), KeyboardType.Decimal) { onUpdateSet(setIndex, null, it, null) }
                Checkbox(
                    checked = set.isDone,
                    onCheckedChange = { onUpdateSet(setIndex, null, null, it) },
                    colors = CheckboxDefaults.colors(checkedColor = TrainColors.Forest),
                    modifier = Modifier.width(44.dp)
                )
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(TrainColors.KeylimeWash).clickable(onClick = onAddSet).padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("+ Thêm set", color = TrainColors.Forest, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun SportBuilderControls(builderExercise: BuilderExercise, onUpdate: (Int?, String?) -> Unit) {
    var duration by remember(builderExercise.durationMinutes) { mutableFloatStateOf(builderExercise.durationMinutes.toFloat()) }
    var intensity by remember(builderExercise.intensityLevel) { mutableStateOf(builderExercise.intensityLevel) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Thời lượng: ${duration.roundToInt()} phút", color = TrainColors.Charcoal, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Slider(
            value = duration,
            onValueChange = { duration = it },
            onValueChangeFinished = { onUpdate(duration.roundToInt(), null) },
            valueRange = 5f..180f,
            colors = SliderDefaults.colors(thumbColor = TrainColors.Forest, activeTrackColor = TrainColors.Forest, inactiveTrackColor = TrainColors.KeylimeWash)
        )
        TrainSegmented(
            options = listOf("LOW" to "Nhẹ", "MEDIUM" to "Vừa", "HIGH" to "Cao"),
            selected = intensity,
            onSelect = {
                intensity = it
                onUpdate(null, it)
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CardioBuilderControls(builderExercise: BuilderExercise, onUpdate: (Float?, Float?) -> Unit) {
    var distance by remember(builderExercise.distanceKm) { mutableStateOf(builderExercise.distanceKm.toString()) }
    var speed by remember(builderExercise.avgSpeedKmh) { mutableStateOf(builderExercise.avgSpeedKmh.toString()) }
    val dist = distance.toFloatOrNull() ?: 0f
    val spd = speed.toFloatOrNull()?.coerceAtLeast(0.5f) ?: 10f
    val duration = (dist / spd * 60).roundToInt().coerceAtLeast(1)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CompactNumberField(distance, Modifier.weight(1f), KeyboardType.Decimal, label = "Khoảng cách (km)") {
                distance = it
                onUpdate(it.toFloatOrNull(), null)
            }
            CompactNumberField(speed, Modifier.weight(1f), KeyboardType.Decimal, label = "Tốc độ (km/h)") {
                speed = it
                onUpdate(null, it.toFloatOrNull())
            }
        }
        TrainCard(tone = TrainTone.Keylime, padding = PaddingValues(12.dp)) {
            Text("$duration phút · pace ${formatBuilderPace(spd)}", color = TrainColors.Forest, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CompactNumberField(
    value: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Number,
    label: String? = null,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label?.let { { Text(it, fontSize = 11.sp) } },
        singleLine = true,
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = TrainColors.Forest,
            unfocusedBorderColor = TrainColors.Border,
            focusedContainerColor = TrainColors.Cream,
            unfocusedContainerColor = TrainColors.Cream
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercisePickerSheet(
    isLoading: Boolean,
    exercises: List<ExerciseDto>,
    onDismiss: () -> Unit,
    onAddExercise: (ExerciseDto) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = TrainColors.Cream,
        dragHandle = {
            Box(Modifier.padding(top = 10.dp).size(width = 40.dp, height = 5.dp).clip(CircleShape).background(TrainColors.Border))
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 21.dp, end = 21.dp, bottom = 28.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Chọn bài tập", color = TrainColors.Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                TrainRoundIconButton(Icons.Default.Close, "Đóng", onDismiss, background = TrainColors.KeylimeWash, tint = TrainColors.Forest, size = 32.dp)
            }
            when {
                isLoading -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TrainColors.Forest)
                }
                exercises.isEmpty() -> Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
                    Text("Chưa có bài tập nào", color = TrainColors.Charcoal, fontSize = 13.sp)
                }
                else -> LazyColumn(modifier = Modifier.heightIn(max = 460.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(exercises) { _, exercise ->
                        TrainCard(tone = TrainTone.Cream, padding = PaddingValues(12.dp), onClick = { onAddExercise(exercise) }) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                TrainExerciseThumb(exercise, size = 44.dp, radius = 11.dp)
                                Column(Modifier.weight(1f)) {
                                    Text(exercise.name, color = TrainColors.Ink, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(exercise.muscleGroup, color = TrainColors.Charcoal, fontSize = 12.sp)
                                }
                                Box(Modifier.size(32.dp).clip(CircleShape).background(TrainColors.Forest), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = TrainColors.Cream, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun itemLine(item: BuilderExercise): String {
    val ex = item.exercise
    return when (ex.exerciseType.uppercase()) {
        "GYM" -> {
            val first = item.sets.firstOrNull()
            "${item.sets.size} sets × ${first?.reps ?: ex.defaultReps ?: 10} reps"
        }
        "CARDIO" -> "${item.distanceKm} km @ ${item.avgSpeedKmh} km/h · pace ${formatBuilderPace(item.avgSpeedKmh)}"
        else -> "${item.durationMinutes} phút · ${builderIntensityLabel(item.intensityLevel)}"
    }
}

private fun estimateDuration(item: BuilderExercise): Int = when (item.exercise.exerciseType.uppercase()) {
    "GYM" -> {
        val reps = item.sets.mapNotNull { it.reps.toIntOrNull() }.average().takeIf { !it.isNaN() } ?: (item.exercise.defaultReps ?: 10).toDouble()
        ((item.sets.size * reps * 3 + (item.sets.size - 1).coerceAtLeast(0) * (item.exercise.restTimeSeconds ?: 90)) / 60.0).roundToInt().coerceAtLeast(1)
    }
    "CARDIO" -> (item.distanceKm / item.avgSpeedKmh.coerceAtLeast(0.5f) * 60).roundToInt().coerceAtLeast(1)
    else -> item.durationMinutes
}

private fun estimateCalories(item: BuilderExercise): Int = when (item.exercise.exerciseType.uppercase()) {
    "GYM" -> (5f * 70f * estimateDuration(item) / 60f).roundToInt()
    "CARDIO" -> {
        val speed = item.avgSpeedKmh.coerceAtLeast(0.5f)
        val met = when {
            speed < 6f -> 3.5f
            speed < 8f -> 6f
            speed < 10f -> 8.3f
            speed < 12f -> 9.8f
            else -> 11f
        }
        (met * 70f * (item.distanceKm / speed)).roundToInt()
    }
    else -> {
        val rate = when (item.intensityLevel) {
            "LOW" -> 4
            "HIGH" -> 10
            else -> 7
        }
        rate * item.durationMinutes
    }
}

private fun formatBuilderPace(speedKmh: Float): String {
    if (speedKmh <= 0f) return "--:-- /km"
    val pace = 60f / speedKmh
    val min = pace.toInt()
    val sec = ((pace - min) * 60).roundToInt().coerceIn(0, 59)
    return "$min:${sec.toString().padStart(2, '0')} /km"
}

private fun builderIntensityLabel(value: String) = when (value.uppercase()) {
    "LOW" -> "Nhẹ"
    "HIGH" -> "Cao"
    else -> "Vừa"
}
