package com.vitalai.ui.screens.workout

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
    viewModel: ActivityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    ActivityScreenContent(
        uiState = uiState,
        onBackClick = { navController.popBackStack() },
        onPrevDay = {
            val prev = LocalDate.parse(uiState.selectedDate).minusDays(1).toString()
            viewModel.selectDate(prev)
        },
        onNextDay = {
            val next = LocalDate.parse(uiState.selectedDate).plusDays(1)
            if (!next.isAfter(LocalDate.now())) viewModel.selectDate(next.toString())
        },
        onAddWater = { viewModel.addWater(it) },
        onUpdateSteps = { viewModel.updateSteps(it) },
        onUpdateSleep = { viewModel.updateSleep(it) },
        onUpdateMood = { viewModel.updateMood(it) },
        onNoteChange = { viewModel.scheduleNoteUpdate(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreenContent(
    uiState: ActivityUiState,
    onBackClick: () -> Unit,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onAddWater: (Int) -> Unit,
    onUpdateSteps: (Int) -> Unit,
    onUpdateSleep: (Float) -> Unit,
    onUpdateMood: (String) -> Unit,
    onNoteChange: (String) -> Unit
) {
    val log = uiState.log
    val today = LocalDate.now().toString()
    val isToday = uiState.selectedDate == today
    val isNextDisabled = isToday

    val selectedDateDisplay = runCatching {
        val date = LocalDate.parse(uiState.selectedDate)
        if (isToday) "Hôm nay"
        else date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("vi")))
    }.getOrDefault(uiState.selectedDate)

    var stepsText by remember(log?.steps) { mutableStateOf((log?.steps ?: 0).toString()) }
    var noteText by remember(log?.note) { mutableStateOf(log?.note ?: "") }
    var showStepsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nhật ký hoạt động", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppSurface)
            )
        },
        containerColor = AppMutedBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Date selector
            item {
                DateSelector(
                    label = selectedDateDisplay,
                    onPrev = onPrevDay,
                    onNext = onNextDay,
                    nextDisabled = isNextDisabled
                )
            }

            // Water card
            item {
                ActivityCard(title = "💧 Nước uống") {
                    val waterMl = log?.waterMl ?: 0
                    val goal = uiState.waterGoalMl
                    val progress = (waterMl.toFloat() / goal).coerceIn(0f, 1f)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "$waterMl ml",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Ink900
                        )
                        Text(" / $goal ml", fontSize = 14.sp, color = Ink500, modifier = Modifier.padding(top = 4.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(99.dp)),
                        color = MacroWater,
                        trackColor = MacroWater.copy(alpha = 0.15f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WaterButton("-500", Modifier.weight(1f)) { onAddWater(-500) }
                        WaterButton("-250", Modifier.weight(1f)) { onAddWater(-250) }
                        WaterButton("+250", Modifier.weight(1f), primary = true) { onAddWater(250) }
                        WaterButton("+500", Modifier.weight(1f), primary = true) { onAddWater(500) }
                    }
                    if (progress >= 1f) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "🎉 Đã đạt mục tiêu uống nước hôm nay!",
                            fontSize = 13.sp,
                            color = MacroWater,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Steps card
            item {
                ActivityCard(title = "👣 Bước chân") {
                    val steps = log?.steps ?: 0
                    val goal = uiState.stepGoal
                    val progress = (steps.toFloat() / goal).coerceIn(0f, 1f)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("$steps", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink900)
                            Text("/ $goal bước", fontSize = 13.sp, color = Ink500)
                        }
                        TextButton(onClick = { showStepsDialog = true }) {
                            Text("Nhập tay", color = Mint500, fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(99.dp)),
                        color = Mint500,
                        trackColor = Mint500.copy(alpha = 0.15f)
                    )
                }
            }

            // Sleep card
            item {
                ActivityCard(title = "😴 Giấc ngủ") {
                    val sleepVal = log?.sleepHours ?: 0f
                    var sliderVal by remember(sleepVal) { mutableFloatStateOf(sleepVal) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            if (sliderVal == 0f) "Chưa ghi" else "${formatSleep(sliderVal)} giờ",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (sliderVal == 0f) Ink400 else Ink900
                        )
                        Text(
                            when {
                                sliderVal == 0f -> ""
                                sliderVal < 6f -> "😴 Thiếu ngủ"
                                sliderVal < 9f -> "✅ Đủ giấc"
                                else -> "😪 Quá nhiều"
                            },
                            fontSize = 13.sp,
                            color = when {
                                sliderVal < 6f && sliderVal > 0f -> Color(0xFFF87171)
                                sliderVal in 6f..9f -> Color(0xFF34D399)
                                else -> Ink500
                            }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = sliderVal,
                        onValueChange = { sliderVal = it },
                        onValueChangeFinished = { onUpdateSleep(sliderVal) },
                        valueRange = 0f..12f,
                        steps = 23,
                        colors = SliderDefaults.colors(
                            thumbColor = Mint500,
                            activeTrackColor = Mint500,
                            inactiveTrackColor = Mint500.copy(alpha = 0.15f)
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("0h", fontSize = 11.sp, color = Ink400)
                        Text("6h", fontSize = 11.sp, color = Ink400)
                        Text("12h", fontSize = 11.sp, color = Ink400)
                    }
                }
            }

            // Mood card
            item {
                ActivityCard(title = "🎭 Tâm trạng") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MOOD_OPTIONS.forEach { (key, emoji) ->
                            val selected = log?.mood == key
                            MoodChip(emoji = emoji, selected = selected, onClick = { onUpdateMood(key) })
                        }
                    }
                }
            }

            // Note card
            item {
                ActivityCard(title = "📝 Ghi chú") {
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = {
                            noteText = it
                            onNoteChange(it)
                        },
                        placeholder = { Text("Ghi lại cảm nhận hôm nay...", color = Ink400, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(VitalRadius.Md),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Mint500,
                            unfocusedBorderColor = AppLine
                        )
                    )
                }
            }

            // 7-day achievement calendar
            item {
                ActivityCard(title = "📅 Tuần này") {
                    val today7 = LocalDate.now()
                    val days = (6 downTo 0).map { today7.minusDays(it.toLong()) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        days.forEach { day ->
                            val logForDay = uiState.weeklyLogs.find { it.logDate == day.toString() }
                            val waterOk = (logForDay?.waterMl ?: 0) >= uiState.waterGoalMl
                            val stepsOk = (logForDay?.steps ?: 0) >= uiState.stepGoal
                            val sleepOk = (logForDay?.sleepHours ?: 0f) >= 6f
                            val allOk = waterOk && stepsOk
                            val isSelected = day.toString() == uiState.selectedDate

                            CalendarDayCell(
                                dayLabel = DAY_LABELS[day.dayOfWeek.value - 1],
                                dayNum = day.dayOfMonth.toString(),
                                waterOk = waterOk,
                                stepsOk = stepsOk,
                                sleepOk = sleepOk,
                                allOk = allOk,
                                isToday = day == today7,
                                isSelected = isSelected
                            )
                        }
                    }
                }
            }
        }
    }

    // Steps input dialog
    if (showStepsDialog) {
        var stepsInput by remember { mutableStateOf((log?.steps ?: 0).toString()) }
        AlertDialog(
            onDismissRequest = { showStepsDialog = false },
            title = { Text("Nhập số bước chân") },
            text = {
                OutlinedTextField(
                    value = stepsInput,
                    onValueChange = { stepsInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Số bước") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    stepsInput.toIntOrNull()?.let { onUpdateSteps(it) }
                    showStepsDialog = false
                }) { Text("Lưu", color = Mint500) }
            },
            dismissButton = {
                TextButton(onClick = { showStepsDialog = false }) { Text("Hủy") }
            }
        )
    }
}

// ─── Private composables ──────────────────────────────────────────────────────

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
