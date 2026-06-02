package com.vitalai.ui.screens.workout.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitalai.data.remote.model.WorkoutSessionDto
import com.vitalai.ui.screens.workout.viewmodels.DayCalorieData
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun StreakBanner(streak: Int, longestStreak: Int) {
    TrainCard(tone = TrainTone.Mint, padding = PaddingValues(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(15.dp)) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(TrainColors.Forest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.LocalFireDepartment,
                    contentDescription = null,
                    tint = TrainColors.MintGlaze,
                    modifier = Modifier.size(27.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    "Chuỗi $streak ngày liên tiếp",
                    color = TrainColors.Forest,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 22.sp
                )
                Text(
                    "Kỷ lục của bạn: $longestStreak ngày",
                    color = TrainColors.Forest.copy(alpha = 0.75f),
                    fontSize = 12.5.sp
                )
            }
        }
    }
}

@Composable
internal fun TodaySummary(calories: Int, minutes: Int) {
    val today = LocalDate.now()
    val title = "Hôm nay · ${today.format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", Locale("vi")))}"
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("vi")) else it.toString() }
    Column {
        TrainSectionTitle(title, modifier = Modifier.padding(bottom = 11.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TrainStatTile(
                icon = Icons.Default.LocalFireDepartment,
                value = calories.toString(),
                label = "Kcal đã đốt",
                tone = TrainTone.Keylime,
                modifier = Modifier.weight(1f)
            )
            TrainStatTile(
                icon = Icons.Default.AccessTime,
                value = minutes.toString(),
                label = "Phút tập luyện",
                tone = TrainTone.Kiss,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
internal fun WeeklyCaloriesCard(
    data: List<DayCalorieData>,
    weekStart: LocalDate,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit
) {
    val total = data.sumOf { it.calories.toDouble() }.toInt()
    val activeDays = data.count { it.hasSession }
    val maxIndex = data.indices.maxByOrNull { data[it].calories } ?: -1
    val weekEnd = weekStart.plusDays(6)
    val currentWeekStart = LocalDate.now().minusDays((LocalDate.now().dayOfWeek.value - 1).toLong())
    val canGoNext = weekStart.isBefore(currentWeekStart)
    val weekLabel = "${weekStart.format(DateTimeFormatter.ofPattern("dd/MM"))} - ${weekEnd.format(DateTimeFormatter.ofPattern("dd/MM"))}"
    TrainCard(tone = TrainTone.Cream, padding = PaddingValues(18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Calories đốt · 7 ngày", color = TrainColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            CaloriesTotalPill(total = total)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 13.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TrainRoundIconButton(
                icon = Icons.Default.ChevronLeft,
                contentDescription = "Tuần trước",
                onClick = onPreviousWeek,
                background = TrainColors.Cream,
                tint = TrainColors.Forest,
                size = 34.dp
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(999.dp))
                    .background(TrainColors.KeylimeWash)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    weekLabel,
                    color = TrainColors.Forest,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            TrainRoundIconButton(
                icon = Icons.Default.ChevronRight,
                contentDescription = "Tuần sau",
                onClick = onNextWeek,
                enabled = canGoNext,
                background = TrainColors.Cream,
                tint = if (canGoNext) TrainColors.Forest else TrainColors.Charcoal.copy(alpha = 0.35f),
                size = 34.dp
            )
        }
        TrainBarChart(
            values = data.map { it.dayLabel to it.calories },
            highlightIndex = maxIndex,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "$activeDays/7 ngày có buổi tập trong tuần này",
            color = TrainColors.Charcoal.copy(alpha = 0.7f),
            fontSize = 11.5.sp,
            modifier = Modifier.padding(top = 14.dp)
        )
    }
}

@Composable
private fun CaloriesTotalPill(total: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(TrainColors.KeylimeWash)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "${String.format(Locale.US, "%,d", total)} kcal",
            color = TrainColors.Forest,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
internal fun QuickLinkCard(
    icon: ImageVector,
    label: String,
    tone: TrainTone,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TrainCard(
        modifier = modifier.heightIn(min = 92.dp),
        tone = tone,
        padding = PaddingValues(16.dp),
        onClick = onClick
    ) {
        Icon(icon, contentDescription = null, tint = TrainColors.Forest, modifier = Modifier.size(22.dp))
        Spacer(Modifier.height(10.dp))
        Text(
            label,
            color = TrainColors.Forest,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 17.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun RecentHistory(sessions: List<WorkoutSessionDto>, onSessionClick: (WorkoutSessionDto) -> Unit) {
    Column {
        TrainSectionTitle("Lịch sử gần đây", modifier = Modifier.padding(bottom = 11.dp))
        if (sessions.isEmpty()) {
            TrainCard(tone = TrainTone.Keylime, padding = PaddingValues(22.dp)) {
                Text(
                    "Chưa có buổi tập gần đây",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = TrainColors.Forest,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                sessions.forEach { session ->
                    RecentSessionRow(session = session, onClick = { onSessionClick(session) })
                }
            }
        }
    }
}

@Composable
private fun RecentSessionRow(session: WorkoutSessionDto, onClick: () -> Unit) {
    val date = runCatching { LocalDate.parse(session.sessionDate) }.getOrNull()
    val day = date?.dayOfMonth?.toString()?.padStart(2, '0') ?: "--"
    val month = date?.monthValue?.toString()?.padStart(2, '0') ?: "--"
    TrainCard(tone = TrainTone.Cream, padding = PaddingValues(15.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TrainColors.KeylimeWash),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(day, color = TrainColors.Forest, fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 15.sp)
                    Text(month, color = TrainColors.Forest.copy(alpha = 0.7f), fontSize = 9.sp)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    session.name ?: "Buổi tập",
                    color = TrainColors.Ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${session.details.size} bài tập · ${session.totalDurationMinutes} phút",
                    color = TrainColors.Charcoal,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    session.totalCaloriesBurned.toInt().toString(),
                    color = TrainColors.Forest,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("kcal", color = TrainColors.Charcoal, fontSize = 10.5.sp)
            }
        }
    }
}
