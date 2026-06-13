package com.vitalai.ui.screens.workout.screens

import com.vitalai.ui.screens.workout.components.*
import com.vitalai.ui.screens.workout.viewmodels.*

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vitalai.data.remote.model.ActivityLogDto
import com.vitalai.ui.components.VitalScreenHeader
import com.vitalai.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

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
            VitalScreenHeader(
                title = "Nhật ký hôm nay",
                subtitle = runCatching {
                    val date = LocalDate.parse(uiState.selectedDate)
                    date.format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", Locale("vi")))
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("vi")) else it.toString() }
                }.getOrDefault(selectedDateDisplay),
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Chọn ngày", tint = Ink900)
                    }
                }
            )
        },
        containerColor = AppSurface
    ) { padding ->
        if (uiState.isInitialLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Mint500)
            }
        } else {
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
                PixelSleepCard(
                    sleepHours = log?.sleepHours ?: 0f,
                    onUpdateSleep = onUpdateSleep
                )
                Spacer(Modifier.height(16.dp))
                PixelMoodAndNoteCard(
                    mood = log?.mood,
                    onUpdateMood = onUpdateMood,
                    savedNote = log?.note.orEmpty(),
                    noteText = noteText,
                    onNoteTextChange = { noteText = it },
                    onSaveNote = { onSaveNote(noteText) }
                )
            }
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
fun AnimatedWaterBackground(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave1"
    )
    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave2"
    )

    val color1 = Mint500.copy(alpha = 0.4f)
    val color2 = Mint500.copy(alpha = 0.8f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val waterHeight = height * progress
        val baseHeight = height - waterHeight
        
        val waveLength1 = width * 1.5f
        val waveLength2 = width * 1.2f
        val frequency1 = (2f * PI / waveLength1).toFloat()
        val frequency2 = (2f * PI / waveLength2).toFloat()
        
        val amplitude1 = 12.dp.toPx()
        val amplitude2 = 10.dp.toPx()

        clipRect {
            val path1 = Path()
            path1.moveTo(0f, height)
            path1.lineTo(0f, baseHeight)
            for (x in 0..width.toInt() step 5) {
                val y = baseHeight + sin((x * frequency1 + phase1).toDouble()).toFloat() * amplitude1
                path1.lineTo(x.toFloat(), y)
            }
            path1.lineTo(width, height)
            path1.close()
            drawPath(path1, color1)

            val path2 = Path()
            path2.moveTo(0f, height)
            path2.lineTo(0f, baseHeight)
            for (x in 0..width.toInt() step 5) {
                val y = baseHeight + sin((x * frequency2 - phase2).toDouble()).toFloat() * amplitude2
                path2.lineTo(x.toFloat(), y)
            }
            path2.lineTo(width, height)
            path2.close()
            drawPath(path2, color2)
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
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "waterProgress")

    val mainTextColor by animateColorAsState(if (animatedProgress > 0.45f) Color.White else Ink900, label = "mainText")
    val subTextColor by animateColorAsState(if (animatedProgress > 0.45f) Color.White.copy(alpha = 0.8f) else Ink500, label = "subText")
    
    val btnTextColor by animateColorAsState(if (animatedProgress > 0.15f) Color.White else Ink900, label = "btnText")
    val btnBgColor by animateColorAsState(if (animatedProgress > 0.15f) Color.White.copy(alpha = 0.3f) else AppSurface2, label = "btnBg")
    val btnBorderColor by animateColorAsState(if (animatedProgress > 0.15f) Color.White.copy(alpha = 0.5f) else AppLine, label = "btnBorder")

    val topTextColor by animateColorAsState(if (animatedProgress > 0.85f) Color.White else Ink500, label = "topText")
    val topBorderColor by animateColorAsState(if (animatedProgress > 0.85f) Color.White.copy(alpha = 0.5f) else Ink200, label = "topBorder")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(230.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedWaterBackground(
                progress = animatedProgress,
                modifier = Modifier.fillMaxSize()
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Tier 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Nước", color = if (animatedProgress > 0.85f) Color.White else Ink900, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(2.dp))
                        Text("Mục tiêu: %,d ml".format(goal), color = if (animatedProgress > 0.85f) Color.White.copy(alpha = 0.8f) else Ink500, fontSize = 13.sp)
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .border(1.dp, topBorderColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.LocalDrink,
                            contentDescription = "Water",
                            tint = topTextColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                // Tier 2
                Row(verticalAlignment = Alignment.Bottom) {
                    val litWater = String.format(java.util.Locale.US, "%.1f", waterMl / 1000f)
                    val litGoal = String.format(java.util.Locale.US, "%.1f", goal / 1000f)
                    Text(litWater, fontSize = 52.sp, fontWeight = FontWeight.ExtraBold, color = mainTextColor)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("/ ${litGoal}L", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = subTextColor, modifier = Modifier.padding(bottom = 8.dp))
                }
                
                // Tier 3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(btnBgColor)
                            .border(1.dp, btnBorderColor, RoundedCornerShape(12.dp))
                            .clickable { onAddWater(250) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+ 250ml", color = btnTextColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(btnBgColor)
                            .border(1.dp, btnBorderColor, RoundedCornerShape(12.dp))
                            .clickable { onAddWater(500) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+ 500ml", color = btnTextColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
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
    val remaining = (goal - steps).coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        // 1. Header & Progress
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Bước chân", color = Ink900, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text("Mục tiêu: %,d".format(goal), color = Ink500, fontSize = 13.sp)
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .border(1.dp, Ink200, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.DirectionsRun,
                    contentDescription = null,
                    tint = Ink500,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Box(Modifier.fillMaxWidth().height(12.dp).clip(CircleShape).background(AppSurface2)) {
            Box(Modifier.fillMaxWidth(progress).fillMaxHeight().clip(CircleShape).background(Mint500))
        }

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("%,d steps".format(steps), color = Ink900, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("${(progress * 100).toInt()}%", color = Ink700, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(24.dp))
        // 2. Action Area
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = stepsText,
                onValueChange = onStepsTextChange,
                placeholder = { Text("Nhập số bước...", color = Ink500, fontSize = 15.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppLine,
                    unfocusedBorderColor = AppLine,
                    focusedContainerColor = AppSurface2.copy(alpha = 0.5f),
                    unfocusedContainerColor = AppSurface2.copy(alpha = 0.5f)
                ),
                textStyle = LocalTextStyle.current.copy(fontSize = 15.sp)
            )
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Mint500)
                    .clickable(onClick = onSaveSteps),
                contentAlignment = Alignment.Center
            ) {
                Text("Lưu", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(20.dp))

        // 3. Motivational Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AppSurface2)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val bannerText = if (remaining > 0) {
                "Bạn còn cách mục tiêu %,d bước nữa!".format(remaining)
            } else {
                "Tuyệt vời! Bạn đã hoàn thành mục tiêu."
            }
            Text(bannerText, color = Ink700, fontSize = 13.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun PixelSleepCard(
    sleepHours: Float,
    onUpdateSleep: (Float) -> Unit
) {
    var sleep by remember(sleepHours) { mutableFloatStateOf(sleepHours.takeIf { it > 0f } ?: 7.5f) }
    val h = sleep.toInt()
    val m = ((sleep - h) * 60f).toInt()
    val deepSleep = sleep * 0.2f
    val dh = deepSleep.toInt()
    val dm = ((deepSleep - dh) * 60f).toInt()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = Mint100,
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text("Giấc ngủ", color = Ink900, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text("Thời gian tối ưu: 8g 30p", color = Ink500, fontSize = 13.sp)
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Mint500.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.NightsStay,
                        contentDescription = null,
                        tint = Mint500,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Mint500.copy(alpha = 0.15f))
                        .clickable { 
                            sleep = (sleep - 0.5f).coerceAtLeast(0f)
                            onUpdateSleep(sleep)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("−", color = Mint500, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$h",
                        color = Mint500,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.alignByBaseline()
                    )
                    Text(
                        text = "g",
                        color = Ink500,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.alignByBaseline().padding(start = 2.dp, end = 8.dp)
                    )
                    Text(
                        text = m.toString().padStart(2, '0'),
                        color = Mint500,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.alignByBaseline()
                    )
                    Text(
                        text = "p",
                        color = Ink500,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.alignByBaseline().padding(start = 2.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Mint500.copy(alpha = 0.15f))
                        .clickable { 
                            sleep = (sleep + 0.5f).coerceAtMost(24f)
                            onUpdateSleep(sleep)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", color = Mint500, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(Modifier.height(18.dp))
            
            Text("Ngủ sâu: ${dh}g ${dm.toString().padStart(2, '0')}p", color = Ink700, fontSize = 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun PixelMoodAndNoteCard(
    mood: String?,
    onUpdateMood: (String) -> Unit,
    savedNote: String,
    noteText: String,
    onNoteTextChange: (String) -> Unit,
    onSaveNote: () -> Unit
) {
    var editing by remember(savedNote) { mutableStateOf(savedNote.isBlank()) }
    val moodData = listOf(
        "happy" to Pair("😊", "Vui vẻ"),
        "neutral" to Pair("😐", "Ổn"),
        "sad" to Pair("😔", "Buồn"),
        "angry" to Pair("😤", "Bực bội"),
        "energetic" to Pair("💪", "Khỏe")
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tâm trạng", color = Ink900, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .border(1.dp, Ink200, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Face,
                        contentDescription = "Mood",
                        tint = Ink500,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                moodData.forEach { (key, data) ->
                    val (emoji, label) = data
                    val selected = mood == key
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) Mint100 else Color.Transparent)
                            .clickable { onUpdateMood(key) }
                            .padding(vertical = 10.dp)
                    ) {
                        Text(emoji, fontSize = 28.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = label,
                            color = if (selected) Mint500 else Ink500,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Ghi chú", color = Ink900, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (!editing && savedNote.isNotBlank()) {
                    Text(
                        "Sửa",
                        color = Mint500,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { editing = true }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            
            if (editing) {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = onNoteTextChange,
                    placeholder = { Text("Viết một ghi chú nhanh về ngày của bạn...", color = Ink500, fontSize = 15.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppLine,
                        unfocusedBorderColor = AppLine,
                        focusedContainerColor = AppSurface2.copy(alpha = 0.5f),
                        unfocusedContainerColor = AppSurface2.copy(alpha = 0.5f)
                    ),
                    textStyle = LocalTextStyle.current.copy(fontSize = 15.sp)
                )
                
                Spacer(Modifier.height(16.dp))
                
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(Mint500)
                            .clickable {
                                onSaveNote()
                                editing = false
                            }
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text("Lưu ghi chú", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppSurface2.copy(alpha = 0.5f))
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
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val selectedDate = java.time.Instant.ofEpochMilli(utcTimeMillis)
                    .atZone(java.time.ZoneId.of("UTC"))
                    .toLocalDate()
                val today = LocalDate.now()
                return !selectedDate.isAfter(today)
            }
        }
    )
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
            ) { Text("Chọn", color = Mint500) }
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
                Text(if (ok) "♢" else "−", color = if (ok) Mint500 else Ink400, fontSize = 18.sp)
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
