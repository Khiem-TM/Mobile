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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.vitalai.data.remote.model.HealthProfileDto
import com.vitalai.data.remote.model.ProgressPhotoDto
import com.vitalai.data.remote.model.UpsertBodyMetricRequest
import com.vitalai.navigation.Screen
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.components.SectionHeader
import com.vitalai.ui.components.VitalCard
import com.vitalai.ui.theme.*
import java.io.File
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import kotlin.math.pow
import android.util.Log

private val MetricGreen = Color(0xFF228C66)

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
                // 1. Current weight + weight progress
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CurrentWeightCard(
                            latest = uiState.latest,
                            profile = uiState.healthProfile,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            onLogWeight = {
                                viewModel.setUpdateTab(0)
                                showUpdateSheet = true
                            }
                        )
                        uiState.summary?.let { summary ->
                            WeightChangeRow(
                                weightChange = summary.weightChange,
                                latestDate = summary.latestDate,
                                startWeight = summary.startWeight,
                                currentWeight = summary.currentWeight,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                onClick = { navController.navigate(Screen.MetricsHistory) }
                            )
                        } ?: WeightProgressEmptyCard(
                            modifier = Modifier.weight(1f),
                            onClick = { navController.navigate(Screen.MetricsHistory) }
                        )
                    }
                }

                // 2. Period chart section
                item {
                    PeriodChartSection(
                        selectedPeriod = uiState.selectedPeriod,
                        periodData = uiState.periodData,
                        onSelectPeriod = viewModel::selectPeriod
                    )
                }

                // 3. BMI progress bar
                val profileBmi = calculateBmi(uiState.latest?.weightKg, uiState.healthProfile?.heightCm)
                profileBmi?.let { bmi ->
                    item { BmiBar(bmi = bmi) }
                }

                // 4. Personal information
                item {
                    PersonalInfoCard(
                        profile = uiState.healthProfile,
                        onClick = {
                            viewModel.setUpdateTab(3)
                            showUpdateSheet = true
                        }
                    )
                }

                // 5. Energy basics
                uiState.latest?.let { latest ->
                    if (latest.bmr != null || latest.tdee != null) {
                        item {
                            EnergyBasicsCard(latest = latest)
                        }
                    }
                }

                // 6. Body measurements card
                uiState.latest?.let { latest ->
                    item {
                        BodyMeasurementsCard(
                            latest = latest,
                            onClick = {
                                viewModel.setUpdateTab(1)
                                showUpdateSheet = true
                            }
                        )
                    }
                }

                // 7. Progress photos section
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
// CurrentWeightCard
// ─────────────────────────────────────────────────────────────

@Composable
private fun CurrentWeightCard(
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(8.dp, shape, clip = false)
            .clip(shape)
            .background(Color.White)
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
                color = Color(0xFF228C66),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                currentWeightKg?.let { "%.1f kg".format(it) } ?: "-- kg",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF228C66),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(Modifier.height(10.dp))

            // Thanh Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(VitalRadius.Pill))
                    .background(Color(0xFFE2E0DC))
            ) {
                // Giữ nguyên hàm mũ 1.3 của bạn, bọc thêm coerceIn để an toàn
                val amplifiedProgress = progress.toDouble().pow(1.3).toFloat().coerceIn(0f, 1f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth(amplifiedProgress)
                        .fillMaxHeight()
                        .background(Color(0xFF228C66))
                )
            }

            Spacer(Modifier.height(6.dp))
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(
                        color = Color(0xFF9A9A9A),
                        fontWeight = FontWeight.Normal
                    )) {
                        append("Mục tiêu ")
                    }
                    withStyle(style = SpanStyle(
                        color = Color(0xFF228C66),
                        fontWeight = FontWeight.Bold
                    )) {
                        val valueText = targetWeightKg?.let { "%.1f kg".format(it) } ?: "-- kg"
                        append(valueText)
                    }
                },
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }

        // Nút Cập nhật
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF228C66))
                .clickable(onClick = onLogWeight)
                .padding(horizontal = 16.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Cập nhật",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// BmiBar
// ─────────────────────────────────────────────────────────────

@Composable
private fun BmiBar(bmi: Float) {
    val minBmi = 10f
    val maxBmi = 40f
    val clampedBmi = bmi.coerceIn(minBmi, maxBmi)
    val thumbFraction = (clampedBmi - minBmi) / (maxBmi - minBmi)

    VitalCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Chỉ số BMI", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MetricGreen)
                Text(
                    text = "%.1f - %s".format(bmi, bmiLabel(bmi)),
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
                // ĐƯA THANH MÀU VÀ THUMB VÀO CHUNG MỘT BOX
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // 1. Thanh màu BMI làm nền phía dưới
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

                    // 2. Dùng Row + Spacer weight để đẩy Chấm tròn Indicator đè lên trên
                    // thumbFraction được giới hạn từ 0.0001f đến 0.9999f để tránh weight bị bằng 0 gây crash
                    val safeFraction = thumbFraction.coerceIn(0.0001f, 0.9999f)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Spacer này đóng vai trò "khoảng đẩy" bên trái
                        Spacer(modifier = Modifier.weight(safeFraction))

                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Ink900)
                                .border(2.dp, AppSurface, CircleShape)
                        )

                        // Spacer này đóng vai trò "khoảng bù" bên phải
                        Spacer(modifier = Modifier.weight(1f - safeFraction))
                    }
                }

                Spacer(Modifier.height(6.dp))

                // 3. Phần chữ Labels phân vùng nằm ở dưới thanh bar
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

// ─────────────────────────────────────────────────────────────
// PersonalInfoCard
// ─────────────────────────────────────────────────────────────

@Composable
private fun PersonalInfoCard(
    profile: HealthProfileDto?,
    onClick: () -> Unit
) {
    VitalCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                "Thông tin cá nhân",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MetricGreen
            )

            PersonalInfoRow(
                label = "Giới tính",
                value = formatGender(profile?.gender)
            )
            PersonalInfoRow(
                label = "Cân nặng ban đầu",
                value = profile?.initialWeightKg?.let { "%.1f kg".format(it) } ?: "--"
            )
            PersonalInfoRow(
                label = "Chiều cao",
                value = profile?.heightCm?.let { "%.0f cm".format(it) } ?: "--"
            )
            PersonalInfoRow(
                label = "Tuổi",
                value = calculateAge(profile?.birthDate)?.let { "$it tuổi" } ?: "--"
            )
        }
    }
}

@Composable
private fun PersonalInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = Ink500)
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MetricGreen,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun EnergyBasicsCard(latest: BodyMetricDto) {
    VitalCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                "Năng lượng cơ bản",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MetricGreen
            )
            latest.bmr?.let { bmr ->
                PersonalInfoRow(
                    label = "BMR",
                    value = "%.0f kcal/ngày".format(bmr)
                )
            }
            latest.tdee?.let { tdee ->
                PersonalInfoRow(
                    label = "TDEE",
                    value = "%.0f kcal/ngày".format(tdee)
                )
            }
        }
    }
}

@Composable
private fun InputTopLabel(label: String) {
    Text(
        label,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = Ink500,
        modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
    )
}

@Composable
private fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    keyboardType: KeyboardType = KeyboardType.Decimal
) {
    Column(modifier = modifier) {
        InputTopLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            suffix = suffix?.let { { Text(it, color = Ink500, fontSize = 12.sp) } },
            modifier = Modifier.fillMaxWidth(),
            colors = metricTextFieldColors()
        )
    }
}

@Composable
private fun metricTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MetricGreen,
    focusedLabelColor = MetricGreen,
    cursorColor = MetricGreen,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    disabledContainerColor = Color.White,
    errorContainerColor = Color.White
)

// ─────────────────────────────────────────────────────────────
// WeightChangeRow
// ─────────────────────────────────────────────────────────────

@Composable
private fun WeightChangeRow(
    weightChange: Float,
    latestDate: String?,
    startWeight: Float,
    currentWeight: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    VitalCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Tiến độ cân nặng", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF228C66))
            latestDate?.let { date ->
                Text(
                    "Cập nhật: ${formatDateDisplay(date)}",
                    fontSize = 11.sp,
                    color = Ink400
                )
            }
            val arrow = if (weightChange <= 0f) "↓" else "↑"
            val changeColor = if (weightChange <= 0f) Color(0xFF228C66) else Color(0xFFF87171)
            Text(
                "$arrow %.1f kg".format(kotlin.math.abs(weightChange)),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = changeColor,
                maxLines = 1
            )
            Text(
                "%.1f → %.1f kg".format(startWeight, currentWeight),
                fontSize = 12.sp,
                color = Ink500,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun WeightProgressEmptyCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    VitalCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Tiến độ cân nặng", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MetricGreen)
            Text("Chưa có dữ liệu", fontSize = 12.sp, color = Ink500)
            Text(
                "-- kg",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Ink400
            )
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
    var weekRangeText   by remember { mutableStateOf("") }
    var selectedMetric  by remember { mutableStateOf<BodyMetricDto?>(null) }
    var selectedDayIdx  by remember { mutableStateOf<Int?>(null) }
    var chartWidthPx    by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current

    VitalCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionHeader(title = "Biểu đồ cân nặng", color = MetricGreen)
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
                            .background(if (isSelected) Color(0xFF228C66) else Color.Transparent)
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
                // Stats row: chiều cao cố định bằng placeholder vô hình để không bị layout shift
                val idx    = selectedDayIdx
                val metric = selectedMetric
                val showTooltip = selectedPeriod == "week" && idx != null && metric != null && chartWidthPx > 0f
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Placeholder luôn hiện (vô hình) → giữ chiều cao Box = chiều cao tooltip card
                    Column(modifier = Modifier.alpha(0f).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text("99.9 kg", fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 15.sp)
                        Text("99/99/9999", fontSize = 8.sp, lineHeight = 11.sp)
                    }

                    if (showTooltip) {
                        val parentWDp = with(density) { chartWidthPx.toDp() }
                        val chartWDp  = parentWDp - 8.dp - 46.dp
                        val colWDp    = chartWDp / 7f
                        val cardWDp   = chartWDp * 2f / 7f
                        val centerX   = 8.dp + colWDp * (idx!! + 0.5f)
                        val cardLeft  = (centerX - cardWDp / 2f).coerceIn(0.dp, parentWDp - cardWDp)
                        Box(
                            modifier = Modifier
                                .offset(x = cardLeft)
                                .width(cardWDp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Ink900)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Column {
                                Text(
                                    "%.1f kg".format(metric!!.weightKg),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 15.sp
                                )
                                Text(
                                    formatDateDisplay(metric.date),
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontSize = 8.sp,
                                    lineHeight = 11.sp
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth().matchParentSize(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("TB: ${"%.1f".format(periodData.avgWeight)} kg", fontSize = 12.sp, color = Ink500)
                            if (selectedPeriod == "week" && weekRangeText.isNotEmpty()) {
                                Text(weekRangeText, fontSize = 11.sp, color = Ink400)
                            }
                            Text("↓%.1f / ↑%.1f kg".format(periodData.minWeight, periodData.maxWeight), fontSize = 12.sp, color = Ink500)
                        }
                    }
                }
                Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
                    val selIdx = selectedDayIdx
                    if (selIdx != null && size.width > 0f) {
                        val lp    = 8.dp.toPx()
                        val rp    = 46.dp.toPx()
                        val connX = lp + (selIdx + 0.5f) * ((size.width - lp - rp) / 7f)
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.35f),
                            start = Offset(connX, 0f),
                            end   = Offset(connX, size.height),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }
                }
                WeightLineChart(
                    data = periodData.data,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .onSizeChanged { chartWidthPx = it.width.toFloat() },
                    period = selectedPeriod,
                    onWeekRangeChange = { weekRangeText = it },
                    onDaySelected = { i, m ->
                        selectedDayIdx = i
                        selectedMetric = m
                    }
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
private fun WeightLineChart(
    data: List<BodyMetricDto>,
    modifier: Modifier = Modifier,
    period: String = "week",
    onWeekRangeChange: ((String) -> Unit)? = null,
    onDaySelected: ((Int?, BodyMetricDto?) -> Unit)? = null
) {
    val pointsByDate = remember(data) {
        data.mapNotNull { metric ->
            metric.metricLocalDateOrNull()?.let { it to metric }
        }.distinctBy { it.first }.sortedBy { it.first }
    }
    if (pointsByDate.size < 2) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Cần ít nhất 2 điểm dữ liệu", color = Ink500, fontSize = 12.sp)
        }
        return
    }

    val weights = pointsByDate.map { it.second.weightKg }
    val minW = weights.minOrNull() ?: 0f
    val maxW = weights.maxOrNull() ?: 0f
    val dataRange = (maxW - minW).coerceAtLeast(1f)

    // Làm tròn trục Y thành bội của 10
    val niceMin = (floor((minW - dataRange * 0.05) / 10.0) * 10).toFloat()
    val niceMax = (ceil((maxW + dataRange * 0.05) / 10.0) * 10).toFloat()
    val niceRange = (niceMax - niceMin).coerceAtLeast(10f)
    val tickStep = (ceil(niceRange / 20.0) * 10).toFloat()
    val axisMin = niceMin
    val axisMax = niceMin + 2f * tickStep
    val axisRange = axisMax - axisMin

    val textMeasurer = rememberTextMeasurer()
    val axisLabelStyle = TextStyle(fontSize = 9.sp, color = Color(0xFF6B7280))

    if (period == "week") {
        var dayOffset by remember { mutableIntStateOf(0) }
        val dragX  = remember { Animatable(0f) }
        val scope  = rememberCoroutineScope()
        var rawDrag by remember { mutableFloatStateOf(0f) }
        val dragXValue = rawDrag + dragX.value

        val baseMonday = remember(pointsByDate) { pointsByDate.last().first.with(DayOfWeek.MONDAY) }
        val startDay   = remember(baseMonday, dayOffset) { baseMonday.plusDays(dayOffset.toLong()) }
        val prevStart  = remember(startDay) { startDay.minusDays(7) }
        val nextStart  = remember(startDay) { startDay.plusDays(7) }

        // Nhãn thứ theo ngày thực tế của cửa sổ hiện tại
        val dayLabels = remember(startDay) {
            val vn = mapOf(
                DayOfWeek.MONDAY to "T2", DayOfWeek.TUESDAY to "T3",
                DayOfWeek.WEDNESDAY to "T4", DayOfWeek.THURSDAY to "T5",
                DayOfWeek.FRIDAY to "T6", DayOfWeek.SATURDAY to "T7",
                DayOfWeek.SUNDAY to "CN"
            )
            (0..6).map { d -> vn[startDay.plusDays(d.toLong()).dayOfWeek] ?: "" }
        }

        fun buildMap(ref: LocalDate): Map<Int, BodyMetricDto> = pointsByDate
            .groupBy { (date, _) -> java.time.temporal.ChronoUnit.DAYS.between(ref, date).toInt() }
            .filterKeys { it in 0..6 }
            .mapValues { (_, list) -> list.maxByOrNull { it.first }!!.second }

        val prevMap = remember(pointsByDate, prevStart) { buildMap(prevStart) }
        val curMap  = remember(pointsByDate, startDay)  { buildMap(startDay) }
        val nextMap = remember(pointsByDate, nextStart) { buildMap(nextStart) }

        // Giới hạn: Mon của tuần đầu tiên/cuối cùng có data
        val minStartDay = remember(pointsByDate) { pointsByDate.first().first.with(DayOfWeek.MONDAY) }
        val maxStartDay = remember(pointsByDate) { pointsByDate.last().first.with(DayOfWeek.MONDAY) }
        val minStartDayState = rememberUpdatedState(minStartDay)
        val maxStartDayState = rememberUpdatedState(maxStartDay)

        // Day-level: còn lướt được không (chưa chạm giới hạn tuần đầu/cuối)
        val hasPrevDay = startDay > minStartDay
        val hasNextDay = startDay < maxStartDay
        val hasPrevDayState = rememberUpdatedState(hasPrevDay)
        val hasNextDayState = rememberUpdatedState(hasNextDay)

        // Fast swipe: luôn nhảy đến Mon của tuần kế (Mon-CN)
        val prevWeekMonday = remember(startDay) { startDay.with(DayOfWeek.MONDAY).minusWeeks(1) }
        val nextWeekMonday = remember(startDay) { startDay.with(DayOfWeek.MONDAY).plusWeeks(1) }
        val prevWeekMondayState = rememberUpdatedState(prevWeekMonday)
        val nextWeekMondayState = rememberUpdatedState(nextWeekMonday)
        val hasPrevState = rememberUpdatedState(prevWeekMonday >= minStartDay)
        val hasNextState = rememberUpdatedState(nextWeekMonday <= maxStartDay)

        val curMapState            = rememberUpdatedState(curMap)
        val onDaySelectedState     = rememberUpdatedState(onDaySelected)
        val onWeekRangeChangeState = rememberUpdatedState(onWeekRangeChange)
        val startDayState          = rememberUpdatedState(startDay)

        var selectedDayIdx by remember { mutableStateOf<Int?>(null) }
        LaunchedEffect(dayOffset) {
            selectedDayIdx = null
            onDaySelectedState.value?.invoke(null, null)
        }

        val weekRangeText = remember(startDay) {
            val endDay = startDay.plusDays(6)
            if (startDay.month == endDay.month)
                "${startDay.dayOfMonth}-${endDay.dayOfMonth} thg ${endDay.monthValue}, ${endDay.year}"
            else
                "${startDay.dayOfMonth} thg ${startDay.monthValue} - ${endDay.dayOfMonth} thg ${endDay.monthValue}, ${endDay.year}"
        }
        LaunchedEffect(weekRangeText) { onWeekRangeChangeState.value?.invoke(weekRangeText) }

        fun formatRange(m: LocalDate): String {
            val s = m.plusDays(6)
            return if (m.month == s.month)
                "${m.dayOfMonth}-${s.dayOfMonth} thg ${s.monthValue}, ${s.year}"
            else
                "${m.dayOfMonth} thg ${m.monthValue} - ${s.dayOfMonth} thg ${s.monthValue}, ${s.year}"
        }

        Box(
            modifier = modifier
                .pointerInput(Unit) {
                val leftPad = 8.dp.toPx()
                val rightPad = 46.dp.toPx()
                val pageW = size.width.toFloat() - leftPad - rightPad
                val colW  = pageW / 7f

                awaitEachGesture {
                    val velocityTracker = VelocityTracker()
                    val down = awaitFirstDown(requireUnconsumed = false)
                    velocityTracker.addPosition(down.uptimeMillis, down.position)
                    var dragging = false

                    while (true) {
                        val event  = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break

                        if (!change.pressed) {
                            if (dragging) {
                                val velocity = velocityTracker.calculateVelocity().x
                                scope.launch {
                                    val cur = rawDrag + dragX.value
                                    dragX.snapTo(cur)
                                    rawDrag = 0f
                                    val dayShift = (-cur / colW).roundToInt().coerceIn(-7, 7)
                                    when {
                                        // Fast swipe → Mon của tuần kế (mép luôn là Mon-CN)
                                        // Animate đúng số ngày đến Monday mục tiêu (không phải ±pageW)
                                        // → tại vị trí cuối animation, column tương ứng đã ở đúng col 0 → snapTo(0f) không gây giật
                                        velocity < -300f && hasNextState.value -> {
                                            val target = nextWeekMondayState.value
                                            val days   = java.time.temporal.ChronoUnit.DAYS.between(startDayState.value, target).toInt()
                                            onWeekRangeChangeState.value?.invoke(formatRange(target))
                                            onDaySelectedState.value?.invoke(null, null)
                                            dragX.animateTo(-(days * colW), spring(dampingRatio = 1f, stiffness = 800f))
                                            dayOffset = java.time.temporal.ChronoUnit.DAYS.between(baseMonday, target).toInt()
                                            dragX.snapTo(0f)
                                        }
                                        velocity > 300f && hasPrevState.value -> {
                                            val target = prevWeekMondayState.value
                                            val days   = java.time.temporal.ChronoUnit.DAYS.between(target, startDayState.value).toInt()
                                            onWeekRangeChangeState.value?.invoke(formatRange(target))
                                            onDaySelectedState.value?.invoke(null, null)
                                            dragX.animateTo(days * colW, spring(dampingRatio = 1f, stiffness = 800f))
                                            dayOffset = java.time.temporal.ChronoUnit.DAYS.between(baseMonday, target).toInt()
                                            dragX.snapTo(0f)
                                        }
                                        // Slow drag → chuyển theo ngày, clamp về giới hạn
                                        dayShift >= 1 && hasNextDayState.value -> {
                                            val maxOff = java.time.temporal.ChronoUnit.DAYS.between(baseMonday, maxStartDayState.value).toInt()
                                            val shift  = dayShift.coerceAtMost(maxOff - dayOffset).coerceAtLeast(1)
                                            onWeekRangeChangeState.value?.invoke(formatRange(baseMonday.plusDays((dayOffset + shift).toLong())))
                                            onDaySelectedState.value?.invoke(null, null)
                                            dragX.animateTo(-(shift * colW), spring(dampingRatio = 1f, stiffness = 800f))
                                            dayOffset += shift; dragX.snapTo(0f)
                                        }
                                        dayShift <= -1 && hasPrevDayState.value -> {
                                            val minOff = java.time.temporal.ChronoUnit.DAYS.between(baseMonday, minStartDayState.value).toInt()
                                            val shift  = dayShift.coerceAtLeast(minOff - dayOffset).coerceAtMost(-1)
                                            onWeekRangeChangeState.value?.invoke(formatRange(baseMonday.plusDays((dayOffset + shift).toLong())))
                                            onDaySelectedState.value?.invoke(null, null)
                                            dragX.animateTo(-(shift * colW), spring(dampingRatio = 1f, stiffness = 800f))
                                            dayOffset += shift; dragX.snapTo(0f)
                                        }
                                        else -> dragX.animateTo(0f, spring(dampingRatio = 0.65f, stiffness = 300f))
                                    }
                                }
                            } else if (!dragX.isRunning) {
                                // Tap: chỉ khi không có animation navigation đang chạy
                                val tapDx = change.position.x - down.position.x
                                val tapDy = change.position.y - down.position.y
                                if (kotlin.math.abs(tapDx) < viewConfiguration.touchSlop &&
                                    kotlin.math.abs(tapDy) < viewConfiguration.touchSlop) {
                                    val colW = pageW / 7f
                                    val dayIdx = ((down.position.x - leftPad) / colW).toInt().coerceIn(0, 6)
                                    val map = curMapState.value
                                    val newIdx = if (map.containsKey(dayIdx))
                                        if (selectedDayIdx == dayIdx) null else dayIdx
                                    else null
                                    selectedDayIdx = newIdx
                                    onDaySelectedState.value?.invoke(newIdx, newIdx?.let { map[it] })
                                }
                            }
                            break
                        }

                        val totalDx = change.position.x - down.position.x
                        val totalDy = change.position.y - down.position.y

                        if (!dragging) {
                            when {
                                kotlin.math.abs(totalDx) > viewConfiguration.touchSlop &&
                                kotlin.math.abs(totalDx) >= kotlin.math.abs(totalDy) -> {
                                    dragging = true
                                    selectedDayIdx = null
                                    onDaySelectedState.value?.invoke(null, null)
                                    change.consume()
                                }
                                kotlin.math.abs(totalDy) > viewConfiguration.touchSlop -> break
                            }
                        }

                        if (dragging) {
                            change.consume()
                            val dx = change.position.x - change.previousPosition.x
                            val elastic = (dx > 0f && !hasPrevDayState.value) || (dx < 0f && !hasNextDayState.value)
                            // Cập nhật trực tiếp — không qua coroutine, không có hàng đợi tồn đọng
                            rawDrag += if (elastic) dx * 0.3f else dx
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                        }
                    }
                }
            }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val leftPadding   = 8.dp.toPx()
                val rightPadding  = 46.dp.toPx()
                val topPadding    = 12.dp.toPx()
                val tickExtend    = 6.dp.toPx()
                val labelHeight   = 16.dp.toPx()
                val bottomPadding = tickExtend + labelHeight + 4.dp.toPx()
                val chartW        = w - leftPadding - rightPadding
                val chartH        = h - topPadding - bottomPadding
                val columnWidth   = chartW / 7f
                val pageW         = chartW
                val chartBottom   = topPadding + chartH

                fun yForWeight(wt: Float) = topPadding + (1f - (wt - axisMin) / axisRange) * chartH

                val gridColor  = Ink200.copy(alpha = 0.75f)
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()), 0f)
                val lineColor  = Color(0xFF228C66)
                val lineWidth  = 2.dp.toPx()
                val dotRadius  = 4.dp.toPx()

                // === Trục Y cố định ===
                repeat(3) { idx ->
                    val fraction = idx / 2f
                    val y = topPadding + fraction * chartH
                    drawLine(gridColor, Offset(leftPadding, y), Offset(w - rightPadding, y), strokeWidth = 1.dp.toPx())
                    val wLabel = axisMax - fraction * axisRange
                    val mLabel = textMeasurer.measure("%.0f".format(wLabel), axisLabelStyle)
                    drawText(mLabel, topLeft = Offset(w - rightPadding + 5.dp.toPx(), y - mLabel.size.height / 2f))
                }

                val pageOffsets = listOf(dragXValue - pageW, dragXValue, dragXValue + pageW)
                val pageMaps    = listOf(prevMap, curMap, nextMap)

                // Tính trước điểm dữ liệu để dùng nhiều lần
                val pagePts = pageOffsets.zip(pageMaps).map { (tx, dayMap) ->
                    (0..6).mapNotNull { d ->
                        dayMap[d]?.let { m -> Offset(tx + leftPadding + (d + 0.5f) * columnWidth, yForWeight(m.weightKg)) }
                    }
                }
                val prevLastPt  = pagePts[0].lastOrNull()
                val curFirstPt  = pagePts[1].firstOrNull()
                val curLastPt   = pagePts[1].lastOrNull()
                val nextFirstPt = pagePts[2].firstOrNull()

                clipRect(leftPadding, 0f, w - rightPadding, h) {

                    // === PASS 1: Nền — cột nét đứt + nhãn thứ ===
                    pageOffsets.forEach { tx ->
                        for (i in 0..7) {
                            val x = tx + leftPadding + i * columnWidth
                            drawLine(Ink200.copy(alpha = 0.9f), Offset(x, topPadding),
                                Offset(x, chartBottom + tickExtend), 1.dp.toPx(), pathEffect = dashEffect)
                        }
                        for (d in 0..6) {
                            val cx = tx + leftPadding + (d + 0.5f) * columnWidth
                            val m  = textMeasurer.measure(dayLabels[d], axisLabelStyle)
                            drawText(m, topLeft = Offset(cx - m.size.width / 2f, chartBottom + tickExtend + 3.dp.toPx()))
                        }
                    }

                    // === Đường kẻ xám chỉ ngày được chọn ===
                    selectedDayIdx?.let { dayIdx ->
                        val x = leftPadding + (dayIdx + 0.5f) * columnWidth
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.35f),
                            start = Offset(x, 0f),
                            end   = Offset(x, chartBottom),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }

                    // === PASS 2: Fill (trong trang + xuyên trang) ===
                    pagePts.forEach { pts ->
                        if (pts.size >= 2) drawPath(
                            Path().apply { moveTo(pts.first().x, chartBottom); pts.forEach { lineTo(it.x, it.y) }; lineTo(pts.last().x, chartBottom); close() },
                            color = lineColor.copy(alpha = 0.15f)
                        )
                    }
                    if (prevLastPt != null && curFirstPt != null) drawPath(
                        Path().apply { moveTo(prevLastPt.x, chartBottom); lineTo(prevLastPt.x, prevLastPt.y); lineTo(curFirstPt.x, curFirstPt.y); lineTo(curFirstPt.x, chartBottom); close() },
                        color = lineColor.copy(alpha = 0.15f)
                    )
                    if (curLastPt != null && nextFirstPt != null) drawPath(
                        Path().apply { moveTo(curLastPt.x, chartBottom); lineTo(curLastPt.x, curLastPt.y); lineTo(nextFirstPt.x, nextFirstPt.y); lineTo(nextFirstPt.x, chartBottom); close() },
                        color = lineColor.copy(alpha = 0.15f)
                    )

                    // === PASS 3: Đường kẻ (trong trang + xuyên trang) ===
                    pagePts.forEach { pts ->
                        if (pts.size >= 2) drawPath(
                            Path().apply { moveTo(pts.first().x, pts.first().y); pts.drop(1).forEach { lineTo(it.x, it.y) } },
                            color = lineColor, style = Stroke(width = lineWidth)
                        )
                    }
                    if (prevLastPt != null && curFirstPt != null)
                        drawLine(lineColor, prevLastPt, curFirstPt, strokeWidth = lineWidth)
                    if (curLastPt != null && nextFirstPt != null)
                        drawLine(lineColor, curLastPt, nextFirstPt, strokeWidth = lineWidth)

                    // === PASS 4: Điểm tròn luôn ở trên cùng ===
                    pagePts.forEach { pts ->
                        pts.forEach { pt ->
                            drawCircle(lineColor, dotRadius, pt)
                            drawCircle(Color.White, 2.dp.toPx(), pt)
                        }
                    }
                }
            }

        }
    } else {
        val firstDate = pointsByDate.first().first
        val lastDate = pointsByDate.last().first
        val totalDays = java.time.temporal.ChronoUnit.DAYS.between(firstDate, lastDate).toFloat().coerceAtLeast(1f)

        Column(modifier = modifier) {
            Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
                val w = size.width
                val h = size.height
                val leftPadding = 8.dp.toPx()
                val rightPadding = 46.dp.toPx()
                val topPadding = 12.dp.toPx()
                val bottomPadding = 8.dp.toPx()
                val chartW = w - leftPadding - rightPadding
                val chartH = h - topPadding - bottomPadding

                val points = pointsByDate.map { (date, metric) ->
                    val dayOffset = java.time.temporal.ChronoUnit.DAYS.between(firstDate, date).toFloat()
                    val x = leftPadding + dayOffset / totalDays * chartW
                    val y = topPadding + (1f - (metric.weightKg - axisMin) / axisRange) * chartH
                    Offset(x, y)
                }

                val gridColor = Ink200.copy(alpha = 0.75f)

                repeat(3) { index ->
                    val fraction = index / 2f
                    val y = topPadding + fraction * chartH
                    drawLine(gridColor, Offset(leftPadding, y), Offset(w - rightPadding, y), strokeWidth = 1.dp.toPx())
                    val weightAtLine = axisMax - fraction * axisRange
                    val measured = textMeasurer.measure("%.0f".format(weightAtLine), axisLabelStyle)
                    drawText(measured, topLeft = Offset(w - rightPadding + 5.dp.toPx(), y - measured.size.height / 2f))
                }

                drawPath(
                    Path().apply {
                        moveTo(points.first().x, topPadding + chartH)
                        points.forEach { lineTo(it.x, it.y) }
                        lineTo(points.last().x, topPadding + chartH)
                        close()
                    },
                    color = Color(0xFF228C66).copy(alpha = 0.15f)
                )
                drawPath(
                    Path().apply {
                        moveTo(points.first().x, points.first().y)
                        points.drop(1).forEach { lineTo(it.x, it.y) }
                    },
                    color = Color(0xFF228C66),
                    style = Stroke(width = 2.dp.toPx())
                )

                points.forEach { pt ->
                    drawCircle(color = Color(0xFF228C66), radius = 4.dp.toPx(), center = pt)
                    drawCircle(color = Color.White, radius = 2.dp.toPx(), center = pt)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(firstDate.format(DateTimeFormatter.ofPattern("dd/MM", Locale("vi"))), color = Ink500, fontSize = 10.sp)
                Text(lastDate.format(DateTimeFormatter.ofPattern("dd/MM", Locale("vi"))), color = Ink500, fontSize = 10.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// BodyMeasurementsCard
// ─────────────────────────────────────────────────────────────

@Composable
private fun BodyMeasurementsCard(
    latest: BodyMetricDto,
    onClick: () -> Unit
) {
    val measurements = buildList {
        latest.bodyFatPct?.let { add("Tỷ lệ mỡ" to "%.1f%%".format(it)) }
        latest.waistCm?.let { add("Vòng eo" to "%.0f cm".format(it)) }
        latest.hipCm?.let { add("Vòng hông" to "%.0f cm".format(it)) }
        latest.chestCm?.let { add("Vòng ngực" to "%.0f cm".format(it)) }
        latest.armCm?.let { add("Bắp tay" to "%.0f cm".format(it)) }
        latest.neckCm?.let { add("Vòng cổ" to "%.0f cm".format(it)) }
    }

    VitalCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        SectionHeader(title = "Số đo cơ thể", color = MetricGreen)
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
            SectionHeader(title = "Ảnh tiến độ", color = MetricGreen)
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
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MetricGreen),
            border = androidx.compose.foundation.BorderStroke(1.dp, MetricGreen)
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                when (uiState.updateSheetTab) {
                    0 -> BasicUpdateTab(uiState = uiState, viewModel = viewModel, onDone = onDismiss)
                    1 -> AdvancedUpdateTab(uiState = uiState, viewModel = viewModel, onDone = onDismiss)
                    2 -> PhotoUpdateTab(viewModel = viewModel, photos = uiState.photos, onDone = onDismiss)
                    3 -> PersonalInfoUpdateTab(uiState = uiState, viewModel = viewModel, onDone = onDismiss)
                }
            }
        }
    }
}

@Composable
private fun BasicUpdateTab(uiState: MetricsUiState, viewModel: MetricsViewModel, onDone: () -> Unit) {
    val latest = uiState.latest
    var weightStr by remember(latest?.weightKg) {
        mutableStateOf(latest?.weightKg?.let { "%.1f".format(it) }.orEmpty())
    }
    val weightVal = weightStr.toFloatOrNull()
    val heightCm = uiState.healthProfile?.heightCm
    val bmiPreview = calculateBmi(weightVal, heightCm)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Cập nhật cân nặng", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MetricGreen)

        LabeledTextField(
            label = "Cân nặng",
            value = weightStr,
            onValueChange = { weightStr = it },
            suffix = "kg",
            modifier = Modifier.fillMaxWidth()
        )

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
                            heightCm = heightCm
                        )
                    )
                    onDone()
                }
            },
            enabled = weightVal != null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(VitalRadius.Md),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF228C66))
        ) {
            Text("Lưu thông tin", fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PersonalInfoUpdateTab(uiState: MetricsUiState, viewModel: MetricsViewModel, onDone: () -> Unit) {
    val profile = uiState.healthProfile
    var genderExpanded by remember { mutableStateOf(false) }
    var gender by remember(profile?.gender) { mutableStateOf(normalizeGender(profile?.gender)) }
    var ageStr by remember(profile?.birthDate) {
        mutableStateOf(calculateAge(profile?.birthDate)?.toString().orEmpty())
    }
    var heightStr by remember(profile?.heightCm) {
        mutableStateOf(profile?.heightCm?.let { "%.0f".format(it) }.orEmpty())
    }
    var initialWeightStr by remember(profile?.initialWeightKg) {
        mutableStateOf(profile?.initialWeightKg?.let { "%.1f".format(it) }.orEmpty())
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Cập nhật thông tin cá nhân", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MetricGreen)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                InputTopLabel("Giới tính")
                ExposedDropdownMenuBox(
                    expanded = genderExpanded,
                    onExpandedChange = { genderExpanded = !genderExpanded }
                ) {
                    OutlinedTextField(
                        value = genderDisplay(gender),
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = metricTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = genderExpanded,
                        onDismissRequest = { genderExpanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        listOf("male" to "Nam", "female" to "Nữ", "other" to "Khác").forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    gender = value
                                    genderExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            LabeledTextField(
                label = "Tuổi",
                value = ageStr,
                onValueChange = { ageStr = it.filter(Char::isDigit) },
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LabeledTextField(
                label = "Chiều cao",
                value = heightStr,
                onValueChange = { heightStr = it },
                suffix = "cm",
                modifier = Modifier.weight(1f)
            )
            LabeledTextField(
                label = "Cân nặng ban đầu",
                value = initialWeightStr,
                onValueChange = { initialWeightStr = it },
                suffix = "kg",
                modifier = Modifier.weight(1f)
            )
        }

        Button(
            onClick = {
                val updated = (profile ?: HealthProfileDto()).copy(
                    gender = gender,
                    birthDate = ageStr.toIntOrNull()?.let { ageToBirthDate(it, profile?.birthDate) } ?: profile?.birthDate,
                    heightCm = heightStr.toFloatOrNull(),
                    initialWeightKg = initialWeightStr.toFloatOrNull()
                )
                viewModel.updateHealthProfile(updated)
                onDone()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(VitalRadius.Md),
            colors = ButtonDefaults.buttonColors(containerColor = MetricGreen)
        ) {
            Text("Lưu thông tin", fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

private data class AdvancedField(
    val label: String,
    val value: String,
    val suffix: String,
    val onChange: (String) -> Unit
)

@Composable
private fun AdvancedUpdateTab(uiState: MetricsUiState, viewModel: MetricsViewModel, onDone: () -> Unit) {
    val latest = uiState.latest
    var bodyFatStr by remember(latest?.bodyFatPct) {
        mutableStateOf(latest?.bodyFatPct?.let { "%.1f".format(it) }.orEmpty())
    }
    var waistStr by remember(latest?.waistCm) {
        mutableStateOf(latest?.waistCm?.let { "%.0f".format(it) }.orEmpty())
    }
    var hipStr by remember(latest?.hipCm) {
        mutableStateOf(latest?.hipCm?.let { "%.0f".format(it) }.orEmpty())
    }
    var chestStr by remember(latest?.chestCm) {
        mutableStateOf(latest?.chestCm?.let { "%.0f".format(it) }.orEmpty())
    }
    var armStr by remember(latest?.armCm) {
        mutableStateOf(latest?.armCm?.let { "%.0f".format(it) }.orEmpty())
    }
    var neckStr by remember(latest?.neckCm) {
        mutableStateOf(latest?.neckCm?.let { "%.0f".format(it) }.orEmpty())
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Số đo nâng cao", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MetricGreen)
        Text("Tất cả trường đều tùy chọn", fontSize = 12.sp, color = Ink500)

        val fieldDefs = listOf(
            AdvancedField("% Mỡ cơ thể", bodyFatStr, "%") { v: String -> bodyFatStr = v },
            AdvancedField("Vòng eo", waistStr, "cm") { v: String -> waistStr = v },
            AdvancedField("Vòng hông", hipStr, "cm") { v: String -> hipStr = v },
            AdvancedField("Vòng ngực", chestStr, "cm") { v: String -> chestStr = v },
            AdvancedField("Bắp tay", armStr, "cm") { v: String -> armStr = v },
            AdvancedField("Vòng cổ", neckStr, "cm") { v: String -> neckStr = v }
        )

        fieldDefs.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { field ->
                    LabeledTextField(
                        label = field.label,
                        value = field.value,
                        onValueChange = field.onChange,
                        suffix = field.suffix,
                        modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Decimal
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
                        neckCm = neckStr.toFloatOrNull(),
                        armCm = armStr.toFloatOrNull()
                    )
                )
                onDone()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(VitalRadius.Md),
            colors = ButtonDefaults.buttonColors(containerColor = MetricGreen)
        ) {
            Text("Lưu số đo", fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

private fun BodyMetricDto.metricLocalDateOrNull(): LocalDate? {
    return runCatching { LocalDate.parse(date.take(10)) }.getOrNull()
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

private fun formatGender(gender: String?): String = when (gender?.lowercase(Locale.US)) {
    "male", "nam" -> "Nam"
    "female", "nu", "nữ" -> "Nữ"
    "other", "khac", "khác" -> "Khác"
    null, "" -> "--"
    else -> gender
}

private fun normalizeGender(gender: String?): String = when (gender?.lowercase(Locale.US)) {
    "male", "nam" -> "male"
    "female", "nu", "nữ" -> "female"
    "other", "khac", "khác" -> "other"
    else -> "male"
}

private fun genderDisplay(gender: String): String = when (gender) {
    "male" -> "Nam"
    "female" -> "Nữ"
    "other" -> "Khác"
    else -> formatGender(gender)
}

private fun calculateAge(birthDate: String?): Int? {
    val date = birthDate?.takeIf { it.isNotBlank() } ?: return null
    return runCatching {
        Period.between(LocalDate.parse(date.take(10)), LocalDate.now()).years
    }.getOrNull()?.takeIf { it >= 0 }
}

private fun ageToBirthDate(age: Int, currentBirthDate: String?): String {
    val today = LocalDate.now()
    val currentMonthDay = runCatching { LocalDate.parse(currentBirthDate?.take(10)).let { it.monthValue to it.dayOfMonth } }.getOrNull()
    val month = currentMonthDay?.first ?: today.monthValue
    val day = currentMonthDay?.second ?: today.dayOfMonth
    val candidate = runCatching { LocalDate.of(today.year - age, month, day) }
        .getOrElse { LocalDate.of(today.year - age, month, 1) }
    return candidate.toString()
}

private fun calculateBmi(weightKg: Float?, heightCm: Float?): Float? {
    if (weightKg == null || heightCm == null || heightCm <= 0f) return null
    val heightM = heightCm / 100f
    return weightKg / (heightM * heightM)
}

private fun calculateWeightGoalProgress(
    initialWeightKg: Float?,
    currentWeightKg: Float?,
    targetWeightKg: Float?
): Float {
    if (currentWeightKg == null || targetWeightKg == null || targetWeightKg <= 0f) return 0f
    if (initialWeightKg != null && initialWeightKg != targetWeightKg) {
        val totalChange = kotlin.math.abs(targetWeightKg - initialWeightKg)
        val currentChange = kotlin.math.abs(currentWeightKg - initialWeightKg)
        return (currentChange / totalChange).coerceIn(0.04f, 1f)
    }
    return (currentWeightKg / targetWeightKg).coerceIn(0.04f, 1f)
}

private fun formatPhotoType(type: String): String = when (type) {
    "front" -> "Mặt trước"
    "back" -> "Mặt sau"
    "side" -> "Mặt bên"
    else -> type
}

private fun formatDateDisplay(dateStr: String): String {
    val date = runCatching {
        Instant.parse(dateStr).atZone(ZoneId.systemDefault()).toLocalDate()
    }.recoverCatching {
        LocalDate.parse(dateStr.take(10))
    }.getOrNull()

    return if (date != null) {
        "%02d/%02d/%04d".format(date.dayOfMonth, date.monthValue, date.year)
    } else {
        dateStr
    }
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
        Log.e("UriToFile", "Lỗi khi chuyển đổi Uri sang File: ${e.localizedMessage}", e)
        null
    }
}
