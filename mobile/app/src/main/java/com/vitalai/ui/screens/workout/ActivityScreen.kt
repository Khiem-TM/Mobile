package com.vitalai.ui.screens.workout

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vitalai.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class RingData(
    val label: String,
    val current: Float,
    val target: Float,
    val color: Color,
    val unit: String
)

data class ActivityLogEntry(
    val icon: String,
    val title: String,
    val subtitle: String,
    val time: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    navController: NavController,
    viewModel: ActivityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val log = uiState.log

    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("vi")))

    val rings = listOf(
        RingData("Move", log?.caloriesBurned ?: 320f, 500f, MacroProtein, "kcal"),
        RingData("Steps", (log?.steps ?: 6200).toFloat(), 10000f, Mint500, "bước"),
        RingData("Water", (log?.waterMl ?: 1200).toFloat(), 2000f, MacroWater, "ml"),
        RingData("Active", (log?.activeMins ?: 25).toFloat(), 60f, MacroCarbs, "phút")
    )

    // Mock hourly steps data
    val hourlySteps = remember {
        listOf(0, 0, 0, 0, 0, 0, 120, 450, 800, 650, 300, 900,
            400, 1100, 500, 700, 350, 420, 200, 300, 180, 90, 0, 0)
    }

    val activityLogs = remember {
        listOf(
            ActivityLogEntry("🏃", "Chạy bộ buổi sáng", "2.4 km • 22 phút", "06:30"),
            ActivityLogEntry("🚶", "Đi bộ đến công ty", "1.1 km • 15 phút", "08:15"),
            ActivityLogEntry("💧", "Uống nước", "+250ml", "10:00"),
            ActivityLogEntry("🏋️", "Tập gym", "45 phút • 280 kcal", "17:30"),
            ActivityLogEntry("💧", "Uống nước", "+500ml", "19:00")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Hoạt động hôm nay", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(today, fontSize = 12.sp, color = Ink500)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Settings, contentDescription = "Cài đặt", tint = Ink500)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppSurface)
            )
        },
        containerColor = AppBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Concentric rings
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = AppSurface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ConcentricRings(rings = rings, modifier = Modifier.size(160.dp))
                        Spacer(Modifier.width(20.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            rings.forEach { ring ->
                                RingLegendItem(ring = ring)
                            }
                        }
                    }
                }
            }

            // Hourly bar chart
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AppSurface),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Bước chân theo giờ",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Ink900
                        )
                        Spacer(Modifier.height(12.dp))
                        HourlyStepsChart(hourlyData = hourlySteps)
                    }
                }
            }

            // Quick actions
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionButton(
                        icon = { Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = Mint500, modifier = Modifier.size(18.dp)) },
                        label = "Cập nhật bước",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.updateSteps((log?.steps ?: 0) + 500) }
                    )
                    QuickActionButton(
                        icon = { Icon(Icons.Default.WaterDrop, contentDescription = null, tint = MacroWater, modifier = Modifier.size(18.dp)) },
                        label = "+250ml nước",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.updateWater((log?.waterMl ?: 0) + 250) }
                    )
                    QuickActionButton(
                        icon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MacroCarbs, modifier = Modifier.size(18.dp)) },
                        label = "Ghi chú",
                        modifier = Modifier.weight(1f),
                        onClick = {}
                    )
                }
            }

            // Activity log header
            item {
                Text(
                    "Nhật ký hoạt động",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Ink900,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Activity log items
            items(activityLogs.size) { idx ->
                val entry = activityLogs[idx]
                ActivityLogItem(entry = entry)
            }
        }
    }
}

@Composable
private fun ConcentricRings(rings: List<RingData>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val maxRadius = minOf(size.width, size.height) / 2f - 8f
        val strokeWidth = 14f
        val gap = 20f

        rings.forEachIndexed { index, ring ->
            val radius = maxRadius - index * (strokeWidth + gap)
            val progress = (ring.current / ring.target).coerceIn(0f, 1f)

            // Background track
            drawArc(
                color = ring.color.copy(alpha = 0.15f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Progress arc
            drawArc(
                color = ring.color,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun RingLegendItem(ring: RingData) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(ring.color)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(ring.label, fontSize = 11.sp, color = Ink500, fontWeight = FontWeight.Medium)
            Text(
                "${ring.current.toInt()}/${ring.target.toInt()} ${ring.unit}",
                fontSize = 12.sp,
                color = Ink900,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun HourlyStepsChart(hourlyData: List<Int>) {
    val maxVal = hourlyData.maxOrNull()?.toFloat() ?: 1f
    val threshold = maxVal * 0.7f

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        val barWidth = size.width / hourlyData.size
        val maxHeight = size.height - 16f

        hourlyData.forEachIndexed { index, steps ->
            val barHeight = if (maxVal > 0) (steps / maxVal) * maxHeight else 0f
            val color = if (steps >= threshold) Mint500 else Ink200
            val left = index * barWidth + barWidth * 0.15f
            val right = (index + 1) * barWidth - barWidth * 0.15f
            val top = size.height - barHeight
            val bottom = size.height

            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(right - left, barHeight.coerceAtLeast(2f)),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
            )
        }
    }

    // Hour labels
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        listOf("0h", "6h", "12h", "18h", "23h").forEach { label ->
            Text(label, fontSize = 10.sp, color = Ink500)
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: @Composable () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            icon()
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                fontSize = 10.sp,
                color = Ink700,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ActivityLogItem(entry: ActivityLogEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Mint50),
            contentAlignment = Alignment.Center
        ) {
            Text(entry.icon, fontSize = 18.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Ink900)
            Text(entry.subtitle, fontSize = 12.sp, color = Ink500)
        }
        Text(entry.time, fontSize = 11.sp, color = Ink500)
    }
}
