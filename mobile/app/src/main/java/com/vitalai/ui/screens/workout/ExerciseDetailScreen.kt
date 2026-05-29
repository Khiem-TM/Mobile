package com.vitalai.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vitalai.data.remote.model.AddExerciseRequest
import com.vitalai.data.remote.model.ExerciseDto
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    id: String,
    navController: NavController,
    libraryViewModel: ExerciseLibraryViewModel = hiltViewModel(),
    detailViewModel: ExerciseDetailViewModel = hiltViewModel()
) {
    val libState by libraryViewModel.uiState.collectAsState()
    val detailState by detailViewModel.uiState.collectAsState()
    val exercise = libState.exercises.firstOrNull { it.id == id }
        ?: libState.filteredExercises.firstOrNull { it.id == id }
        ?: detailState.exercise
    val isFavorite = exercise?.id in libState.favorites
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showAddSheet by remember { mutableStateOf(false) }

    LaunchedEffect(detailState.saveSuccess, detailState.error) {
        when {
            detailState.saveSuccess -> {
                snackbarHostState.showSnackbar("Đã thêm vào buổi tập")
                detailViewModel.clearSaveState()
                showAddSheet = false
            }
            detailState.error != null -> {
                snackbarHostState.showSnackbar(detailState.error ?: "Có lỗi xảy ra")
                detailViewModel.clearSaveState()
            }
        }
    }

    Scaffold(
        containerColor = TrainColors.Cream,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            ExerciseDetailScreenContent(
                exercise = exercise,
                isLoading = libState.isLoading && exercise == null,
                errorMessage = libState.error,
                isFavorite = isFavorite,
                onRetry = { libraryViewModel.loadExercises() },
                onBackClick = { navController.popBackStack() },
                onFavoriteToggle = {
                    exercise?.let {
                        scope.launch { libraryViewModel.toggleFavorite(it.id) }
                    }
                },
                onAddToWorkoutClick = { showAddSheet = true }
            )
        }
    }

    if (showAddSheet && exercise != null) {
        AddToSessionSheet(
            exercise = exercise,
            isSaving = detailState.isSaving,
            onDismiss = { showAddSheet = false },
            onSubmit = detailViewModel::addToTodaySession
        )
    }
}

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
    when {
        isLoading -> LoadingState(modifier = Modifier.fillMaxSize())
        exercise == null -> ErrorState(message = errorMessage ?: "Không tìm thấy bài tập", onRetry = onRetry)
        else -> DetailShell(
            exercise = exercise,
            isFavorite = isFavorite,
            onBackClick = onBackClick,
            onFavoriteToggle = onFavoriteToggle,
            onAddToWorkoutClick = onAddToWorkoutClick
        )
    }
}

@Composable
private fun DetailShell(
    exercise: ExerciseDto,
    isFavorite: Boolean,
    onBackClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onAddToWorkoutClick: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    TrainScreen {
        Column(Modifier.fillMaxSize()) {
            TrainHeader(
                title = exercise.name,
                subtitle = exercise.category ?: exercise.muscleGroup,
                onBack = onBackClick,
                right = {
                    TrainRoundIconButton(
                        icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Yêu thích",
                        onClick = onFavoriteToggle,
                        tint = if (isFavorite) TrainColors.Cardio else TrainColors.Ink
                    )
                }
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    TrainCard(tone = TrainTone.Mint, padding = PaddingValues(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            TrainExerciseThumb(exercise, size = 82.dp, radius = 18.dp)
                            Column(Modifier.weight(1f)) {
                                TrainTypeBadge(exercise.exerciseType)
                                Spacer(Modifier.height(8.dp))
                                TrainDisplayTitle(exercise.name, maxLinesModifier())
                                Text(
                                    exercise.description ?: "Chưa có mô tả",
                                    color = TrainColors.Forest.copy(alpha = 0.78f),
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (!exercise.videoUrl.isNullOrBlank()) {
                            Spacer(Modifier.height(14.dp))
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(TrainColors.Forest)
                                    .clickable { uriHandler.openUri(exercise.videoUrl) }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = TrainColors.Cream, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Xem video hướng dẫn", color = TrainColors.Cream, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
                item {
                    when (exercise.exerciseType.uppercase()) {
                        "GYM" -> GymDetailContent(exercise)
                        "CARDIO" -> CardioDetailContent(exercise)
                        else -> SportDetailContent(exercise)
                    }
                }
                item { TextBlocks(exercise) }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TrainColors.Cream)
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                TrainPrimaryButton(text = "Thêm vào buổi tập", icon = Icons.Default.Add, onClick = onAddToWorkoutClick)
            }
        }
    }
}

@Composable
private fun maxLinesModifier(): Modifier = Modifier

@Composable
private fun GymDetailContent(exercise: ExerciseDto) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Section("Thông số mặc định") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DefaultStat((exercise.defaultSets ?: 3).toString(), "Sets", Modifier.weight(1f))
                DefaultStat((exercise.defaultReps ?: 10).toString(), "Reps", Modifier.weight(1f))
                DefaultStat("${exercise.defaultWeightKg ?: 0f} kg", "Tạ", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            InfoRow("Nghỉ giữa set", "${exercise.restTimeSeconds ?: 90} giây")
            if (exercise.metValue > 0f) InfoRow("MET", "%.1f".format(exercise.metValue))
        }
        Section("Dụng cụ") {
            InfoRow("Thiết bị", exercise.equipment ?: "Không cần dụng cụ")
        }
    }
}

@Composable
private fun SportDetailContent(exercise: ExerciseDto) {
    val rates = listOf("LOW" to 4, "MEDIUM" to 7, "HIGH" to 10)
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Section("Ước tính tiêu hao calo") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rates.forEach { (key, rate) ->
                    DefaultStat(rate.toString(), "kcal/phút · ${intensityLabel(key)}", Modifier.weight(1f), tone = if (key == "MEDIUM") TrainTone.Mint else TrainTone.Keylime)
                }
            }
        }
        TrainCard(tone = TrainTone.Cream, padding = PaddingValues(16.dp)) {
            InfoRow("Thời lượng khuyến nghị", "${exercise.defaultDurationMinutes ?: 30} phút")
            InfoRow("Loại vận động", exercise.movementType ?: "Toàn thân")
            InfoRow("Cường độ mặc định", intensityLabel(exercise.defaultIntensityLevel ?: "MEDIUM"))
            if (exercise.metValue > 0f) InfoRow("MET", "%.1f".format(exercise.metValue))
        }
    }
}

@Composable
private fun CardioDetailContent(exercise: ExerciseDto) {
    var distance by remember { mutableFloatStateOf(5f) }
    var speed by remember { mutableFloatStateOf(10f) }
    val duration = cardioDuration(distance, speed)
    val kcal = cardioCalories(exercise, distance, speed)
    val met = if (exercise.metValue > 0f) exercise.metValue else runningMet(speed)
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Section("Mô phỏng nhanh") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TrainNumberStepper("Khoảng cách (km)", distance, { distance = it }, 0.5f, 0.5f, 1000f, Modifier.weight(1f), decimals = 1)
                TrainNumberStepper("Tốc độ (km/h)", speed, { speed = it }, 0.5f, 0.5f, 80f, Modifier.weight(1f), decimals = 1)
            }
            Spacer(Modifier.height(14.dp))
            TrainCard(tone = TrainTone.Mint, padding = PaddingValues(16.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
                    SimStat("${duration.roundToInt()}", "Thời gian", "phút")
                    SimStat(formatPace(speed), "Pace", "/km")
                    SimStat("~$kcal", "Ước tính", "kcal")
                }
            }
        }
        Section("Bảng MET theo tốc độ") {
            TrainCard(tone = TrainTone.Cream, padding = PaddingValues(16.dp)) {
                InfoRow("< 6 km/h", "MET 3.5")
                InfoRow("6-8 km/h", "MET 6.0")
                InfoRow("8-10 km/h", "MET 8.3")
                InfoRow("> 10 km/h", "MET 9.8+")
            }
            Text("Đang áp dụng MET %.1f cho tốc độ $speed km/h".format(met), color = TrainColors.Charcoal.copy(alpha = 0.7f), fontSize = 11.5.sp, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        TrainSectionTitle(title, modifier = Modifier.padding(bottom = 11.dp))
        content()
    }
}

@Composable
private fun DefaultStat(value: String, label: String, modifier: Modifier = Modifier, tone: TrainTone = TrainTone.Keylime) {
    TrainCard(modifier = modifier, tone = tone, padding = PaddingValues(14.dp)) {
        Text(value, modifier = Modifier.fillMaxWidth(), color = TrainColors.Forest, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text(label, modifier = Modifier.fillMaxWidth(), color = TrainColors.Charcoal, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = TrainColors.Forest, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = TrainColors.Charcoal, fontSize = 13.5.sp, modifier = Modifier.weight(1f))
        Text(value, color = TrainColors.Forest, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SimStat(value: String, label: String, sub: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = TrainColors.Forest, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TrainColors.Charcoal, fontSize = 11.sp)
        Text(sub, color = TrainColors.Charcoal.copy(alpha = 0.6f), fontSize = 10.sp)
    }
}

@Composable
private fun TextBlocks(exercise: ExerciseDto) {
    val instructions = splitExerciseText(exercise.instructions)
    val tips = splitExerciseText(exercise.formTips)
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Section("Cách thực hiện") {
            TrainCard(tone = TrainTone.Cream, padding = PaddingValues(16.dp)) {
                if (instructions.isEmpty()) {
                    Text("Chưa có hướng dẫn", color = TrainColors.Charcoal, fontSize = 13.sp)
                } else {
                    instructions.forEachIndexed { index, step ->
                        Text("${index + 1}. $step", color = TrainColors.Charcoal, fontSize = 13.5.sp, lineHeight = 20.sp)
                        if (index < instructions.lastIndex) Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
        Section("Mẹo tập đúng") {
            TrainCard(tone = TrainTone.Keylime, padding = PaddingValues(16.dp)) {
                if (tips.isEmpty()) {
                    Text("Chưa có mẹo", color = TrainColors.Charcoal, fontSize = 13.sp)
                } else {
                    tips.forEach { tip ->
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = TrainColors.Forest, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(tip, color = TrainColors.Charcoal, fontSize = 13.5.sp, lineHeight = 20.sp)
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToSessionSheet(
    exercise: ExerciseDto,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (AddExerciseRequest) -> Unit
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Thêm ${exercise.name}", color = TrainColors.Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                TrainRoundIconButton(icon = Icons.Default.Close, contentDescription = "Đóng", onClick = onDismiss, background = TrainColors.KeylimeWash, tint = TrainColors.Forest, size = 32.dp)
            }
            when (exercise.exerciseType.uppercase()) {
                "GYM" -> GymAddSheet(exercise, isSaving, onSubmit)
                "CARDIO" -> CardioAddSheet(exercise, isSaving, onSubmit)
                else -> SportAddSheet(exercise, isSaving, onSubmit)
            }
        }
    }
}

@Composable
private fun GymAddSheet(exercise: ExerciseDto, isSaving: Boolean, onSubmit: (AddExerciseRequest) -> Unit) {
    var sets by remember { mutableFloatStateOf((exercise.defaultSets ?: 3).toFloat()) }
    var reps by remember { mutableFloatStateOf((exercise.defaultReps ?: 10).toFloat()) }
    var weight by remember { mutableFloatStateOf(exercise.defaultWeightKg ?: 0f) }
    var rest by remember { mutableFloatStateOf((exercise.restTimeSeconds ?: 90).toFloat()) }
    var intensity by remember { mutableStateOf("MEDIUM") }
    val duration = ceil((sets * reps * 3 + (sets - 1).coerceAtLeast(0f) * rest) / 60f).roundToInt().coerceAtLeast(1)
    val kcal = ((gymIntensityMet(intensity) * 70f * duration) / 60f).roundToInt()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TrainNumberStepper("Sets", sets, { sets = it }, 1f, 1f, 20f, Modifier.weight(1f))
            TrainNumberStepper("Reps", reps, { reps = it }, 1f, 1f, 100f, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TrainNumberStepper("Tạ (kg)", weight, { weight = it }, 2.5f, 0f, 500f, Modifier.weight(1f), decimals = 1)
            TrainNumberStepper("Nghỉ (giây)", rest, { rest = it }, 15f, 0f, 600f, Modifier.weight(1f))
        }
        IntensitySelector(intensity) { intensity = it }
        CaloriePreview(duration, kcal, "${sets.roundToInt()}×${reps.roundToInt()} @ ${weight.roundToInt()}kg")
        TrainPrimaryButton(text = if (isSaving) "Đang thêm..." else "Thêm vào buổi tập", icon = Icons.Default.Check, enabled = !isSaving) {
            onSubmit(AddExerciseRequest(exercise.id, "GYM", sets.roundToInt(), reps.roundToInt(), weight, duration, 0, intensity, null, null, rest.roundToInt()))
        }
    }
}

@Composable
private fun SportAddSheet(exercise: ExerciseDto, isSaving: Boolean, onSubmit: (AddExerciseRequest) -> Unit) {
    var duration by remember { mutableFloatStateOf((exercise.defaultDurationMinutes ?: 30).toFloat()) }
    var intensity by remember { mutableStateOf(exercise.defaultIntensityLevel ?: "MEDIUM") }
    var distance by remember { mutableStateOf("") }
    val kcal = ((exercise.metValue.takeIf { it > 0f } ?: sportRate(intensity)) * duration).roundToInt()
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TrainNumberStepper("Thời lượng (phút)", duration, { duration = it }, 5f, 1f, 600f, Modifier.fillMaxWidth())
        IntensitySelector(intensity) { intensity = it }
        OutlinedTextField(
            value = distance,
            onValueChange = { distance = it },
            label = { Text("Khoảng cách (km) · tùy chọn") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = TrainColors.Forest, unfocusedBorderColor = TrainColors.Border, focusedContainerColor = TrainColors.Cream, unfocusedContainerColor = TrainColors.Cream)
        )
        CaloriePreview(duration.roundToInt(), kcal, intensityLabel(intensity))
        TrainPrimaryButton(text = if (isSaving) "Đang thêm..." else "Thêm vào buổi tập", icon = Icons.Default.Check, enabled = !isSaving) {
            onSubmit(AddExerciseRequest(exercise.id, "SPORT", null, null, null, duration.roundToInt(), 0, intensity, distance.toFloatOrNull(), null, null))
        }
    }
}

@Composable
private fun CardioAddSheet(exercise: ExerciseDto, isSaving: Boolean, onSubmit: (AddExerciseRequest) -> Unit) {
    var distance by remember { mutableFloatStateOf(5f) }
    var speed by remember { mutableFloatStateOf(10f) }
    val duration = cardioDuration(distance, speed).roundToInt()
    val kcal = cardioCalories(exercise, distance, speed)
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TrainNumberStepper("Khoảng cách (km)", distance, { distance = it }, 0.5f, 0.1f, 1000f, Modifier.fillMaxWidth(), decimals = 1)
        TrainNumberStepper("Tốc độ trung bình (km/h)", speed, { speed = it }, 0.5f, 0.5f, 80f, Modifier.fillMaxWidth(), decimals = 1)
        TrainCard(tone = TrainTone.Keylime, padding = PaddingValues(14.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxWidth()) {
                SimStat(duration.toString(), "Thời gian", "phút")
                SimStat(formatPace(speed), "Pace", "/km")
            }
        }
        CaloriePreview(duration, kcal, "${distance}km @ ${speed}km/h")
        TrainPrimaryButton(text = if (isSaving) "Đang thêm..." else "Thêm vào buổi tập", icon = Icons.Default.Check, enabled = !isSaving) {
            onSubmit(AddExerciseRequest(exercise.id, "CARDIO", null, null, null, duration, 0, null, distance, speed, null))
        }
    }
}

@Composable
private fun TrainNumberStepper(
    label: String,
    value: Float,
    onChange: (Float) -> Unit,
    step: Float,
    min: Float,
    max: Float,
    modifier: Modifier = Modifier,
    decimals: Int = 0
) {
    Column(modifier) {
        Text(label, color = TrainColors.Charcoal, fontSize = 13.sp, modifier = Modifier.padding(bottom = 7.dp))
        Row(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(TrainColors.KeylimeWash).padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StepButton("-") { onChange((value - step).coerceIn(min, max)) }
            Text(
                if (decimals > 0) "%.${decimals}f".format(value) else value.roundToInt().toString(),
                color = TrainColors.Forest,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
            StepButton("+") { onChange((value + step).coerceIn(min, max)) }
        }
    }
}

@Composable
private fun StepButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(TrainColors.Cream).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = TrainColors.Forest, fontSize = 22.sp)
    }
}

@Composable
private fun IntensitySelector(selected: String, onSelect: (String) -> Unit) {
    Column {
        Text("Cường độ", color = TrainColors.Charcoal, fontSize = 13.sp, modifier = Modifier.padding(bottom = 7.dp))
        TrainSegmented(
            options = listOf("LOW" to "Nhẹ", "MEDIUM" to "Vừa", "HIGH" to "Cao"),
            selected = selected,
            onSelect = onSelect,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun CaloriePreview(duration: Int, kcal: Int, extra: String) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(TrainColors.Forest).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("Ước tính tiêu hao", color = TrainColors.MintGlaze, fontSize = 11.5.sp)
            Text("$extra · ~$duration phút", color = TrainColors.MintGlaze.copy(alpha = 0.82f), fontSize = 12.sp)
        }
        Text("~$kcal kcal", color = TrainColors.Cream, fontSize = 23.sp, fontWeight = FontWeight.Bold)
    }
}

private fun splitExerciseText(text: String?): List<String> {
    if (text.isNullOrBlank()) return emptyList()
    return text
        .replace(Regex("\\s+(?=\\d+[.)]\\s+)"), "\n")
        .lines()
        .map { it.trim().replace(Regex("^\\d+[.)]\\s*"), "").removePrefix("-").removePrefix("•").trim() }
        .filter { it.isNotBlank() }
}

private fun runningMet(speed: Float) = when {
    speed < 6f -> 3.5f
    speed < 8f -> 6f
    speed < 10f -> 8.3f
    speed < 12f -> 9.8f
    else -> 11f
}

private fun cardioDuration(distanceKm: Float, speedKmh: Float): Float = (distanceKm / speedKmh.coerceAtLeast(0.5f) * 60f).coerceAtLeast(1f)
private fun cardioCalories(exercise: ExerciseDto, distanceKm: Float, speedKmh: Float): Int {
    val met = if (exercise.metValue > 0f) exercise.metValue else runningMet(speedKmh)
    return (met * 70f * (distanceKm / speedKmh.coerceAtLeast(0.5f))).roundToInt()
}
private fun formatPace(speedKmh: Float): String {
    if (speedKmh <= 0f) return "--:--"
    val paceMin = 60f / speedKmh
    val mins = paceMin.toInt()
    val secs = ((paceMin - mins) * 60).roundToInt().coerceIn(0, 59)
    return "$mins:${secs.toString().padStart(2, '0')}"
}
private fun gymIntensityMet(intensity: String) = when (intensity) {
    "LOW" -> 3.5f
    "HIGH" -> 6.5f
    else -> 5f
}
private fun sportRate(intensity: String) = when (intensity) {
    "LOW" -> 4f
    "HIGH" -> 10f
    else -> 7f
}
private fun intensityLabel(intensity: String) = when (intensity.uppercase()) {
    "LOW" -> "Nhẹ"
    "HIGH" -> "Cao"
    else -> "Vừa"
}
