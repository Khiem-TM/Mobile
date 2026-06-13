package com.vitalai.ui.screens.workout.screens

import com.vitalai.ui.screens.workout.components.*
import com.vitalai.ui.screens.workout.viewmodels.*

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vitalai.data.remote.model.SessionExerciseDto
import com.vitalai.data.remote.model.WorkoutSessionDto
import com.vitalai.navigation.Screen
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun WorkoutSessionScreen(
    sessionId: String,
    date: String,
    navController: NavController,
    viewModel: WorkoutSessionViewModel = hiltViewModel()
) {
    LaunchedEffect(sessionId, date) { viewModel.load(sessionId, date) }
    val uiState by viewModel.uiState.collectAsState()

    TrainScreen {
        Column(Modifier.fillMaxSize()) {
            TrainHeader(
                title = uiState.session?.name ?: "Buổi tập",
                subtitle = formatSessionDate(uiState.session?.sessionDate ?: date),
                onBack = { navController.popBackStack() }
            )

            when {
                uiState.isInitialLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TrainColors.Forest)
                }
                uiState.session == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        uiState.error ?: "Không tìm thấy buổi tập",
                        color = TrainColors.Charcoal,
                        fontSize = 14.sp
                    )
                }
                else -> SessionDetailContent(
                    session = uiState.session!!,
                    onExerciseClick = { exerciseId ->
                        if (exerciseId.isNotBlank()) navController.navigate(Screen.ExerciseDetail(exerciseId))
                    }
                )
            }
        }
    }
}

@Composable
private fun SessionDetailContent(
    session: WorkoutSessionDto,
    onExerciseClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TrainStatTile(
                    icon = Icons.Default.LocalFireDepartment,
                    value = session.totalCaloriesBurned.roundToInt().toString(),
                    label = "Kcal đã đốt",
                    tone = TrainTone.Mint,
                    modifier = Modifier.weight(1f)
                )
                TrainStatTile(
                    icon = Icons.Default.AccessTime,
                    value = session.totalDurationMinutes.toString(),
                    label = "Phút tập luyện",
                    tone = TrainTone.Kiss,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            TrainSectionTitle("Danh sách bài tập (${session.details.size})")
        }
        if (session.details.isEmpty()) {
            item {
                TrainCard(tone = TrainTone.Keylime, padding = PaddingValues(22.dp)) {
                    Text(
                        "Buổi tập này chưa có bài tập nào",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = TrainColors.Forest,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        } else {
            items(session.details) { item ->
                SessionExerciseRow(item, onClick = { onExerciseClick(item.exerciseId) })
            }
        }
    }
}

@Composable
private fun SessionExerciseRow(item: SessionExerciseDto, onClick: () -> Unit) {
    TrainCard(tone = TrainTone.Cream, padding = PaddingValues(14.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            item.exercise?.let { TrainExerciseThumb(it, size = 46.dp, radius = 12.dp) } ?: Box(
                modifier = Modifier.size(46.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = TrainColors.Forest)
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(
                        item.exerciseName.ifBlank { "Bài tập" },
                        color = TrainColors.Ink,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    TrainTypeBadge(item.exerciseType)
                }
                Text(
                    sessionItemLine(item),
                    color = TrainColors.Charcoal,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    item.caloriesBurned.roundToInt().toString(),
                    color = TrainColors.Forest,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("kcal", color = TrainColors.Charcoal, fontSize = 10.sp)
            }
        }
    }
}

private fun sessionItemLine(item: SessionExerciseDto): String = when (item.exerciseType.uppercase()) {
    "GYM" -> {
        val sets = item.sets ?: 0
        val reps = item.repsPerSet ?: 0
        val weight = item.weightKg ?: 0f
        val weightPart = if (weight > 0f) " × ${weight.trimZero()} kg" else ""
        "$sets sets × $reps reps$weightPart"
    }
    "CARDIO" -> {
        val dist = item.distanceKm ?: 0f
        val speed = item.avgSpeedKmh ?: 0f
        "${dist.trimZero()} km · ${speed.trimZero()} km/h · ${item.durationMinutes} phút"
    }
    else -> "${item.durationMinutes} phút · ${intensityLabel(item.intensityLevel)}"
}

private fun Float.trimZero(): String =
    if (this % 1f == 0f) this.toInt().toString() else this.toString()

private fun intensityLabel(value: String?): String = when (value?.uppercase()) {
    "LOW" -> "Nhẹ"
    "HIGH" -> "Cao"
    "MEDIUM" -> "Vừa"
    else -> "Vừa"
}

private fun formatSessionDate(raw: String): String {
    val date = runCatching { LocalDate.parse(raw) }.getOrNull() ?: return ""
    return date.format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", Locale("vi")))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("vi")) else it.toString() }
}
