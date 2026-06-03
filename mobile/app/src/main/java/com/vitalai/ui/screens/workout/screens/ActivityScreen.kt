package com.vitalai.ui.screens.workout.screens

import com.vitalai.ui.screens.workout.components.*
import com.vitalai.ui.screens.workout.viewmodels.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vitalai.data.remote.model.ActivityLogDto
import com.vitalai.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MOOD_OPTIONS = listOf(
    "happy" to "😊",
    "neutral" to "😐",
    "sad" to "😔",
    "angry" to "😤",
    "energetic" to "💪"
)

private val DAY_LABELS = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    navController: NavController,
    initialDate: String = "",
    viewModel: ActivityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(initialDate) {
        if (initialDate.isNotBlank() && initialDate != uiState.selectedDate) {
            runCatching { LocalDate.parse(initialDate) }.onSuccess {
                viewModel.selectDate(initialDate)
            }
        }
    }

    ActivityScreenContent(
        uiState = uiState,
        onBackClick = { navController.popBackStack() },
        onSelectDate = viewModel::selectDate,
        onAddWater = { viewModel.addWater(it) },
        onUpdateSteps = { viewModel.updateSteps(it) },
        onUpdateSleep = { viewModel.updateSleep(it) },
        onUpdateMood = { viewModel.updateMood(it) },
        onSaveNote = { viewModel.saveNote(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreenContent(
    uiState: ActivityUiState,
    onBackClick: () -> Unit,
    onSelectDate: (String) -> Unit,
    onAddWater: (Int) -> Unit,
    onUpdateSteps: (Int) -> Unit,
    onUpdateSleep: (Float) -> Unit,
    onUpdateMood: (String) -> Unit,
    onSaveNote: (String) -> Unit
) {
    val log = uiState.log
    val today = LocalDate.now().toString()
    val isToday = uiState.selectedDate == today

    val selectedDateDisplay = runCatching {
        val date = LocalDate.parse(uiState.selectedDate)
        if (isToday) "Hôm nay"
        else date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("vi")))
    }.getOrDefault(uiState.selectedDate)

    var stepsText by remember(log?.steps) { mutableStateOf((log?.steps ?: 0).toString()) }
    var noteText by remember(log?.note) { mutableStateOf(log?.note ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            ActivityPixelHeader(
                title = "Nhật ký hôm nay",
                subtitle = runCatching {
                    val date = LocalDate.parse(uiState.selectedDate)
                    date.format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", Locale("vi")))
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("vi")) else it.toString() }
                }.getOrDefault(selectedDateDisplay),
                onBackClick = onBackClick,
                onCalendarClick = { showDatePicker = true }
            )
        },
        containerColor = AppSurface
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(top = 20.dp, bottom = 38.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                PixelWaterCard(
                    waterMl = log?.waterMl ?: 0,
                    waterGoalMl = uiState.waterGoalMl,
                    onAddWater = onAddWater
                )
            }

            item {
                PixelStepsCard(
                    steps = log?.steps ?: 0,
                    stepGoal = uiState.stepGoal,
                    stepsText = stepsText,
                    onStepsTextChange = { stepsText = it.filter(Char::isDigit).take(6) },
                    onSaveSteps = { stepsText.toIntOrNull()?.let(onUpdateSteps) }
                )
            }

            item {
                PixelWellbeingCard(
                    sleepHours = log?.sleepHours ?: 0f,
                    mood = log?.mood,
                    onUpdateSleep = onUpdateSleep,
                    onUpdateMood = onUpdateMood
                )
            }

            item {
                PixelNoteField(
                    savedNote = log?.note.orEmpty(),
                    noteText = noteText,
                    onNoteTextChange = { noteText = it },
                    onSaveNote = { onSaveNote(noteText) }
                )
            }
        }
    }
    if (showDatePicker) {
        ActivityDatePickerDialog(
            selectedDate = uiState.selectedDate,
            onDismiss = { showDatePicker = false },
            onSelectDate = {
                onSelectDate(it)
                showDatePicker = false
            }
        )
    }
}

// ─── Private composables ──────────────────────────────────────────────────────

@Composable
private fun ActivityPixelHeader(
    title: String,
    subtitle: String,
    onBackClick: () -> Unit,
    onCalendarClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppSurface)
            .statusBarsPadding()
            .padding(start = 16.dp, end = 19.dp, top = 12.dp, bottom = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Mint100)
                .clickable(onClick = onBackClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = Mint700, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Ink900, fontSize = 23.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp)
            Text(subtitle, color = Ink700, fontSize = 14.sp, lineHeight = 17.sp)
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(AppSurface)
                .border(1.dp, AppLine, CircleShape)
                .clickable(onClick = onCalendarClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = "Chọn ngày", tint = Ink900, modifier = Modifier.size(23.dp))
        }
    }
}

@Composable
private fun PixelWaterCard(
    waterMl: Int,
    waterGoalMl: Int,
    onAddWater: (Int) -> Unit
) {
    val goal = waterGoalMl.coerceAtLeast(1)
    val progress = (waterMl.toFloat() / goal).coerceIn(0f, 1f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFBBD5DC))
            .padding(start = 21.dp, end = 21.dp, top = 23.dp, bottom = 22.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("♧", color = TrainColors.Forest, fontSize = 24.sp, modifier = Modifier.width(28.dp))
            Text("Nước uống", color = TrainColors.Forest, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(waterMl.toString(), color = TrainColors.Forest, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("/${goal}ml", color = Ink700, fontSize = 14.sp)
        }
        Spacer(Modifier.height(16.dp))
        Box(Modifier.fillMaxWidth().height(14.dp).clip(CircleShape).background(AppSurface)) {
            Box(Modifier.fillMaxWidth(progress).fillMaxHeight().clip(CircleShape).background(TrainColors.Forest))
        }
        Spacer(Modifier.height(17.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            PixelWaterButton("−250ml", primary = false, modifier = Modifier.weight(1f)) { onAddWater(-250) }
            PixelWaterButton("+250ml", primary = true, modifier = Modifier.weight(1f)) { onAddWater(250) }
        }
        Spacer(Modifier.height(11.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            PixelWaterButton("−500ml", primary = false, modifier = Modifier.weight(1f)) { onAddWater(-500) }
            PixelWaterButton("+500ml", primary = true, modifier = Modifier.weight(1f)) { onAddWater(500) }
        }
    }
}

@Composable
private fun PixelWaterButton(
    text: String,
    primary: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(49.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (primary) TrainColors.Forest else AppSurface)
            .border(1.dp, TrainColors.Forest, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (primary) AppSurface else TrainColors.Forest, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PixelStepsCard(
    steps: Int,
    stepGoal: Int,
    stepsText: String,
    onStepsTextChange: (String) -> Unit,
    onSaveSteps: () -> Unit
) {
    val goal = stepGoal.coerceAtLeast(1)
    val progress = (steps.toFloat() / goal).coerceIn(0f, 1f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppSurface)
            .border(1.dp, AppLine, RoundedCornerShape(14.dp))
            .padding(start = 21.dp, end = 21.dp, top = 23.dp, bottom = 20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("♚", color = TrainColors.Forest, fontSize = 23.sp, modifier = Modifier.width(28.dp))
            Text("Bước chân", color = Ink900, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("%,d".format(steps), color = TrainColors.Forest, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text("/%,d".format(goal), color = Ink700, fontSize = 14.sp)
        }
        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth().height(14.dp).clip(CircleShape).background(AppSurface2)) {
            Box(Modifier.fillMaxWidth(progress).fillMaxHeight().clip(CircleShape).background(TrainColors.Forest))
        }
        Spacer(Modifier.height(17.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(11.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = stepsText,
                onValueChange = onStepsTextChange,
                placeholder = { Text("Nhập số bước", color = Ink500, fontSize = 16.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f).height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppLine,
                    unfocusedBorderColor = AppLine,
                    focusedContainerColor = AppSurface,
                    unfocusedContainerColor = AppSurface
                )
            )
            Box(
                modifier = Modifier
                    .width(74.dp)
                    .height(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TrainColors.Forest)
                    .clickable(onClick = onSaveSteps),
                contentAlignment = Alignment.Center
            ) {
                Text("Lưu", color = AppSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PixelWellbeingCard(
    sleepHours: Float,
    mood: String?,
    onUpdateSleep: (Float) -> Unit,
    onUpdateMood: (String) -> Unit
) {
    var sleep by remember(sleepHours) { mutableFloatStateOf(sleepHours.takeIf { it > 0f } ?: 7.5f) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppSurface)
            .border(1.dp, AppLine, RoundedCornerShape(14.dp))
            .padding(start = 21.dp, end = 21.dp, top = 23.dp, bottom = 20.dp)
    ) {
        Text("☾  Giấc ngủ", color = Ink900, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(57.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Mint100),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SleepStepButton("−") {
                sleep = (sleep - 0.5f).coerceAtLeast(0f)
                onUpdateSleep(sleep)
            }
            Text(
                formatSleep(sleep),
                color = TrainColors.Forest,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Text("giờ", color = Ink700, fontSize = 14.sp, modifier = Modifier.padding(end = 18.dp))
            SleepStepButton("+") {
                sleep = (sleep + 0.5f).coerceAtMost(24f)
                onUpdateSleep(sleep)
            }
        }
        Spacer(Modifier.height(28.dp))
        HorizontalDivider(color = AppLine)
        Spacer(Modifier.height(23.dp))
        Text("🎭  Tâm trạng", color = Ink900, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            MOOD_OPTIONS.forEach { (key, emoji) ->
                val selected = mood == key
                Box(
                    modifier = Modifier
                        .size(65.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) Color(0xFFA9D7B3) else Mint100)
                        .border(
                            if (selected) 2.dp else 0.dp,
                            TrainColors.Forest,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { onUpdateMood(key) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 25.sp)
                }
            }
        }
    }
}

@Composable
private fun SleepStepButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AppSurface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = TrainColors.Forest, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PixelNoteField(
    savedNote: String,
    noteText: String,
    onNoteTextChange: (String) -> Unit,
    onSaveNote: () -> Unit
) {
    var editing by remember(savedNote) { mutableStateOf(savedNote.isBlank()) }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Ghi chú", color = Ink900, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (!editing && savedNote.isNotBlank()) {
                Text(
                    "Sửa",
                    color = TrainColors.Forest,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { editing = true }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        if (editing) {
            OutlinedTextField(
                value = noteText,
                onValueChange = onNoteTextChange,
                placeholder = { Text("Hôm nay cảm thấy thế nào?", color = Ink500, fontSize = 16.sp) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppLine,
                    unfocusedBorderColor = AppLine,
                    focusedContainerColor = AppSurface,
                    unfocusedContainerColor = AppSurface
                )
            )
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TrainColors.Forest)
                    .clickable {
                        onSaveNote()
                        editing = false
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("Lưu ghi chú", color = AppSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Mint100)
                    .border(1.dp, AppLine, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Text(savedNote, color = Ink900, fontSize = 15.sp, lineHeight = 21.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityDatePickerDialog(
    selectedDate: String,
    onDismiss: () -> Unit,
    onSelectDate: (String) -> Unit
) {
    val initialMillis = remember(selectedDate) {
        runCatching {
            LocalDate.parse(selectedDate)
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }.getOrNull()
    }
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = pickerState.selectedDateMillis
                    if (millis != null) {
                        val date = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                            .toString()
                        onSelectDate(date)
                    } else {
                        onDismiss()
                    }
                }
            ) { Text("Chọn", color = TrainColors.Forest) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy", color = Ink500) }
        }
    ) {
        DatePicker(state = pickerState)
    }
}

@Composable
private fun PixelWeekCard(
    selectedDate: String,
    weeklyLogs: List<ActivityLogDto>,
    waterGoalMl: Int,
    stepGoal: Int,
    onSelectDate: (String) -> Unit
) {
    val selected = runCatching { LocalDate.parse(selectedDate) }.getOrDefault(LocalDate.now())
    val days = (6 downTo 0).map { selected.minusDays(it.toLong()) }
    val logsByDate = weeklyLogs.associateBy { it.logDate }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(AppSurface)
            .border(1.dp, AppLine, RoundedCornerShape(14.dp))
            .padding(start = 21.dp, end = 21.dp, top = 17.dp, bottom = 17.dp)
    ) {
        Text("7 ngày gần đây", color = Ink900, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(15.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            days.forEach { day ->
                Text(
                    DAY_LABELS[day.dayOfWeek.value - 1],
                    modifier = Modifier.width(34.dp),
                    color = if (day == selected) Ink900 else Ink500,
                    fontSize = 12.sp,
                    fontWeight = if (day == selected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(9.dp))
        PixelGoalRow(days, logsByDate, Color(0xFFBBD5DC), onSelectDate) { (it?.waterMl ?: 0) >= waterGoalMl }
        Spacer(Modifier.height(8.dp))
        PixelGoalRow(days, logsByDate, Color(0xFFA9DDB5), onSelectDate) { (it?.steps ?: 0) >= stepGoal }
        Spacer(Modifier.height(17.dp))
        HorizontalDivider(color = AppLine)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            PixelLegend(Color(0xFFBBD5DC), "Nước")
            PixelLegend(Color(0xFFA9DDB5), "Bước chân")
        }
    }
}

@Composable
private fun PixelGoalRow(
    days: List<LocalDate>,
    logsByDate: Map<String, ActivityLogDto>,
    color: Color,
    onSelectDate: (String) -> Unit,
    achieved: (ActivityLogDto?) -> Boolean
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEach { day ->
            val date = day.toString()
            val ok = achieved(logsByDate[date])
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (ok) color else Mint100.copy(alpha = 0.5f))
                    .clickable { onSelectDate(date) },
                contentAlignment = Alignment.Center
            ) {
                Text(if (ok) "♢" else "−", color = if (ok) TrainColors.Forest else Ink400, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun PixelLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.size(13.dp).clip(CircleShape).background(color))
        Text(label, color = Ink700, fontSize = 12.sp)
    }
}

@Composable
private fun DateSelector(
    label: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    nextDisabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Ngày trước", tint = Ink700)
        }
        Text(
            label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = Ink900
        )
        IconButton(onClick = onNext, enabled = !nextDisabled) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Ngày sau",
                tint = if (nextDisabled) Ink400 else Ink700
            )
        }
    }
}

@Composable
private fun ActivityCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(VitalRadius.Lg),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppLine),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Ink700)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun WaterButton(
    label: String,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    val bg = if (primary) Mint500.copy(alpha = 0.12f) else AppSurface2
    val textColor = if (primary) Mint700 else Ink700
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(VitalRadius.Md))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textColor)
    }
}

@Composable
private fun MoodChip(emoji: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (selected) Mint500.copy(alpha = 0.18f) else Color.Transparent)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Mint500 else AppLine,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = 20.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun CalendarDayCell(
    dayLabel: String,
    dayNum: String,
    waterOk: Boolean,
    stepsOk: Boolean,
    sleepOk: Boolean,
    allOk: Boolean,
    isToday: Boolean,
    isSelected: Boolean
) {
    val bgColor = when {
        isSelected -> Mint500.copy(alpha = 0.2f)
        allOk -> Mint500.copy(alpha = 0.1f)
        else -> Color.Transparent
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(VitalRadius.Md))
            .background(bgColor)
            .padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Text(
            dayLabel,
            fontSize = 10.sp,
            color = if (isToday) Mint500 else Ink500,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            dayNum,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isToday) Mint500 else Ink900
        )
        // Achievement dots
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            AchievementDot(waterOk, MacroWater)
            AchievementDot(stepsOk, Mint500)
            AchievementDot(sleepOk, MacroCarbs)
        }
    }
}

@Composable
private fun AchievementDot(achieved: Boolean, color: Color) {
    Box(
        modifier = Modifier
            .size(5.dp)
            .clip(CircleShape)
            .background(if (achieved) color else AppLine)
    )
}

private fun formatSleep(hours: Float): String {
    val h = hours.toInt()
    val m = ((hours - h) * 60).toInt()
    return if (m == 0) "$h" else "$h:${m.toString().padStart(2, '0')}"
}
