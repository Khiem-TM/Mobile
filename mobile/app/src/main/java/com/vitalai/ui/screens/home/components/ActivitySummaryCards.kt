package com.vitalai.ui.screens.home.components

import kotlin.math.roundToInt

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vitalai.data.remote.model.BodyMetricDto
import com.vitalai.navigation.Screen
import com.vitalai.ui.components.ArcGauge
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.theme.AppLine
import com.vitalai.ui.theme.AppSurface
import com.vitalai.ui.theme.AppSurface2
import com.vitalai.ui.theme.AmberContainer
import com.vitalai.ui.theme.AmberOnContainer
import com.vitalai.ui.theme.AmberTint
import com.vitalai.ui.theme.Ink400
import com.vitalai.ui.theme.Ink500
import com.vitalai.ui.theme.Ink700
import com.vitalai.ui.theme.Ink900
import com.vitalai.ui.theme.MacroCarbs
import com.vitalai.ui.theme.MacroFat
import com.vitalai.ui.theme.MacroProtein
import com.vitalai.ui.theme.MealTimeBg
import com.vitalai.ui.theme.MealTimeText
import com.vitalai.ui.theme.Mint100
import com.vitalai.ui.theme.Mint500
import com.vitalai.ui.theme.WaterBlue
import com.vitalai.ui.theme.WaterBlueTint
import com.vitalai.ui.theme.VitalRadius
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.vitalai.ui.screens.home.viewmodels.HomeUiState
import com.vitalai.ui.screens.home.viewmodels.HomeViewModel

@Composable
fun DailyActivitySummaryCard(
    uiState: HomeUiState,
    onOpenActivityLog: () -> Unit
) {
    val d = uiState.dashboard
    val waterMl = d?.waterMl ?: 0
    val waterGoalMl = (d?.waterGoalMl ?: 2500).coerceAtLeast(1)
    val waterProgress = (waterMl.toFloat() / waterGoalMl).coerceIn(0f, 1f)
    val burnedKcal = d?.caloriesBurned?.roundToInt() ?: 0
    val steps = d?.steps ?: 0
    val stepGoal = (d?.stepGoal ?: 10000).coerceAtLeast(1)
    val stepsProgress = (steps.toFloat() / stepGoal).coerceIn(0f, 1f)

    Column(modifier = Modifier.padding(top = 36.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Hoạt động hôm nay", color = Ink900, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clickable(onClick = onOpenActivityLog),
            shape = RoundedCornerShape(VitalRadius.Lg),
            colors = CardDefaults.cardColors(containerColor = AppSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ActivityMiniMetric("Calories", "$burnedKcal", "kcal", MacroCarbs, AmberTint, Modifier.weight(1f))
                ActivityMiniMetric("Bước chân", "%,d".format(steps), "/%,d".format(stepGoal), Mint500, Mint100, Modifier.weight(1f))
                ActivityMiniMetric("Nước", "${waterMl}ml", "/${waterGoalMl}ml", WaterBlue, WaterBlueTint, Modifier.weight(1f))
            }
            DualProgressRow("Bước chân", stepsProgress, Mint500, "Nước", waterProgress, WaterBlue)
        }
    }
    }
}

@Composable
fun ActivityMiniMetric(
    label: String,
    value: String,
    sub: String,
    color: Color,
    bg: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(VitalRadius.Md))
            .background(bg.copy(alpha = 0.7f))
            .padding(10.dp)
    ) {
        Text(label, color = Ink500, fontSize = 10.5.sp, maxLines = 1)
        Text(value, color = Ink900, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(sub, color = color, fontSize = 10.5.sp, maxLines = 1)
    }
}

@Composable
fun DualProgressRow(
    firstLabel: String,
    firstProgress: Float,
    firstColor: Color,
    secondLabel: String,
    secondProgress: Float,
    secondColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        listOf(firstLabel to (firstProgress to firstColor), secondLabel to (secondProgress to secondColor)).forEach { (label, pair) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(label, color = Ink500, fontSize = 11.sp, modifier = Modifier.width(64.dp))
                Box(modifier = Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(100)).background(AppSurface2)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(pair.first)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(100))
                            .background(pair.second)
                    )
                }
            }
        }
    }
}

@Composable
fun ActivityWeekSummaryCard(
    uiState: HomeUiState,
    onDateClick: (String) -> Unit
) {
    val selected = remember(uiState.selectedDate) {
        runCatching { java.time.LocalDate.parse(uiState.selectedDate) }.getOrDefault(java.time.LocalDate.now())
    }
    val days = remember(selected) { (6 downTo 0).map { selected.minusDays(it.toLong()) } }
    val waterGoal = (uiState.dashboard?.waterGoalMl ?: 2500).coerceAtLeast(1)
    val stepGoal = (uiState.dashboard?.stepGoal ?: 10000).coerceAtLeast(1)
    val logsByDate = remember(uiState.recentActivityLogs) {
        uiState.recentActivityLogs.associateBy { it.logDate }
    }
    val labels = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")

    Column(modifier = Modifier.padding(top = 36.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("7 ngày gần đây", color = Ink900, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(VitalRadius.Lg),
            colors = CardDefaults.cardColors(containerColor = AppSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                days.forEach { day ->
                    val dateStr = day.toString()
                    val log = logsByDate[dateStr]
                    val waterProgress = ((log?.waterMl ?: 0).toFloat() / waterGoal).coerceIn(0f, 1f)
                    val stepProgress = ((log?.steps ?: 0).toFloat() / stepGoal).coerceIn(0f, 1f)
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(VitalRadius.Md))
                            .clickable { onDateClick(dateStr) }
                            .padding(top = 8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Water bar
                            Box(
                                modifier = Modifier
                                    .width(10.dp)
                                    .fillMaxHeight(waterProgress.coerceAtLeast(0.04f))
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(WaterBlue)
                            )
                            // Step bar
                            Box(
                                modifier = Modifier
                                    .width(10.dp)
                                    .fillMaxHeight(stepProgress.coerceAtLeast(0.04f))
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(Mint500)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = labels[day.dayOfWeek.value - 1],
                            color = if (day == selected) Ink900 else Ink500,
                            fontSize = 12.sp,
                            fontWeight = if (day == selected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = AppLine)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
                LegendDot(WaterBlue, "Nước")
                LegendDot(Mint500, "Bước chân")
            }
        }
    }
    }
}

@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.size(13.dp).clip(CircleShape).background(color))
        Text(label, color = Ink700, fontSize = 12.sp)
    }
}