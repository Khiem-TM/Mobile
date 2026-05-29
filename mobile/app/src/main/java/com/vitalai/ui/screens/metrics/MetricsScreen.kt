package com.vitalai.ui.screens.metrics

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vitalai.data.remote.model.BodyMetricDto
import com.vitalai.data.remote.model.BodyMetricsPeriodDto
import com.vitalai.data.remote.model.ProgressPhotoDto
import com.vitalai.data.remote.model.UpsertBodyMetricRequest
import com.vitalai.navigation.Screen
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.components.MetricTile
import com.vitalai.ui.components.SectionHeader
import com.vitalai.ui.components.VitalCard
import com.vitalai.ui.theme.*
import java.io.File
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricsScreen(
    navController: NavController,
    viewModel: MetricsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showUpdateSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.updateSuccessCount) {
        if (uiState.updateSuccessCount > 0) {
            Toast.makeText(context, "Cập nhật thông tin thành công", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chỉ số cơ thể", fontWeight = FontWeight.Bold, color = Ink900) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = Ink900)
                    }
                },
                actions = {
                    TextButton(onClick = {
                        viewModel.setUpdateTab(0)
                        showUpdateSheet = true
                    }) {
                        Text("Cập nhật", color = Mint500, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppSurface)
            )
        },
        containerColor = AppMutedBackground
    ) { padding ->
        when {
            uiState.isLoading -> LoadingState(modifier = Modifier.padding(padding))
            uiState.error != null && uiState.latest == null -> ErrorState(
                message = uiState.error!!,
                onRetry = viewModel::loadData,
                modifier = Modifier.padding(padding)
            )
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Latest metric + weight/height/BMI card
                item {
                    WeightHeightBmiCard(latest = uiState.latest)
                }

                // 2. BMI progress bar
                uiState.latest?.bmi?.let { bmi ->
                    item { BmiBar(bmi = bmi) }
                }

                // 3. BMR + TDEE row
                uiState.latest?.let { latest ->
                    if (latest.bmr != null || latest.tdee != null) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                latest.bmr?.let { bmr ->
                                    MetricTile(
                                        label = "BMR (kcal/ngày)",
                                        value = "%.0f".format(bmr),
                                        modifier = Modifier.weight(1f),
                                        color = Mint700,
                                        subtitle = "Nhu cầu cơ bản"
                                    )
                                }
                                latest.tdee?.let { tdee ->
                                    MetricTile(
                                        label = "TDEE (kcal/ngày)",
                                        value = "%.0f".format(tdee),
                                        modifier = Modifier.weight(1f),
                                        color = Color(0xFF6366F1),
                                        subtitle = "Tiêu thụ thực tế"
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Weight change row
                uiState.summary?.let { summary ->
                    item {
                        WeightChangeRow(
                            weightChange = summary.weightChange,
                            startDate = summary.startDate,
                            startWeight = summary.startWeight,
                            currentWeight = summary.currentWeight,
                            totalRecords = summary.totalRecords
                        )
                    }
                }

                // 5. Period chart section
                item {
                    PeriodChartSection(
                        selectedPeriod = uiState.selectedPeriod,
                        periodData = uiState.periodData,
                        onSelectPeriod = viewModel::selectPeriod
                    )
                }

                // 6. History navigation button
                item {
                    TextButton(
                        onClick = { navController.navigate(Screen.MetricsHistory) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "Xem lịch sử đầy đủ",
                                color = Mint600,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Mint600,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // 7. Body measurements card
                uiState.latest?.let { latest ->
                    item {
                        BodyMeasurementsCard(latest = latest)
                    }
                }

                // 8. Progress photos section
                item {
                    ProgressPhotosSection(
                        photos = uiState.photos,
                        isUploading = uiState.isUploadingPhoto,
                        onAddPhotoClick = {
                            viewModel.setUpdateTab(2)
                            showUpdateSheet = true
                        },
                        onDeletePhoto = { id -> viewModel.deletePhoto(id) }
                    )
                }

                // Empty state
                if (uiState.latest == null && !uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("📊", fontSize = 40.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Chưa có dữ liệu chỉ số cơ thể",
                                    color = Ink500,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Nhấn 'Cập nhật' để thêm chỉ số đầu tiên",
                                    color = Ink400,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Update bottom sheet
    if (showUpdateSheet) {
        UpdateBottomSheet(
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = { showUpdateSheet = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────
// WeightHeightBmiCard
// ─────────────────────────────────────────────────────────────

@Composable
private fun WeightHeightBmiCard(latest: BodyMetricDto?) {
    VitalCard(modifier = Modifier.fillMaxWidth()) {
        if (latest == null) {
            Text(
                "Chưa có số liệu. Nhấn 'Cập nhật' để thêm.",
                color = Ink500,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
            return@VitalCard
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "%.1f kg".format(latest.weightKg),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink900
                )
                Text("Cân nặng", fontSize = 12.sp, color = Ink500)
            }
            latest.heightCm?.let { h ->
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "%.0f cm".format(h),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Ink700
                    )
                    Text("Chiều cao", fontSize = 12.sp, color = Ink500)
                }
            }
        }
        latest.bmi?.let { bmi ->
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = AppLine)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "BMI: ",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink700
                )
                Text(
                    "%.1f".format(bmi),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = bmiColor(bmi)
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(VitalRadius.Pill))
                        .background(bmiColor(bmi).copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        bmiLabel(bmi),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = bmiColor(bmi)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// BmiBar
// ─────────────────────────────────────────────────────────────

@Composable
private fun BmiBar(bmi: Float) {
    // BMI range shown: 10 to 40
    val minBmi = 10f
    val maxBmi = 40f
    val clampedBmi = bmi.coerceIn(minBmi, maxBmi)
    val thumbFraction = (clampedBmi - minBmi) / (maxBmi - minBmi)

    VitalCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Text("Chỉ số BMI", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink900)
        Spacer(Modifier.height(12.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val totalWidth = maxWidth

            // Zone fractions relative to range 10-40 (total span = 30)
            // Thiếu cân: 10-18.5 (8.5), Bình thường: 18.5-25 (6.5), Thừa cân: 25-30 (5), Béo phì: 30-40 (10)
            val zones = listOf(
                Triple("Thiếu cân", Color(0xFF60A5FA), 8.5f / 30f),
                Triple("Bình thường", Color(0xFF34D399), 6.5f / 30f),
                Triple("Thừa cân", Color(0xFFFBBF24), 5f / 30f),
                Triple("Béo phì", Color(0xFFF87171), 10f / 30f)
            )

            Column {
                // Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(VitalRadius.Pill))
                ) {
                    zones.forEach { (_, color, fraction) ->
                        Box(
                            modifier = Modifier
                                .weight(fraction)
                                .fillMaxHeight()
                                .background(color)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Thumb indicator
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .offset(x = (totalWidth * thumbFraction) - 6.dp)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Ink900)
                            .border(2.dp, AppSurface, CircleShape)
                    )
                }

                Spacer(Modifier.height(6.dp))

                // Labels
                Row(modifier = Modifier.fillMaxWidth()) {
                    zones.forEach { (label, color, fraction) ->
                        Text(
                            label,
                            modifier = Modifier.weight(fraction),
                            fontSize = 9.sp,
                            color = color,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            "Chỉ số của bạn: %.1f — %s".format(bmi, bmiLabel(bmi)),
            fontSize = 12.sp,
            color = bmiColor(bmi),
            fontWeight = FontWeight.SemiBold
        )
    }
}

// ─────────────────────────────────────────────────────────────
// WeightChangeRow
// ─────────────────────────────────────────────────────────────

@Composable
private fun WeightChangeRow(
    weightChange: Float,
    startDate: String?,
    startWeight: Float,
    currentWeight: Float,
    totalRecords: Int
) {
    VitalCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Tiến độ cân nặng", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Ink900)
                Text("$totalRecords lần cập nhật", fontSize = 12.sp, color = Ink500)
                startDate?.let { date ->
                    Text(
                        "Từ ${formatDateDisplay(date)}",
                        fontSize = 11.sp,
                        color = Ink400,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                val arrow = if (weightChange <= 0f) "↓" else "↑"
                val changeColor = if (weightChange <= 0f) Color(0xFF34D399) else Color(0xFFF87171)
                Text(
                    "$arrow %+.1f kg".format(weightChange),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = changeColor
                )
                Text(
                    "%.1f → %.1f kg".format(startWeight, currentWeight),
                    fontSize = 12.sp,
                    color = Ink500
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// PeriodChartSection
// ─────────────────────────────────────────────────────────────

@Composable
private fun PeriodChartSection(
    selectedPeriod: String,
    periodData: BodyMetricsPeriodDto?,
    onSelectPeriod: (String) -> Unit
) {
    VitalCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeader(title = "Biểu đồ cân nặng")
            Spacer(Modifier.height(10.dp))

            // Tab row: Tuần / Tháng / Quý
            val tabs = listOf(
                "week" to "Tuần",
                "month" to "Tháng",
                "3months" to "Quý"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(VitalRadius.Pill))
                    .background(AppSurface2)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                tabs.forEach { (key, label) ->
                    val isSelected = selectedPeriod == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(VitalRadius.Pill))
                            .background(if (isSelected) Mint500 else Color.Transparent)
                            .clickable { onSelectPeriod(key) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) Color.White else Ink700
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (periodData != null && periodData.data.size >= 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "TB: ${"%.1f".format(periodData.avgWeight)} kg",
                        fontSize = 12.sp,
                        color = Ink500
                    )
                    Text(
                        "↓%.1f / ↑%.1f kg".format(periodData.minWeight, periodData.maxWeight),
                        fontSize = 12.sp,
                        color = Ink500
                    )
                }
                Spacer(Modifier.height(6.dp))
                WeightLineChart(
                    data = periodData.data,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Cần ít nhất 2 điểm dữ liệu để hiển thị biểu đồ",
                        color = Ink400,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// WeightLineChart
// ─────────────────────────────────────────────────────────────

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

        // Fill area
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

// ─────────────────────────────────────────────────────────────
// BodyMeasurementsCard
// ─────────────────────────────────────────────────────────────

@Composable
private fun BodyMeasurementsCard(latest: BodyMetricDto) {
    val measurements = buildList {
        latest.bodyFatPct?.let { add("Tỷ lệ mỡ" to "%.1f%%".format(it)) }
        latest.waistCm?.let { add("Vòng eo" to "%.0f cm".format(it)) }
        latest.hipCm?.let { add("Vòng mông" to "%.0f cm".format(it)) }
        latest.chestCm?.let { add("Vòng ngực" to "%.0f cm".format(it)) }
        latest.armCm?.let { add("Bắp tay" to "%.0f cm".format(it)) }
        latest.neckCm?.let { add("Vòng cổ" to "%.0f cm".format(it)) }
    }

    VitalCard(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(title = "Số đo cơ thể")
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
            // 2-column grid
            measurements.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { (label, value) ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(VitalRadius.Md))
                                .background(AppSurface2)
                                .padding(10.dp)
                        ) {
                            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Ink900)
                            Text(label, fontSize = 11.sp, color = Ink500, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                    // If odd number of items, fill remaining space
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// ProgressPhotosSection
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProgressPhotosSection(
    photos: List<ProgressPhotoDto>,
    isUploading: Boolean,
    onAddPhotoClick: () -> Unit,
    onDeletePhoto: (String) -> Unit
) {
    VitalCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader(title = "Ảnh tiến độ")
            if (isUploading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Mint500
                )
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
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(photos) { photo ->
                    var showDeleteDialog by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(VitalRadius.Md))
                            .combinedClickable(
                                onClick = {},
                                onLongClick = { showDeleteDialog = true }
                            )
                    ) {
                        AsyncImage(
                            model = photo.photoUrl,
                            contentDescription = photo.photoType,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Photo type badge
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
                            containerColor = AppSurface,
                            title = { Text("Xóa ảnh?", color = Ink900) },
                            text = { Text("Bạn có chắc muốn xóa ảnh này không?", color = Ink700) },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        onDeletePhoto(photo.id)
                                        showDeleteDialog = false
                                    },
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

        // Add photo button
        OutlinedButton(
            onClick = onAddPhotoClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(VitalRadius.Md),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Mint600),
            border = androidx.compose.foundation.BorderStroke(1.dp, Mint500)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Thêm ảnh", fontWeight = FontWeight.SemiBold)
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Update Bottom Sheet
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdateBottomSheet(
    uiState: MetricsUiState,
    viewModel: MetricsViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AppSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Tab row
            TabRow(
                selectedTabIndex = uiState.updateSheetTab,
                containerColor = AppSurface,
                contentColor = Mint500
            ) {
                listOf("Cơ bản", "Nâng cao", "Ảnh").forEachIndexed { index, label ->
                    Tab(
                        selected = uiState.updateSheetTab == index,
                        onClick = { viewModel.setUpdateTab(index) },
                        text = {
                            Text(
                                label,
                                fontWeight = FontWeight.SemiBold,
                                color = if (uiState.updateSheetTab == index) Mint600 else Ink500
                            )
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                when (uiState.updateSheetTab) {
                    0 -> BasicUpdateTab(viewModel = viewModel, onDone = onDismiss)
                    1 -> AdvancedUpdateTab(viewModel = viewModel, onDone = onDismiss)
                    2 -> PhotoUpdateTab(viewModel = viewModel, photos = uiState.photos, onDone = onDismiss)
                }
            }
        }
    }
}

@Composable
private fun BasicUpdateTab(viewModel: MetricsViewModel, onDone: () -> Unit) {
    var weightStr by remember { mutableStateOf("") }
    var heightStr by remember { mutableStateOf("") }
    val weightVal = weightStr.toFloatOrNull()
    val heightVal = heightStr.toFloatOrNull()
    val bmiPreview = if (weightVal != null && heightVal != null && heightVal > 0f) {
        weightVal / ((heightVal / 100f) * (heightVal / 100f))
    } else null

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Cập nhật cơ bản", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink900)

        OutlinedTextField(
            value = weightStr,
            onValueChange = { weightStr = it },
            label = { Text("Cân nặng (kg) *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Mint500,
                focusedLabelColor = Mint500
            )
        )

        OutlinedTextField(
            value = heightStr,
            onValueChange = { heightStr = it },
            label = { Text("Chiều cao (cm) *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Mint500,
                focusedLabelColor = Mint500
            )
        )

        // BMI preview
        bmiPreview?.let { bmi ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(VitalRadius.Md))
                    .background(bmiColor(bmi).copy(alpha = 0.10f))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("BMI dự kiến: ", fontSize = 14.sp, color = Ink700)
                    Text(
                        "%.1f".format(bmi),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = bmiColor(bmi)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        bmiLabel(bmi),
                        fontSize = 13.sp,
                        color = bmiColor(bmi),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Button(
            onClick = {
                if (weightVal != null) {
                    viewModel.addMetric(
                        UpsertBodyMetricRequest(
                            recordedAt = LocalDate.now().toString(),
                            weightKg = weightVal,
                            heightCm = heightVal
                        )
                    )
                    onDone()
                }
            },
            enabled = weightVal != null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(VitalRadius.Md),
            colors = ButtonDefaults.buttonColors(containerColor = Mint500)
        ) {
            Text("Lưu", fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

@Composable
private fun AdvancedUpdateTab(viewModel: MetricsViewModel, onDone: () -> Unit) {
    var bodyFatStr by remember { mutableStateOf("") }
    var waistStr by remember { mutableStateOf("") }
    var hipStr by remember { mutableStateOf("") }
    var chestStr by remember { mutableStateOf("") }
    var armStr by remember { mutableStateOf("") }
    var neckStr by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Số đo nâng cao", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink900)
        Text("Tất cả trường đều tùy chọn", fontSize = 12.sp, color = Ink500)

        val fieldDefs = listOf(
            Triple("% Mỡ cơ thể", bodyFatStr) { v: String -> bodyFatStr = v },
            Triple("Vòng eo (cm)", waistStr) { v: String -> waistStr = v },
            Triple("Vòng mông (cm)", hipStr) { v: String -> hipStr = v },
            Triple("Vòng ngực (cm)", chestStr) { v: String -> chestStr = v },
            Triple("Bắp tay (cm)", armStr) { v: String -> armStr = v },
            Triple("Vòng cổ (cm)", neckStr) { v: String -> neckStr = v }
        )

        fieldDefs.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { (label, value, onChange) ->
                    OutlinedTextField(
                        value = value,
                        onValueChange = onChange,
                        label = { Text(label, fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Mint500,
                            focusedLabelColor = Mint500
                        )
                    )
                }
            }
        }

        Button(
            onClick = {
                viewModel.addMetric(
                    UpsertBodyMetricRequest(
                        recordedAt = LocalDate.now().toString(),
                        bodyFatPct = bodyFatStr.toFloatOrNull(),
                        waistCm = waistStr.toFloatOrNull(),
                        hipCm = hipStr.toFloatOrNull(),
                        chestCm = chestStr.toFloatOrNull(),
                        neckCm = neckStr.toFloatOrNull()
                        // armCm is display-only; not in UpsertBodyMetricRequest
                    )
                )
                onDone()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(VitalRadius.Md),
            colors = ButtonDefaults.buttonColors(containerColor = Mint500)
        ) {
            Text("Lưu số đo", fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

@Composable
private fun PhotoUpdateTab(
    viewModel: MetricsViewModel,
    photos: List<ProgressPhotoDto>,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    var selectedPhotoType by remember { mutableStateOf("front") }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    val photoTypes = listOf(
        "front" to "Mặt trước",
        "back" to "Mặt sau",
        "side" to "Mặt bên"
    )

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Thêm ảnh tiến độ", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink900)

        // Photo type selection
        Text("Loại ảnh", fontSize = 14.sp, color = Ink700, fontWeight = FontWeight.SemiBold)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            photoTypes.forEach { (type, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(VitalRadius.Md))
                        .clickable { selectedPhotoType = type }
                        .background(
                            if (selectedPhotoType == type) Mint500.copy(alpha = 0.10f)
                            else Color.Transparent
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedPhotoType == type,
                        onClick = { selectedPhotoType = type },
                        colors = RadioButtonDefaults.colors(selectedColor = Mint500)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(label, fontSize = 14.sp, color = Ink900)
                }
            }
        }

        // Selected image preview
        selectedUri?.let { uri ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(VitalRadius.Md))
            ) {
                AsyncImage(
                    model = uri,
                    contentDescription = "Ảnh đã chọn",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = { selectedUri = null },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Xóa ảnh đã chọn",
                        tint = Color.White,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .padding(4.dp)
                    )
                }
            }
        }

        // Pick from gallery
        OutlinedButton(
            onClick = { launcher.launch("image/*") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(VitalRadius.Md),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Mint600),
            border = androidx.compose.foundation.BorderStroke(1.dp, Mint500)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                if (selectedUri == null) "Chọn ảnh từ thư viện" else "Chọn ảnh khác",
                fontWeight = FontWeight.SemiBold
            )
        }

        // Upload button
        Button(
            onClick = {
                val uri = selectedUri
                if (uri != null) {
                    val file = uriToFile(context, uri)
                    if (file != null) {
                        viewModel.uploadPhoto(file, selectedPhotoType)
                        onDone()
                    } else {
                        Toast.makeText(context, "Không thể đọc ảnh", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            enabled = selectedUri != null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(VitalRadius.Md),
            colors = ButtonDefaults.buttonColors(containerColor = Mint500)
        ) {
            Text("Tải lên", fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────

private fun bmiColor(bmi: Float): Color = when {
    bmi < 18.5f -> Color(0xFF60A5FA)
    bmi < 25f -> Color(0xFF34D399)
    bmi < 30f -> Color(0xFFFBBF24)
    else -> Color(0xFFF87171)
}

private fun bmiLabel(bmi: Float): String = when {
    bmi < 18.5f -> "Thiếu cân"
    bmi < 25f -> "Bình thường"
    bmi < 30f -> "Thừa cân"
    else -> "Béo phì"
}

private fun formatPhotoType(type: String): String = when (type) {
    "front" -> "Mặt trước"
    "back" -> "Mặt sau"
    "side" -> "Mặt bên"
    else -> type
}

private fun formatDateDisplay(dateStr: String): String {
    return runCatching {
        val date = LocalDate.parse(dateStr)
        "%02d/%02d/%04d".format(date.dayOfMonth, date.monthValue, date.year)
    }.getOrDefault(dateStr)
}

private fun uriToFile(context: android.content.Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File.createTempFile("progress_photo_", ".jpg", context.cacheDir)
        tempFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        inputStream.close()
        tempFile
    } catch (e: Exception) {
        null
    }
}
