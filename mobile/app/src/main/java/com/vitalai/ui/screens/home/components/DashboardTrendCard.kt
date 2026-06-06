package com.vitalai.ui.screens.home.components

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
fun DashboardTrendCard(uiState: HomeUiState) {
    val weekly = uiState.weeklyDashboard
    val monthly = uiState.monthlyDashboard
    if (weekly == null && monthly == null) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        shape = RoundedCornerShape(VitalRadius.Lg),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = BorderStroke(1.dp, AppLine)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Xu hướng", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink900)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                weekly?.let {
                    TrendMiniMetric(
                        label = "Tuần này",
                        value = "${it.nutrition.avgDailyCalories}",
                        sub = "kcal/ngày",
                        modifier = Modifier.weight(1f)
                    )
                    TrendMiniMetric(
                        label = "Bước TB",
                        value = "${it.activity.avgDailySteps}",
                        sub = "bước/ngày",
                        modifier = Modifier.weight(1f)
                    )
                }
                monthly?.let {
                    TrendMiniMetric(
                        label = "Tháng này",
                        value = "${it.training.workoutCount}",
                        sub = "buổi tập",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (uiState.weightTrend.size >= 2) {
                Spacer(Modifier.height(14.dp))
                Text("Cân nặng", color = Ink700, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                HomeWeightChart(
                    data = uiState.weightTrend,
                    modifier = Modifier.fillMaxWidth().height(118.dp)
                )
            }
        }
    }
}

@Composable
fun HomeWeightChart(data: List<BodyMetricDto>, modifier: Modifier = Modifier) {
    val points = remember(data) {
        data.mapNotNull { metric ->
            runCatching { LocalDate.parse(metric.date.take(10)) }.getOrNull()?.let { it to metric.weightKg }
        }.distinctBy { it.first }.sortedBy { it.first }
    }
    if (points.size < 2) return
    val minWeight = points.minOf { it.second }
    val maxWeight = points.maxOf { it.second }
    val range = (maxWeight - minWeight).coerceAtLeast(1f)
    val firstDate = points.first().first
    val lastDate = points.last().first
    val totalDays = java.time.temporal.ChronoUnit.DAYS.between(firstDate, lastDate).toFloat().coerceAtLeast(1f)

    Column(modifier = modifier) {
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val left = 10.dp.toPx()
            val right = 10.dp.toPx()
            val top = 8.dp.toPx()
            val bottom = 8.dp.toPx()
            val chartW = size.width - left - right
            val chartH = size.height - top - bottom
            val offsets = points.map { (date, weight) ->
                val dx = java.time.temporal.ChronoUnit.DAYS.between(firstDate, date).toFloat() / totalDays
                androidx.compose.ui.geometry.Offset(
                    x = left + dx * chartW,
                    y = top + (1f - (weight - minWeight) / range) * chartH
                )
            }
            val path = Path().apply {
                moveTo(offsets.first().x, offsets.first().y)
                offsets.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(path, color = Mint500, style = Stroke(width = 2.dp.toPx()))
            offsets.forEach {
                drawCircle(Mint500, radius = 3.5.dp.toPx(), center = it)
                drawCircle(Color.White, radius = 1.8.dp.toPx(), center = it)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(firstDate.format(DateTimeFormatter.ofPattern("dd/MM", Locale("vi"))), color = Ink500, fontSize = 10.sp)
            Text("%.1f → %.1f kg".format(points.first().second, points.last().second), color = Ink500, fontSize = 10.sp)
            Text(lastDate.format(DateTimeFormatter.ofPattern("dd/MM", Locale("vi"))), color = Ink500, fontSize = 10.sp)
        }
    }
}

@Composable
fun TrendMiniMetric(label: String, value: String, sub: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(VitalRadius.Md))
            .background(AppSurface2)
            .padding(12.dp)
    ) {
        Text(label, fontSize = 11.sp, color = Ink500)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink900)
        Text(sub, fontSize = 11.sp, color = Ink500)
    }
}