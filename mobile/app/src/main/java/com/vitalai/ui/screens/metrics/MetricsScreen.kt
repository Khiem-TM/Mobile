package com.vitalai.ui.screens.metrics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vitalai.data.remote.model.BodyMetricDto
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricsScreen(
    navController: NavController,
    viewModel: MetricsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Số liệu cơ thể", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppSurface)
            )
        },
        containerColor = AppBackground
    ) { padding ->
        when {
            uiState.isLoading -> LoadingState(modifier = Modifier.padding(padding))
            uiState.error != null && uiState.latest == null -> ErrorState(
                message = uiState.error!!,
                onRetry = viewModel::loadData,
                modifier = Modifier.padding(padding)
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Latest metrics summary
                uiState.latest?.let { latest ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricCard("Cân nặng", "${latest.weightKg} kg", Mint500, Modifier.weight(1f))
                        latest.bmi?.let { bmi ->
                            MetricCard("BMI", "%.1f".format(bmi), bmiColor(bmi), Modifier.weight(1f))
                        }
                        latest.bodyFatPct?.let { fat ->
                            MetricCard("Mỡ cơ thể", "%.1f%%".format(fat), MacroFat, Modifier.weight(1f))
                        }
                    }
                }

                // Period selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("1W" to "1 tuần", "1M" to "1 tháng", "3M" to "3 tháng").forEach { (key, label) ->
                        FilterChip(
                            selected = uiState.selectedPeriod == key,
                            onClick = { viewModel.selectPeriod(key) },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Mint500,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Weight chart
                uiState.periodData?.let { period ->
                    if (period.data.isNotEmpty()) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = AppSurface),
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Biểu đồ cân nặng", fontWeight = FontWeight.Bold, color = Ink900)
                                    Text(
                                        "TB: ${"%.1f".format(period.avgWeight)} kg",
                                        fontSize = 12.sp,
                                        color = Ink500
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                WeightLineChart(
                                    data = period.data,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                )
                            }
                        }
                    }
                }

                // BMI info card
                uiState.latest?.bmi?.let { bmi ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = AppSurface),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Chỉ số BMI", fontWeight = FontWeight.Bold, color = Ink900)
                                Text(bmiLabel(bmi), fontSize = 13.sp, color = bmiColor(bmi))
                            }
                            Text(
                                "%.1f".format(bmi),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = bmiColor(bmi)
                            )
                        }
                    }
                }

                if (uiState.latest == null && uiState.periodData == null) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📊", fontSize = 40.sp)
                            Spacer(Modifier.height(8.dp))
                            Text("Chưa có dữ liệu số liệu cơ thể", color = Ink500, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
            Text(label, fontSize = 11.sp, color = color.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun WeightLineChart(data: List<BodyMetricDto>, modifier: Modifier = Modifier) {
    if (data.size < 2) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Cần ít nhất 2 điểm dữ liệu", color = Ink500, fontSize = 12.sp)
        }
        return
    }
    val weights = data.map { it.weightKg }
    val minW = weights.min()
    val maxW = weights.max()
    val range = (maxW - minW).coerceAtLeast(1f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val padding = 16.dp.toPx()
        val chartW = w - padding * 2
        val chartH = h - padding * 2

        val points = data.mapIndexed { i, d ->
            val x = padding + i.toFloat() / (data.size - 1) * chartW
            val y = padding + (1f - (d.weightKg - minW) / range) * chartH
            Offset(x, y)
        }

        // Fill area under line
        val fillPath = Path().apply {
            moveTo(points.first().x, h)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, h)
            close()
        }
        drawPath(fillPath, color = Mint500.copy(alpha = 0.15f))

        // Line
        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(linePath, color = Mint500, style = Stroke(width = 2.dp.toPx()))

        // Points
        points.forEach { pt ->
            drawCircle(color = Mint500, radius = 4.dp.toPx(), center = pt)
            drawCircle(color = Color.White, radius = 2.dp.toPx(), center = pt)
        }
    }
}

private fun bmiColor(bmi: Float) = when {
    bmi < 18.5f -> MacroWater
    bmi < 25f -> Mint500
    bmi < 30f -> MacroCarbs
    else -> MacroProtein
}

private fun bmiLabel(bmi: Float) = when {
    bmi < 18.5f -> "Thiếu cân"
    bmi < 25f -> "Bình thường"
    bmi < 30f -> "Thừa cân"
    else -> "Béo phì"
}
