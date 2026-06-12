package com.vitalai.ui.screens.metrics.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vitalai.data.remote.model.BodyMetricDto
import com.vitalai.data.remote.model.HealthProfileDto
import com.vitalai.data.remote.model.ProgressPhotoDto
import com.vitalai.ui.components.SectionHeader
import com.vitalai.ui.components.VitalCard
import com.vitalai.ui.screens.metrics.bmiColor
import com.vitalai.ui.screens.metrics.bmiLabel
import com.vitalai.ui.screens.metrics.calculateAge
import com.vitalai.ui.screens.metrics.formatCompact
import com.vitalai.ui.screens.metrics.formatDateDisplay
import com.vitalai.ui.screens.metrics.formatGender
import com.vitalai.ui.screens.metrics.formatPhotoType
import com.vitalai.ui.theme.*
import kotlin.math.pow

@Composable
internal fun CurrentWeightCard(
    latest: BodyMetricDto?,
    profile: HealthProfileDto?,
    modifier: Modifier = Modifier,
    onLogWeight: () -> Unit
) {
    val targetWeightKg = profile?.targetWeightKg
    val currentWeightKg = latest?.weightKg

    val progress = remember(currentWeightKg, targetWeightKg) {
        if (currentWeightKg == null || targetWeightKg == null || targetWeightKg == 0f || currentWeightKg == 0f) {
            0f
        } else {
            if (currentWeightKg <= targetWeightKg) {
                (currentWeightKg / targetWeightKg).coerceIn(0f, 1f)
            } else {
                (targetWeightKg / currentWeightKg).coerceIn(0f, 1f)
            }
        }
    }

    val shape = RoundedCornerShape(20.dp)

    com.vitalai.ui.components.VitalBorderCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Hiện tại",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = ForestGreen,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                currentWeightKg?.let { "${it.formatCompact()} kg" } ?: "-- kg",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = ForestGreen,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(VitalRadius.Pill))
                    .background(Color(0xFFE2E0DC))
            ) {
                val amplifiedProgress = progress.toDouble().pow(1.3).toFloat().coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(amplifiedProgress)
                        .fillMaxHeight()
                        .background(ForestGreen)
                )
            }

            Spacer(Modifier.height(6.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = Color(0xFF9A9A9A), fontWeight = FontWeight.Normal)) {
                        append("Mục tiêu ")
                    }
                    withStyle(style = SpanStyle(color = ForestGreen, fontWeight = FontWeight.Bold)) {
                        append(targetWeightKg?.let { "${it.formatCompact()} kg" } ?: "-- kg")
                    }
                },
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ForestGreen)
                .clickable(onClick = onLogWeight)
                .padding(horizontal = 16.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Cập nhật", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
internal fun BmiBar(bmi: Float) {
    val minBmi = 10f
    val maxBmi = 40f
    val clampedBmi = bmi.coerceIn(minBmi, maxBmi)
    val thumbFraction = (clampedBmi - minBmi) / (maxBmi - minBmi)

    com.vitalai.ui.components.VitalBorderCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Chỉ số BMI", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ForestGreen)
                Text(
                    text = "${bmi.formatCompact()} - ${bmiLabel(bmi)}",
                    fontSize = 12.sp,
                    color = bmiColor(bmi),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End
                )
            }
            Spacer(Modifier.height(12.dp))

            val zones = remember {
                listOf(
                    Triple("Thiếu cân", Color(0xFF60A5FA), 8.5f / 30f),
                    Triple("Bình thường", Color(0xFF34D399), 6.5f / 30f),
                    Triple("Thừa cân", Color(0xFFFBBF24), 5f / 30f),
                    Triple("Béo phì", Color(0xFFF87171), 10f / 30f)
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(VitalRadius.Pill))
                    ) {
                        zones.forEach { (_, color, fraction) ->
                            Box(modifier = Modifier.weight(fraction).fillMaxHeight().background(color))
                        }
                    }

                    val safeFraction = thumbFraction.coerceIn(0.0001f, 0.9999f)
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.weight(safeFraction))
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Ink900)
                                .border(2.dp, AppSurface, CircleShape)
                        )
                        Spacer(modifier = Modifier.weight(1f - safeFraction))
                    }
                }

                Spacer(Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    zones.forEach { (label, color, fraction) ->
                        Text(
                            text = label,
                            modifier = Modifier.weight(fraction),
                            fontSize = 9.sp,
                            color = color,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun PersonalInfoCard(profile: HealthProfileDto?, onClick: () -> Unit) {
    com.vitalai.ui.components.VitalBorderCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Thông tin cá nhân", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ForestGreen)
            PersonalInfoRow("Giới tính", formatGender(profile?.gender))
            PersonalInfoRow("Cân nặng ban đầu", profile?.initialWeightKg?.let { "${it.formatCompact()} kg" } ?: "--")
            PersonalInfoRow("Chiều cao", profile?.heightCm?.let { "%.0f cm".format(it) } ?: "--")
            PersonalInfoRow("Tuổi", calculateAge(profile?.birthDate)?.let { "$it tuổi" } ?: "--")
        }
    }
}

@Composable
internal fun PersonalInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = Ink500)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ForestGreen, textAlign = TextAlign.End)
    }
}

@Composable
internal fun EnergyBasicsCard(latest: BodyMetricDto) {
    com.vitalai.ui.components.VitalBorderCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Năng lượng cơ bản", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ForestGreen)
            latest.bmr?.let { PersonalInfoRow("BMR", "%.0f kcal/ngày".format(it)) }
            latest.tdee?.let { PersonalInfoRow("TDEE", "%.0f kcal/ngày".format(it)) }
        }
    }
}

@Composable
internal fun WeightChangeRow(
    weightChange: Float,
    latestDate: String?,
    startWeight: Float,
    currentWeight: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    com.vitalai.ui.components.VitalBorderCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Tiến độ cân nặng", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = ForestGreen)
            latestDate?.let { date ->
                Text("Cập nhật: ${formatDateDisplay(date)}", fontSize = 11.sp, color = Ink400)
            }
            val arrow = if (weightChange <= 0f) "↓" else "↑"
            val changeColor = if (weightChange <= 0f) ForestGreen else Color(0xFFF87171)
            Text(
                "$arrow ${kotlin.math.abs(weightChange).formatCompact()} kg",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = changeColor,
                maxLines = 1
            )
            Text("${startWeight.formatCompact()} → ${currentWeight.formatCompact()} kg", fontSize = 12.sp, color = Ink500, maxLines = 1)
        }
    }
}

@Composable
internal fun WeightProgressEmptyCard(modifier: Modifier = Modifier, onClick: () -> Unit) {
    com.vitalai.ui.components.VitalBorderCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Tiến độ cân nặng", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = ForestGreen)
            Text("Chưa có dữ liệu", fontSize = 12.sp, color = Ink500)
            Text("-- kg", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink400)
        }
    }
}

@Composable
internal fun BodyMeasurementsCard(latest: BodyMetricDto?, onClick: () -> Unit) {
    val measurements = buildList {
        latest?.bodyFatPct?.let { add("Tỷ lệ mỡ" to "${it.formatCompact()}%") }
        latest?.waistCm?.let { add("Vòng eo" to "%.0f cm".format(it)) }
        latest?.hipCm?.let { add("Vòng hông" to "%.0f cm".format(it)) }
        latest?.chestCm?.let { add("Vòng ngực" to "%.0f cm".format(it)) }
        latest?.armCm?.let { add("Bắp tay" to "%.0f cm".format(it)) }
        latest?.neckCm?.let { add("Vòng cổ" to "%.0f cm".format(it)) }
    }

    com.vitalai.ui.components.VitalBorderCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        SectionHeader(title = "Số đo cơ thể", color = ForestGreen)
        Spacer(Modifier.height(12.dp))

        if (measurements.isEmpty()) {
            Text(
                "Chưa có số đo. Nhấn 'Cập nhật' để thêm.",
                color = Ink500,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                measurements.forEach { (label, value) ->
                    PersonalInfoRow(label = label, value = value)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ProgressPhotosSection(
    photos: List<ProgressPhotoDto>,
    isUploading: Boolean,
    onAddPhotoClick: () -> Unit,
    onDeletePhoto: (String) -> Unit
) {
    com.vitalai.ui.components.VitalBorderCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(title = "Ảnh tiến độ", color = ForestGreen)
            if (isUploading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Mint500)
            }
        }
        Spacer(Modifier.height(10.dp))

        if (photos.isEmpty()) {
            Text(
                "Chưa có ảnh tiến độ",
                color = Ink400,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
        }

        if (photos.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                items(photos) { photo ->
                    var showDeleteDialog by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(VitalRadius.Md))
                            .combinedClickable(onClick = {}, onLongClick = { showDeleteDialog = true })
                    ) {
                        AsyncImage(
                            model = photo.photoUrl,
                            contentDescription = photo.photoType,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.45f))
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                formatPhotoType(photo.photoType),
                                fontSize = 9.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    if (showDeleteDialog) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false },
                            title = { Text("Xóa ảnh?", color = Ink900) },
                            text = { Text("Bạn có chắc muốn xóa ảnh này không?", color = Ink700) },
                            confirmButton = {
                                Button(
                                    onClick = { onDeletePhoto(photo.id); showDeleteDialog = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF87171))
                                ) { Text("Xóa", color = Color.White) }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDeleteDialog = false }) { Text("Hủy") }
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        OutlinedButton(
            onClick = onAddPhotoClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(VitalRadius.Md),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestGreen),
            border = androidx.compose.foundation.BorderStroke(1.dp, ForestGreen)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Thêm ảnh", fontWeight = FontWeight.SemiBold, color = ForestGreen)
        }
    }
}
