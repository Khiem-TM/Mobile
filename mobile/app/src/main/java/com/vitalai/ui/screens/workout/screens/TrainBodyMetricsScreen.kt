package com.vitalai.ui.screens.workout.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vitalai.data.remote.model.BodyMetricDto
import com.vitalai.data.remote.model.BodyMetricsSummaryDto
import com.vitalai.data.remote.model.ProgressPhotoDto
import com.vitalai.navigation.Screen
import com.vitalai.ui.screens.workout.components.TrainCard
import com.vitalai.ui.screens.workout.components.TrainChip
import com.vitalai.ui.screens.workout.components.TrainColors
import com.vitalai.ui.screens.workout.components.TrainHeader
import com.vitalai.ui.screens.workout.components.TrainPrimaryButton
import com.vitalai.ui.screens.workout.components.TrainScreen
import com.vitalai.ui.screens.workout.components.TrainSectionTitle
import com.vitalai.ui.screens.workout.components.TrainSegmented
import com.vitalai.ui.screens.workout.components.TrainTone
import com.vitalai.ui.screens.workout.viewmodels.TrainBodyMetricsUiState
import com.vitalai.ui.screens.workout.viewmodels.TrainBodyMetricsViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
fun TrainBodyMetricsScreen(
    navController: NavController,
    viewModel: TrainBodyMetricsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    TrainBodyMetricsContent(
        uiState = uiState,
        onBack = { navController.popBackStack() },
        onUpdate = { navController.navigate(Screen.Metrics) },
        onHistory = { navController.navigate(Screen.MetricsHistory) },
        onSelectPeriod = viewModel::selectPeriod
    )
}

@Composable
private fun TrainBodyMetricsContent(
    uiState: TrainBodyMetricsUiState,
    onBack: () -> Unit,
    onUpdate: () -> Unit,
    onHistory: () -> Unit,
    onSelectPeriod: (String) -> Unit
) {
    TrainScreen {
        Column(Modifier.fillMaxSize()) {
            com.vitalai.ui.components.VitalScreenHeader(
                title = "Chỉ số cơ thể",
                onBackClick = onBack,
                actions = {
                    TrainBodyUpdateButton(onClick = onUpdate)
                    androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(14.dp))
                }
            )

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TrainColors.Forest)
                }
                uiState.latest == null -> EmptyBodyMetricsState(
                    message = uiState.error ?: "Chưa có dữ liệu chỉ số cơ thể",
                    onUpdate = onUpdate
                )
                else -> BodyMetricsDashboard(
                    uiState = uiState,
                    onHistory = onHistory,
                    onSelectPeriod = onSelectPeriod,
                    onUpdate = onUpdate
                )
            }
        }
    }
}

@Composable
private fun BodyMetricsDashboard(
    uiState: TrainBodyMetricsUiState,
    onHistory: () -> Unit,
    onSelectPeriod: (String) -> Unit,
    onUpdate: () -> Unit
) {
    val latest = uiState.latest ?: return
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { LatestBodyMetrics(latest) }
        item { WeightChangeCard(summary = uiState.summary) }
        item {
            WeightChartCard(
                selectedPeriod = uiState.selectedPeriod,
                data = uiState.periodData?.data ?: emptyList(),
                onSelectPeriod = onSelectPeriod,
                onHistory = onHistory
            )
        }
        item { BodyMeasurements(latest) }
        item { ProgressPhotos(photos = uiState.photos, onUpdate = onUpdate) }
    }
}

@Composable
private fun TrainBodyUpdateButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(TrainColors.Forest)
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = TrainColors.Cream, modifier = Modifier.size(16.dp))
        Text("Cập nhật", color = TrainColors.Cream, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LatestBodyMetrics(metric: BodyMetricDto) {
    Column {
        TrainSectionTitle(
            "Chỉ số mới nhất · ${formatMetricDate(metric.date)}",
            modifier = Modifier.padding(bottom = 11.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricValueCard(
                value = formatNumber(metric.weightKg),
                label = "Cân nặng (kg)",
                tone = TrainTone.Mint,
                modifier = Modifier.weight(1f)
            )
            MetricValueCard(
                value = metric.bmi?.let(::formatNumber) ?: "--",
                label = "BMI · ${bmiLabel(metric.bmi)}",
                valueColor = bmiColor(metric.bmi),
                tone = TrainTone.Cream,
                modifier = Modifier.weight(1f)
            )
            MetricValueCard(
                value = metric.heightCm?.let(::formatNumber) ?: "--",
                label = "Chiều cao (cm)",
                tone = TrainTone.Kiss,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(10.dp))
        BmiGauge(metric.bmi)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            EnergyMetricCard("BMR", metric.bmr, Modifier.weight(1f))
            EnergyMetricCard("TDEE", metric.tdee, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MetricValueCard(
    value: String,
    label: String,
    tone: TrainTone,
    modifier: Modifier = Modifier,
    valueColor: Color = TrainColors.Forest
) {
    TrainCard(
        modifier = modifier,
        tone = tone,
        padding = PaddingValues(horizontal = 8.dp, vertical = 14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().height(52.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                color = valueColor,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Text(
                label,
                modifier = Modifier.padding(top = 6.dp),
                color = TrainColors.Charcoal,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BmiGauge(bmi: Float?) {
    TrainCard(tone = TrainTone.Cream, padding = PaddingValues(14.dp)) {
        val value = bmi ?: 0f
        val percent = ((value - 15f) / 25f).coerceIn(0f, 1f)
        BoxWithConstraints(Modifier.fillMaxWidth().height(18.dp)) {
            val markerOffset = (maxWidth - 6.dp) * percent
            Box(Modifier.align(Alignment.Center).fillMaxWidth().height(12.dp).clip(CircleShape)) {
                Row(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(3.5f).fillMaxHeight().background(Color(0xFF8EC5EE)))
                    Box(Modifier.weight(6.5f).fillMaxHeight().background(TrainColors.MintGlaze))
                    Box(Modifier.weight(5f).fillMaxHeight().background(Color(0xFFEAC36D)))
                    Box(Modifier.weight(10f).fillMaxHeight().background(Color(0xFFE78378)))
                }
            }
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = markerOffset)
                    .width(5.dp)
                    .height(18.dp)
                    .clip(CircleShape)
                    .background(TrainColors.Forest)
                    .border(2.dp, TrainColors.Cream, CircleShape)
            )
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(14.dp)
        ) {
            BmiAxisLabel("15", Modifier.align(Alignment.CenterStart).width(28.dp), TextAlign.Start)
            BmiAxisLabelAt("18.5", 18.5f, maxWidth)
            BmiAxisLabelAt("25", 25f, maxWidth)
            BmiAxisLabelAt("30", 30f, maxWidth)
            BmiAxisLabel("40", Modifier.align(Alignment.CenterEnd).width(28.dp), TextAlign.End)
        }
    }
}

@Composable
private fun BmiAxisLabel(text: String, modifier: Modifier, align: TextAlign) {
    Text(
        text,
        modifier = modifier,
        color = TrainColors.Charcoal.copy(alpha = 0.7f),
        fontSize = 10.sp,
        textAlign = align,
        maxLines = 1
    )
}

@Composable
private fun BmiAxisLabelAt(text: String, value: Float, trackWidth: androidx.compose.ui.unit.Dp) {
    val labelWidth = 34.dp
    val percent = ((value - 15f) / 25f).coerceIn(0f, 1f)
    val x = (trackWidth * percent - labelWidth / 2).coerceIn(0.dp, trackWidth - labelWidth)
    BmiAxisLabel(
        text = text,
        modifier = Modifier.offset(x = x).width(labelWidth),
        align = TextAlign.Center
    )
}

@Composable
private fun EnergyMetricCard(label: String, value: Float?, modifier: Modifier = Modifier) {
    TrainCard(modifier = modifier, tone = TrainTone.Keylime, padding = PaddingValues(15.dp)) {
        Text(label, color = TrainColors.Charcoal, fontSize = 11.5.sp)
        Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.Bottom) {
            Text(
                value?.toInt()?.toString() ?: "--",
                color = TrainColors.Forest,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )
            Text(" kcal/ngày", color = TrainColors.Forest, fontSize = 12.sp)
        }
    }
}

@Composable
private fun WeightChangeCard(summary: BodyMetricsSummaryDto?) {
    TrainCard(tone = TrainTone.Cream, padding = PaddingValues(16.dp)) {
        Text("Thay đổi cân nặng", color = TrainColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ChangeMetric(delta = summary?.weightChange ?: 0f, since = summary?.startDate?.let { "từ ${formatMetricDateShort(it)}" } ?: "từ lần đầu", modifier = Modifier.weight(1f))
            Box(Modifier.width(1.dp).height(48.dp).background(TrainColors.Border))
            ChangeMetric(delta = summary?.weightChange ?: 0f, since = "trong 30 ngày", modifier = Modifier.weight(1f))
        }
        Box(Modifier.fillMaxWidth().padding(top = 12.dp).height(1.dp).background(TrainColors.Border.copy(alpha = 0.72f)))
        Text(
            "${formatNumber(summary?.startWeight)} kg (${summary?.startDate?.let(::formatMetricDateShort) ?: "--"}) -> ${formatNumber(summary?.currentWeight)} kg · ${summary?.totalRecords ?: 0} lần đo",
            color = TrainColors.Charcoal.copy(alpha = 0.72f),
            fontSize = 11.5.sp,
            modifier = Modifier.padding(top = 11.dp)
        )
    }
}

@Composable
private fun ChangeMetric(delta: Float, since: String, modifier: Modifier = Modifier) {
    val down = delta < 0
    val color = if (down) Color(0xFF11A86A) else TrainColors.Sport
    Row(modifier = modifier, verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(
            imageVector = if (down) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text("${formatNumber(abs(delta))} kg", color = TrainColors.Forest, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(since, color = TrainColors.Charcoal, fontSize = 12.sp)
        }
    }
}

@Composable
private fun WeightChartCard(
    selectedPeriod: String,
    data: List<BodyMetricDto>,
    onSelectPeriod: (String) -> Unit,
    onHistory: () -> Unit
) {
    TrainCard(tone = TrainTone.Cream, padding = PaddingValues(16.dp)) {
        Text("Biểu đồ cân nặng", color = TrainColors.Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        TrainSegmented(
            options = listOf("week" to "Tuần", "month" to "Tháng", "3months" to "3 tháng"),
            selected = selectedPeriod,
            onSelect = onSelectPeriod,
            small = true,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        )
        WeightLineChart(data = data, modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(TrainColors.Border.copy(alpha = 0.72f)))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 0.dp)
                .clickable(onClick = onHistory)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = TrainColors.Forest, modifier = Modifier.size(16.dp))
                Text(
                    "Xem lịch sử đầy đủ",
                    color = TrainColors.Forest,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                )
            }
        }
    }
}

@Composable
private fun WeightLineChart(data: List<BodyMetricDto>, modifier: Modifier = Modifier) {
    val points = data.sortedBy { it.date }.takeLast(5)
    val weights = points.map { it.weightKg }
    val min = weights.minOrNull() ?: 0f
    val max = weights.maxOrNull() ?: 1f
    val range = (max - min).takeIf { it > 0.1f } ?: 1f

    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(156.dp)) {
            if (points.isEmpty()) {
                drawLine(
                    color = TrainColors.Border,
                    start = Offset(0f, size.height * 0.65f),
                    end = Offset(size.width, size.height * 0.65f),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                return@Canvas
            }
            val coordinates = points.mapIndexed { index, point ->
                val x = if (points.size == 1) size.width / 2 else size.width * index / (points.size - 1)
                val y = size.height - ((point.weightKg - min) / range) * (size.height * 0.58f) - size.height * 0.16f
                Offset(x, y)
            }
            val area = Path().apply {
                moveTo(coordinates.first().x, size.height)
                coordinates.forEach { lineTo(it.x, it.y) }
                lineTo(coordinates.last().x, size.height)
                close()
            }
            drawPath(area, color = TrainColors.KeylimeWash.copy(alpha = 0.55f))
            coordinates.zipWithNext().forEach { (start, end) ->
                drawLine(
                    color = TrainColors.Forest,
                    start = start,
                    end = end,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            coordinates.forEach {
                drawCircle(color = TrainColors.Cream, radius = 5.dp.toPx(), center = it)
                drawCircle(color = TrainColors.Forest, radius = 3.dp.toPx(), center = it)
            }
        }
        if (points.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                points.forEach { point ->
                    Text(formatMetricDateDayMonth(point.date), color = TrainColors.Charcoal.copy(alpha = 0.72f), fontSize = 10.5.sp)
                }
            }
        }
    }
}

@Composable
private fun BodyMeasurements(metric: BodyMetricDto) {
    val measures = listOf(
        Measure("Vòng eo", metric.waistCm?.let { "${formatNumber(it)} cm" } ?: "--", Icons.Default.Straighten),
        Measure("Vòng mông", metric.hipCm?.let { "${formatNumber(it)} cm" } ?: "--", Icons.Default.Straighten),
        Measure("Vòng ngực", metric.chestCm?.let { "${formatNumber(it)} cm" } ?: "--", Icons.Default.Straighten),
        Measure("Bắp tay", metric.armCm?.let { "${formatNumber(it)} cm" } ?: "--", Icons.Default.Straighten),
        Measure("Vòng cổ", metric.neckCm?.let { "${formatNumber(it)} cm" } ?: "--", Icons.Default.Straighten),
        Measure("Mỡ cơ thể", metric.bodyFatPct?.let { "${formatNumber(it)}%" } ?: "--", Icons.Default.AccessibilityNew)
    )
    Column {
        TrainSectionTitle("Số đo cơ thể", modifier = Modifier.padding(bottom = 11.dp))
        measures.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { measure ->
                    MeasureCard(measure = measure, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MeasureCard(measure: Measure, modifier: Modifier = Modifier) {
    TrainCard(modifier = modifier, tone = TrainTone.Cream, padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(TrainColors.KeylimeWash),
                contentAlignment = Alignment.Center
            ) {
                Icon(measure.icon, contentDescription = null, tint = TrainColors.Forest, modifier = Modifier.size(18.dp))
            }
            Column {
                Text(measure.value, color = TrainColors.Forest, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(measure.label, color = TrainColors.Charcoal, fontSize = 11.5.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ProgressPhotos(photos: List<ProgressPhotoDto>, onUpdate: () -> Unit) {
    var photoMode by remember { mutableStateOf("single") }
    val front = photos.firstOrNull { it.photoType.equals("front", ignoreCase = true) } ?: photos.firstOrNull()
    val side = photos.firstOrNull { it.photoType.equals("side", ignoreCase = true) }
    val back = photos.firstOrNull { it.photoType.equals("back", ignoreCase = true) }
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TrainSectionTitle("Ảnh tiến trình")
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                TrainChip(text = "1 ảnh", selected = photoMode == "single", onClick = { photoMode = "single" })
                TrainChip(text = "3 ảnh", selected = photoMode == "triple", onClick = { photoMode = "triple" })
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
        ) {
            AnimatedContent(
                modifier = Modifier.fillMaxSize(),
                targetState = photoMode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "progress_photo_mode"
            ) { mode ->
                if (mode == "single") {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        PhotoSlot(
                            label = "Ảnh mới nhất",
                            takenAt = front?.takenAt,
                            modifier = Modifier.weight(0.86f).fillMaxHeight(),
                            onClick = onUpdate
                        )
                        TrainCard(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            tone = TrainTone.Cream,
                            padding = PaddingValues(16.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = TrainColors.Forest, modifier = Modifier.size(20.dp))
                            Text(
                                "Ảnh được lưu kèm chỉ số khi bạn cập nhật. Chuyển sang 3 ảnh để chụp đủ 3 góc.",
                                color = TrainColors.Charcoal,
                                fontSize = 12.5.sp,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        PhotoSlot(label = "Trước", takenAt = front?.takenAt, modifier = Modifier.weight(1f).fillMaxHeight(), onClick = onUpdate)
                        PhotoSlot(label = "Bên", takenAt = side?.takenAt, modifier = Modifier.weight(1f).fillMaxHeight(), onClick = onUpdate)
                        PhotoSlot(label = "Sau", takenAt = back?.takenAt, modifier = Modifier.weight(1f).fillMaxHeight(), onClick = onUpdate)
                    }
                }
            }
        }
        Row(
            modifier = Modifier.padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = TrainColors.Charcoal.copy(alpha = 0.72f), modifier = Modifier.size(14.dp))
            Text(
                "Ảnh là tùy chọn, được thêm trong lúc cập nhật chỉ số cơ thể.",
                color = TrainColors.Charcoal.copy(alpha = 0.72f),
                fontSize = 11.5.sp,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun PhotoSlot(label: String, takenAt: String?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier = modifier.fillMaxHeight()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(if (takenAt != null) TrainColors.SlateMist else TrainColors.KeylimeWash)
                .then(
                    if (takenAt == null) {
                        Modifier.border(1.dp, TrainColors.MintGlaze, RoundedCornerShape(12.dp))
                    } else {
                        Modifier
                    }
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = if (takenAt != null) Icons.Default.AccessibilityNew else Icons.Default.Add,
                    contentDescription = null,
                    tint = TrainColors.Forest,
                    modifier = Modifier.size(if (takenAt != null) 34.dp else 26.dp)
                )
                Text(
                    takenAt?.let(::formatMetricDateShort) ?: "Thêm ảnh",
                    color = TrainColors.Forest,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Text(
            label,
            modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
            color = TrainColors.Charcoal,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmptyBodyMetricsState(message: String, onUpdate: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TrainCard(tone = TrainTone.Keylime, padding = PaddingValues(24.dp)) {
            Text(
                message,
                modifier = Modifier.fillMaxWidth(),
                color = TrainColors.Forest,
                textAlign = TextAlign.Center,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(14.dp))
        TrainPrimaryButton(text = "Cập nhật chỉ số", icon = Icons.Default.Add, onClick = onUpdate)
    }
}

private data class Measure(val label: String, val value: String, val icon: ImageVector)

private fun bmiLabel(bmi: Float?): String {
    val value = bmi ?: return "--"
    return when {
        value < 18.5f -> "Thiếu"
        value < 25f -> "Bình thường"
        value < 30f -> "Thừa"
        else -> "Béo phì"
    }
}

private fun bmiColor(bmi: Float?): Color {
    val value = bmi ?: return TrainColors.Forest
    return when {
        value < 18.5f -> Color(0xFF3F7D8C)
        value < 25f -> TrainColors.Forest
        value < 30f -> TrainColors.Sport
        else -> TrainColors.Cardio
    }
}

private fun formatNumber(value: Float?): String {
    if (value == null) return "--"
    val rounded = value.toInt()
    return if (abs(value - rounded) < 0.05f) rounded.toString() else String.format(Locale.US, "%.1f", value)
}

private fun formatMetricDate(value: String): String {
    return runCatching {
        LocalDate.parse(value.take(10)).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }.getOrElse { value.take(10) }
}

private fun formatMetricDateShort(value: String): String {
    return runCatching {
        LocalDate.parse(value.take(10)).format(DateTimeFormatter.ofPattern("dd/MM"))
    }.getOrElse { value.take(5) }
}

private fun formatMetricDateDayMonth(value: String): String {
    return runCatching {
        LocalDate.parse(value.take(10)).format(DateTimeFormatter.ofPattern("dd/MM"))
    }.getOrElse { value.take(5) }
}
