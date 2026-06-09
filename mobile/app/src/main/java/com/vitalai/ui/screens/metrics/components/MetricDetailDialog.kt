package com.vitalai.ui.screens.metrics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vitalai.data.remote.model.BodyMetricDto
import com.vitalai.ui.screens.metrics.formatCompact
import com.vitalai.ui.screens.metrics.formatDateDisplay
import com.vitalai.ui.theme.*
import kotlin.math.abs

@Composable
internal fun MetricDetailDialog(
    metric: BodyMetricDto,
    photoUrls: List<String>,
    previousMetric: BodyMetricDto? = null,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chi tiết chỉ số", fontWeight = FontWeight.Bold, color = ForestGreen) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (photoUrls.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(photoUrls) { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = "Ảnh tiến độ",
                                modifier = Modifier
                                    .size(width = 112.dp, height = 138.dp)
                                    .clip(RoundedCornerShape(VitalRadius.Md)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(VitalRadius.Md))
                            .background(AppSurface2)
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Chưa có ảnh gắn với bản ghi này", color = Ink500, fontSize = 12.sp)
                    }
                }

                DetailRow("Ngày đo", formatDateDisplay(metric.date))

                DeltaDetailRow(
                    label = "Cân nặng",
                    value = "${metric.weightKg.formatCompact()} kg",
                    delta = previousMetric?.weightKg?.let { prev -> metric.weightKg - prev }
                )
                metric.heightCm?.let { current ->
                    DeltaDetailRow(
                        label = "Chiều cao",
                        value = "${current.formatCompact()} cm",
                        delta = previousMetric?.heightCm?.let { prev -> current - prev }
                    )
                }
                metric.bmi?.let { DetailRow("BMI", it.formatCompact()) }
                metric.bmr?.let { DetailRow("BMR", "%.0f kcal/ngày".format(it)) }
                metric.tdee?.let { DetailRow("TDEE", "%.0f kcal/ngày".format(it)) }

                metric.bodyFatPct?.let { current ->
                    DeltaDetailRow(
                        label = "Tỷ lệ mỡ",
                        value = "${current.formatCompact()}%",
                        delta = previousMetric?.bodyFatPct?.let { prev -> current - prev }
                    )
                }
                metric.waistCm?.let { current ->
                    DeltaDetailRow(
                        label = "Vòng eo",
                        value = "${current.formatCompact()} cm",
                        delta = previousMetric?.waistCm?.let { prev -> current - prev }
                    )
                }
                metric.hipCm?.let { current ->
                    DeltaDetailRow(
                        label = "Vòng mông",
                        value = "${current.formatCompact()} cm",
                        delta = previousMetric?.hipCm?.let { prev -> current - prev }
                    )
                }
                metric.chestCm?.let { current ->
                    DeltaDetailRow(
                        label = "Vòng ngực",
                        value = "${current.formatCompact()} cm",
                        delta = previousMetric?.chestCm?.let { prev -> current - prev }
                    )
                }
                metric.armCm?.let { current ->
                    DeltaDetailRow(
                        label = "Bắp tay",
                        value = "${current.formatCompact()} cm",
                        delta = previousMetric?.armCm?.let { prev -> current - prev }
                    )
                }
                metric.neckCm?.let { current ->
                    DeltaDetailRow(
                        label = "Vòng cổ",
                        value = "${current.formatCompact()} cm",
                        delta = previousMetric?.neckCm?.let { prev -> current - prev }
                    )
                }

                metric.notes?.let { notes ->
                    if (notes.isNotBlank()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text("Ghi chú", fontSize = 13.sp, color = Ink500)
                            Text(
                                text = notes,
                                fontSize = 13.sp,
                                color = ForestGreen,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
            ) {
                Text("Đóng", color = Color.White)
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = Ink500)
        Text(value, fontSize = 13.sp, color = ForestGreen, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DeltaDetailRow(label: String, value: String, delta: Float?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.sp, color = Ink500)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(value, fontSize = 13.sp, color = ForestGreen, fontWeight = FontWeight.Medium)
            if (delta != null && abs(delta) >= 0.05f) {
                val isUp = delta > 0f
                val deltaColor = if (isUp) Color(0xFFF87171) else ForestGreen
                val arrow = if (isUp) "↑" else "↓"
                Text(
                    "($arrow${abs(delta).formatCompact()})",
                    fontSize = 12.sp,
                    color = deltaColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
