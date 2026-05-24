package com.vitalai.ui.screens.metrics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import android.widget.Toast
import com.vitalai.data.remote.model.BodyMetricDto
import com.vitalai.data.remote.model.HealthProfileDto
import com.vitalai.data.remote.model.UpsertBodyMetricRequest
import com.vitalai.navigation.Screen
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricsScreen(
    navController: NavController,
    viewModel: MetricsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showAddMetricDialog by remember { mutableStateOf(false) }
    var editingSection by remember { mutableStateOf<HealthProfileSection?>(null) }

    LaunchedEffect(uiState.updateSuccessCount) {
        if (uiState.updateSuccessCount > 0) {
            Toast.makeText(context, "Cập nhật thông tin thành công", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Số liệu cơ thể", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddMetricDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Thêm số liệu", tint = Mint500)
                    }
                    TextButton(onClick = { navController.navigate(Screen.MetricsHistory) }) {
                        Text("Lịch sử", color = Mint500, fontSize = 13.sp)
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
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
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

                uiState.summary?.let { summary ->
                    Card(
                        shape = RoundedCornerShape(VitalRadius.Xl),
                        colors = CardDefaults.cardColors(containerColor = AppSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AppLine),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Tiến độ cân nặng", fontWeight = FontWeight.Bold, color = Ink900)
                                Text("${summary.totalRecords} lần cập nhật", fontSize = 12.sp, color = Ink500)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "%+.1f kg".format(summary.weightChange),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (summary.weightChange <= 0f) Mint500 else MacroProtein
                                )
                                Text("${summary.startWeight} -> ${summary.currentWeight} kg", fontSize = 12.sp, color = Ink500)
                            }
                        }
                    }
                }

                // Period selector
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50),
                    color = AppSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppLine)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        listOf(
                            "week" to "1 tuần",
                            "month" to "1 tháng",
                            "3months" to "3 tháng",
                            "6months" to "6 tháng",
                            "year" to "1 năm"
                        ).forEach { (key, label) ->
                            val isSelected = uiState.selectedPeriod == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isSelected) Mint500 else Color.Transparent)
                                    .clickable { viewModel.selectPeriod(key) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    color = if (isSelected) Color.White else Ink500,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Weight chart
                uiState.periodData?.let { period ->
                    if (period.data.isNotEmpty()) {
                        Card(
                            shape = RoundedCornerShape(VitalRadius.Xl),
                            colors = CardDefaults.cardColors(containerColor = AppSurface),
                            elevation = CardDefaults.cardElevation(0.dp),
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

                PersonalInfoCard(profile = uiState.healthProfile, onEdit = { editingSection = HealthProfileSection.Personal })
                DietActivityCard(profile = uiState.healthProfile, onEdit = { editingSection = HealthProfileSection.DietActivity })
                BodyGoalsCard(profile = uiState.healthProfile, onEdit = { editingSection = HealthProfileSection.BodyGoals })
                DailyTargetsCard(profile = uiState.healthProfile, onEdit = { editingSection = HealthProfileSection.DailyTargets })
                GoalTimelineCard(profile = uiState.healthProfile, onEdit = { editingSection = HealthProfileSection.GoalTimeline })

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

    if (showAddMetricDialog) {
        AddMetricDialog(
            onDismiss = { showAddMetricDialog = false },
            onSave = { request ->
                viewModel.addMetric(request)
                showAddMetricDialog = false
            }
        )
    }

    editingSection?.let { section ->
        HealthProfileEditDialog(
            section = section,
            profile = uiState.healthProfile,
            onDismiss = { editingSection = null },
            onSave = { request ->
                viewModel.updateHealthProfile(request)
                editingSection = null
            }
        )
    }
}

private enum class HealthProfileSection {
    Personal,
    DietActivity,
    BodyGoals,
    DailyTargets,
    GoalTimeline
}

private val genderOptions = listOf(
    "male" to "Nam",
    "female" to "Nữ"
)

private val dietTypeOptions = listOf(
    "balanced" to "Cân bằng",
    "high_protein" to "Giàu đạm",
    "keto" to "Ketogenic",
    "vegan" to "Thuần chay",
    "vegetarian" to "Chay thường",
    "paleo" to "Thời kỳ đồ đá"
)

private val activityLevelOptions = listOf(
    "sedentary" to "Ít vận động",
    "lightly_active" to "Vận động nhẹ",
    "moderately_active" to "Vận động vừa phải",
    "very_active" to "Vận động nhiều",
    "extra_active" to "Vận động cực nặng"
)

private val goalTypeOptions = listOf(
    "lose_weight" to "Giảm cân",
    "gain_weight" to "Tăng cân",
    "maintain" to "Giữ cân",
    "gain_muscle" to "Tăng cơ",
    "bulking" to "Tăng cơ tối đa",
    "improve_endurance" to "Cải thiện sức bền"
)

private val goalStatusOptions = listOf(
    "active" to "Đang thực hiện",
    "completed" to "Đã hoàn thành",
    "abandoned" to "Đã từ bỏ"
)

@Composable
private fun PersonalInfoCard(profile: HealthProfileDto?, onEdit: () -> Unit) {
    HealthProfileInfoCard(
        title = "Thông tin cá nhân",
        rows = listOf(
            "Giới tính" to formatGender(profile?.gender),
            "Chiều cao" to formatHeight(profile?.heightCm),
            "Tuổi" to formatAge(profile?.birthDate)
        ),
        onEdit = onEdit
    )
}

@Composable
private fun DietActivityCard(profile: HealthProfileDto?, onEdit: () -> Unit) {
    HealthProfileInfoCard(
        title = "Chế độ ăn uống & vận động",
        rows = listOf(
            "Chế độ ăn" to formatDietType(profile?.dietType),
            "Dị ứng thực phẩm" to formatFoodAllergies(profile?.foodAllergies),
            "Mức vận động" to formatActivityLevel(profile?.activityLevel)
        ),
        onEdit = onEdit
    )
}

@Composable
private fun BodyGoalsCard(profile: HealthProfileDto?, onEdit: () -> Unit) {
    HealthProfileInfoCard(
        title = "Mục tiêu hình thể",
        rows = listOf(
            "Loại mục tiêu" to formatGoalType(profile?.goalType),
            "Cân nặng ban đầu" to formatWeight(profile?.initialWeightKg),
            "Cân nặng mục tiêu" to formatWeight(profile?.weightGoalKg ?: profile?.targetWeightKg),
            "Mốc đối chiếu" to formatWeight(profile?.targetWeightKg),
            "Tốc độ mỗi tuần" to formatWeeklyRate(profile?.weeklyRateKg)
        ),
        onEdit = onEdit
    )
}

@Composable
private fun DailyTargetsCard(profile: HealthProfileDto?, onEdit: () -> Unit) {
    HealthProfileInfoCard(
        title = "Mục tiêu dinh dưỡng hàng ngày",
        rows = listOf(
            "Calo tổng quan" to formatCalories(profile?.caloriesGoal),
            "Calo mỗi ngày" to formatCalories(profile?.dailyCaloriesGoal),
            "Nước" to formatWater(profile?.waterGoalMl),
            "Protein" to formatGram(profile?.proteinGoalG),
            "Chất béo" to formatGram(profile?.fatGoalG),
            "Carbs" to formatGram(profile?.carbsGoalG)
        ),
        onEdit = onEdit
    )
}

@Composable
private fun GoalTimelineCard(profile: HealthProfileDto?, onEdit: () -> Unit) {
    HealthProfileInfoCard(
        title = "Thời gian & trạng thái mục tiêu",
        rows = listOf(
            "Ngày bắt đầu" to formatDate(profile?.goalStartDate),
            "Hạn chót" to formatDate(profile?.goalDeadline),
            "Trạng thái" to formatGoalStatus(profile?.goalStatus)
        ),
        onEdit = onEdit
    )
}

@Composable
private fun HealthProfileInfoCard(title: String, rows: List<Pair<String, String>>, onEdit: () -> Unit) {
    Card(
        shape = RoundedCornerShape(VitalRadius.Xl),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Ink900)
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Chỉnh sửa",
                        tint = Ink700,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            rows.forEach { (label, value) ->
                PersonalInfoRow(label = label, value = value)
            }
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
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Ink900,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            color = Ink700,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HealthProfileEditDialog(
    section: HealthProfileSection,
    profile: HealthProfileDto?,
    onDismiss: () -> Unit,
    onSave: (HealthProfileDto) -> Unit
) {
    var birthDate by remember(section, profile) { mutableStateOf(profile?.birthDate.orEmpty()) }
    var gender by remember(section, profile) { mutableStateOf(profile?.gender.orEmpty()) }
    var heightCm by remember(section, profile) { mutableStateOf(profile?.heightCm?.toCleanString().orEmpty()) }
    var dietType by remember(section, profile) { mutableStateOf(profile?.dietType.orEmpty()) }
    var foodAllergies by remember(section, profile) { mutableStateOf(profile?.foodAllergies?.joinToString(", ").orEmpty()) }
    var activityLevel by remember(section, profile) { mutableStateOf(profile?.activityLevel.orEmpty()) }
    var goalType by remember(section, profile) { mutableStateOf(profile?.goalType.orEmpty()) }
    var initialWeightKg by remember(section, profile) { mutableStateOf(profile?.initialWeightKg?.toCleanString().orEmpty()) }
    var weightGoalKg by remember(section, profile) { mutableStateOf(profile?.weightGoalKg?.toCleanString().orEmpty()) }
    var targetWeightKg by remember(section, profile) { mutableStateOf(profile?.targetWeightKg?.toCleanString().orEmpty()) }
    var weeklyRateKg by remember(section, profile) { mutableStateOf(profile?.weeklyRateKg?.toCleanString().orEmpty()) }
    var caloriesGoal by remember(section, profile) { mutableStateOf(profile?.caloriesGoal?.toString().orEmpty()) }
    var dailyCaloriesGoal by remember(section, profile) { mutableStateOf(profile?.dailyCaloriesGoal?.toString().orEmpty()) }
    var waterGoalMl by remember(section, profile) { mutableStateOf(profile?.waterGoalMl?.toString().orEmpty()) }
    var proteinGoalG by remember(section, profile) { mutableStateOf(profile?.proteinGoalG?.toString().orEmpty()) }
    var fatGoalG by remember(section, profile) { mutableStateOf(profile?.fatGoalG?.toString().orEmpty()) }
    var carbsGoalG by remember(section, profile) { mutableStateOf(profile?.carbsGoalG?.toString().orEmpty()) }
    var goalStartDate by remember(section, profile) { mutableStateOf(profile?.goalStartDate.orEmpty()) }
    var goalDeadline by remember(section, profile) { mutableStateOf(profile?.goalDeadline.orEmpty()) }
    var goalStatus by remember(section, profile) { mutableStateOf(profile?.goalStatus.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = { Text("Chỉnh sửa ${section.editTitle()}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (section) {
                    HealthProfileSection.Personal -> {
                        EditDropdownField("Giới tính", gender, genderOptions, { gender = it })
                        EditTextField("Chiều cao (cm)", heightCm, { heightCm = it })
                        EditDateField("Ngày sinh", birthDate, { birthDate = it })
                    }
                    HealthProfileSection.DietActivity -> {
                        EditDropdownField("Chế độ ăn", dietType, dietTypeOptions, { dietType = it })
                        EditTextField("Dị ứng thực phẩm", foodAllergies, { foodAllergies = it })
                        EditDropdownField("Mức vận động", activityLevel, activityLevelOptions, { activityLevel = it })
                    }
                    HealthProfileSection.BodyGoals -> {
                        EditDropdownField("Loại mục tiêu", goalType, goalTypeOptions, { goalType = it })
                        EditTextField("Cân nặng ban đầu (kg)", initialWeightKg, { initialWeightKg = it })
                        EditTextField("Cân nặng mục tiêu (kg)", weightGoalKg, { weightGoalKg = it })
                        EditTextField("Mốc đối chiếu (kg)", targetWeightKg, { targetWeightKg = it })
                        EditTextField("Tốc độ mỗi tuần (kg)", weeklyRateKg, { weeklyRateKg = it })
                    }
                    HealthProfileSection.DailyTargets -> {
                        EditTextField("Calo tổng quan", caloriesGoal, { caloriesGoal = it })
                        EditTextField("Calo mỗi ngày", dailyCaloriesGoal, { dailyCaloriesGoal = it })
                        EditTextField("Nước (ml)", waterGoalMl, { waterGoalMl = it })
                        EditTextField("Protein (g)", proteinGoalG, { proteinGoalG = it })
                        EditTextField("Chất béo (g)", fatGoalG, { fatGoalG = it })
                        EditTextField("Carbs (g)", carbsGoalG, { carbsGoalG = it })
                    }
                    HealthProfileSection.GoalTimeline -> {
                        EditDateField("Ngày bắt đầu", goalStartDate, { goalStartDate = it })
                        EditDateField("Hạn chót", goalDeadline, { goalDeadline = it })
                        EditDropdownField("Trạng thái", goalStatus, goalStatusOptions, { goalStatus = it })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        when (section) {
                            HealthProfileSection.Personal -> HealthProfileDto(
                                birthDate = birthDate.blankToNull(),
                                gender = gender.blankToNull(),
                                heightCm = heightCm.toFloatOrNull()
                            )
                            HealthProfileSection.DietActivity -> HealthProfileDto(
                                dietType = dietType.blankToNull(),
                                foodAllergies = foodAllergies.splitCsv(),
                                activityLevel = activityLevel.blankToNull()
                            )
                            HealthProfileSection.BodyGoals -> HealthProfileDto(
                                goalType = goalType.blankToNull(),
                                initialWeightKg = initialWeightKg.toFloatOrNull(),
                                weightGoalKg = weightGoalKg.toFloatOrNull(),
                                targetWeightKg = targetWeightKg.toFloatOrNull(),
                                weeklyRateKg = weeklyRateKg.toFloatOrNull()
                            )
                            HealthProfileSection.DailyTargets -> HealthProfileDto(
                                caloriesGoal = caloriesGoal.toFloatOrNull(),
                                dailyCaloriesGoal = dailyCaloriesGoal.toFloatOrNull(),
                                waterGoalMl = waterGoalMl.toIntOrNull(),
                                proteinGoalG = proteinGoalG.toFloatOrNull(),
                                fatGoalG = fatGoalG.toFloatOrNull(),
                                carbsGoalG = carbsGoalG.toFloatOrNull()
                            )
                            HealthProfileSection.GoalTimeline -> HealthProfileDto(
                                goalStartDate = goalStartDate.blankToNull(),
                                goalDeadline = goalDeadline.blankToNull(),
                                goalStatus = goalStatus.blankToNull()
                            )
                        }
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EB67E))
            ) { Text("Lưu", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@Composable
private fun EditTextField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditDropdownField(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val displayValue = options.firstOrNull { it.first == value }?.second.orEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = AppSurface,
                unfocusedContainerColor = AppSurface,
                disabledContainerColor = AppSurface
            ),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(AppSurface)
        ) {
            options.forEach { (code, display) ->
                DropdownMenuItem(
                    text = { Text(display) },
                    colors = MenuDefaults.itemColors(textColor = Ink900),
                    onClick = {
                        onValueChange(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditDateField(label: String, value: String, onValueChange: (String) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = value.toEpochMillisOrNull())

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text("yyyy-MM-dd") },
        trailingIcon = {
            TextButton(onClick = { showDatePicker = true }) {
                Text("Chọn", color = Color(0xFF0EB67E), fontWeight = FontWeight.Bold)
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = AppSurface,
            unfocusedContainerColor = AppSurface,
            disabledContainerColor = AppSurface
        ),
        modifier = Modifier
            .fillMaxWidth()
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onValueChange(millis.toIsoDate())
                        }
                        showDatePicker = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EB67E))
                ) {
                    Text("Lưu", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Hủy") }
            },
            colors = DatePickerDefaults.colors(containerColor = AppSurface)
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun AddMetricDialog(
    onDismiss: () -> Unit,
    onSave: (UpsertBodyMetricRequest) -> Unit
) {
    var weight by remember { mutableStateOf("") }
    var bodyFat by remember { mutableStateOf("") }
    var waist by remember { mutableStateOf("") }
    var hip by remember { mutableStateOf("") }
    var chest by remember { mutableStateOf("") }
    var neck by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppSurface,
        title = { Text("Cập nhật số liệu") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(weight, { weight = it }, label = { Text("Cân nặng kg") }, singleLine = true)
                OutlinedTextField(bodyFat, { bodyFat = it }, label = { Text("% mỡ") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(waist, { waist = it }, label = { Text("Eo") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(hip, { hip = it }, label = { Text("Hông") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(chest, { chest = it }, label = { Text("Ngực") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(neck, { neck = it }, label = { Text("Cổ") }, singleLine = true, modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                onSave(
                    UpsertBodyMetricRequest(
                        recordedAt = LocalDate.now().toString(),
                        weightKg = weight.toFloatOrNull(),
                        bodyFatPct = bodyFat.toFloatOrNull(),
                        waistCm = waist.toFloatOrNull(),
                        hipCm = hip.toFloatOrNull(),
                        chestCm = chest.toFloatOrNull(),
                        neckCm = neck.toFloatOrNull()
                    )
                )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EB67E))
            ) { Text("Lưu", color = Color.White, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@Composable
private fun MetricCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(VitalRadius.Xl),
        colors = CardDefaults.cardColors(containerColor = AppSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.18f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = color)
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, fontSize = 12.sp, color = Ink500, fontWeight = FontWeight.Medium)
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

        // Fill area underline
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

private fun formatGender(gender: String?): String {
    return when (gender) {
        "male" -> "Nam"
        "female" -> "Nữ"
        null -> "--"
        else -> gender.replaceFirstChar { it.uppercase() }
    }
}

private fun formatHeight(heightCm: Float?): String {
    return heightCm?.let { "%.0f cm".format(it) } ?: "--"
}

private fun formatWeight(weightKg: Float?): String {
    return weightKg?.let { "%.1f kg".format(it) } ?: "--"
}

private fun formatWeeklyRate(weeklyRateKg: Float?): String {
    return weeklyRateKg?.let { "%.1f kg/tuần".format(it) } ?: "--"
}

private fun formatCalories(calories: Float?): String {
    return calories?.let { "${it.toDisplayNumber()} kcal" } ?: "--"
}

private fun formatWater(waterMl: Int?): String {
    return waterMl?.let { "$it ml" } ?: "--"
}

private fun formatGram(grams: Float?): String {
    return grams?.let { "${it.toDisplayNumber()}g" } ?: "--"
}

private fun formatAge(birthDate: String?): String {
    return runCatching {
        val date = LocalDate.parse(birthDate)
        "${Period.between(date, LocalDate.now()).years} tuổi"
    }.getOrDefault("--")
}

private fun HealthProfileSection.editTitle(): String {
    return when (this) {
        HealthProfileSection.Personal -> "thông tin cá nhân"
        HealthProfileSection.DietActivity -> "chế độ ăn uống & vận động"
        HealthProfileSection.BodyGoals -> "mục tiêu hình thể"
        HealthProfileSection.DailyTargets -> "mục tiêu dinh dưỡng hàng ngày"
        HealthProfileSection.GoalTimeline -> "thời gian & trạng thái mục tiêu"
    }
}

private fun String.blankToNull(): String? = trim().takeIf { it.isNotEmpty() }

private fun String.splitCsv(): List<String>? {
    return split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .takeIf { it.isNotEmpty() }
}

private fun Float.toCleanString(): String {
    return if (this % 1f == 0f) toInt().toString() else toString()
}

private fun Float.toDisplayNumber(): String {
    return if (this % 1f == 0f) "%.0f".format(this) else "%.2f".format(this)
}

private fun String.toEpochMillisOrNull(): Long? {
    return runCatching {
        LocalDate.parse(this)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }.getOrNull()
}

private fun Long.toIsoDate(): String {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .toString()
}

private fun formatDateInput(date: String): String {
    return date.takeIf { it.isNotBlank() }?.let(::formatDate) ?: ""
}

private fun formatDate(date: String?): String {
    return runCatching {
        val parsed = LocalDate.parse(date)
        "%02d/%02d/%04d".format(parsed.dayOfMonth, parsed.monthValue, parsed.year)
    }.getOrDefault("--")
}

private fun formatDietType(dietType: String?): String {
    return when (dietType) {
        "balanced" -> "Cân bằng"
        "high_protein" -> "Giàu đạm"
        "keto" -> "Ketogenic"
        "vegan" -> "Thuần chay"
        "vegetarian" -> "Chay thường"
        "paleo" -> "Thời kỳ đồ đá"
        null -> "--"
        else -> dietType.replace("_", " ").replaceFirstChar { it.uppercase() }
    }
}

private fun formatFoodAllergies(foodAllergies: List<String>?): String {
    if (foodAllergies.isNullOrEmpty()) return "Không có"
    return foodAllergies.joinToString(", ") { allergy ->
        when (allergy) {
            "gluten" -> "Gluten"
            "dairy" -> "Sữa"
            "nuts" -> "Các loại hạt"
            "seafood" -> "Hải sản"
            "eggs" -> "Trứng"
            "soy" -> "Đậu nành"
            else -> allergy.replace("_", " ").replaceFirstChar { it.uppercase() }
        }
    }
}

private fun formatActivityLevel(activityLevel: String?): String {
    return when (activityLevel) {
        "sedentary" -> "Ít vận động"
        "lightly_active" -> "Vận động nhẹ"
        "moderately_active" -> "Vận động vừa phải"
        "very_active" -> "Vận động nhiều"
        "extra_active" -> "Vận động cực nặng"
        null -> "--"
        else -> activityLevel.replace("_", " ").replaceFirstChar { it.uppercase() }
    }
}

private fun formatGoalType(goalType: String?): String {
    return when (goalType) {
        "lose_weight" -> "Giảm cân"
        "gain_weight" -> "Tăng cân"
        "maintain" -> "Giữ cân"
        "gain_muscle" -> "Tăng cơ"
        "bulking" -> "Tăng cơ tối đa"
        "improve_endurance" -> "Cải thiện sức bền"
        null -> "--"
        else -> goalType.replace("_", " ").replaceFirstChar { it.uppercase() }
    }
}

private fun formatGoalStatus(goalStatus: String?): String {
    return when (goalStatus) {
        "active" -> "Đang thực hiện"
        "completed" -> "Đã hoàn thành"
        "abandoned" -> "Đã từ bỏ"
        null -> "--"
        else -> goalStatus.replace("_", " ").replaceFirstChar { it.uppercase() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MetricsScreenPreview() {

    val mockData = listOf(
        BodyMetricDto(
            id = "1",
            date = "2026-05-01",
            weightKg = 78.5f,
            bodyFatPct = 24f,
            waistCm = null,
            bmi = 26.2f,
            notes = null
        ),
        BodyMetricDto(
            id = "2",
            date = "2026-05-05",
            weightKg = 77.8f,
            bodyFatPct = 23.5f,
            waistCm = null,
            bmi = 25.9f,
            notes = null
        ),
        BodyMetricDto(
            id = "3",
            date = "2026-05-10",
            weightKg = 77.1f,
            bodyFatPct = 23f,
            waistCm = null,
            bmi = 25.6f,
            notes = null
        ),
        BodyMetricDto(
            id = "4",
            date = "2026-05-13",
            weightKg = 76.4f,
            bodyFatPct = 22.4f,
            waistCm = null,
            bmi = 25.2f,
            notes = "Giảm mỡ tốt"
        )
    )

    val latest = mockData.last()
    val mockProfile = HealthProfileDto(
        birthDate = "1997-01-01",
        gender = "male",
        heightCm = 170f,
        initialWeightKg = 65f,
        activityLevel = "moderately_active",
        dietType = "balanced",
        foodAllergies = listOf("gluten"),
        weightGoalKg = 60f,
        caloriesGoal = 2000f,
        goalType = "lose_weight",
        targetWeightKg = 65f,
        dailyCaloriesGoal = 1800f,
        proteinGoalG = 150f,
        fatGoalG = 65f,
        carbsGoalG = 200f,
        weeklyRateKg = 0.5f,
        goalStartDate = "2026-01-01",
        goalDeadline = "2026-12-31",
        goalStatus = "active",
        waterGoalMl = 2000
    )
    VitalAITheme {
        Scaffold(
            topBar = {
                TopAppBar(
                title = { Text("Số liệu cơ thể", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {}) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    TextButton(onClick = {}) {
                        Text("Lịch sử", color = Mint500, fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppSurface)
            )
        },
        containerColor = AppMutedBackground
    ) { padding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    MetricCard(
                        "Cân nặng",
                        "${latest.weightKg} kg",
                        Mint500,
                        Modifier.weight(1f)
                    )

                    latest.bmi?.let { bmi ->

                        MetricCard(
                            "BMI",
                            "%.1f".format(bmi),
                            bmiColor(bmi),
                            Modifier.weight(1f)
                        )
                    }

                    latest.bodyFatPct?.let { fat ->

                        MetricCard(
                            "Mỡ cơ thể",
                            "%.1f%%".format(fat),
                            MacroFat,
                            Modifier.weight(1f)
                        )
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50),
                    color = AppSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AppLine)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp)
                    ) {
                        listOf(
                            "week" to "1 tuần",
                            "month" to "1 tháng",
                            "3months" to "3 tháng",
                            "6months" to "6 tháng",
                            "year" to "1 năm"
                        ).forEach { (key, label) ->
                            val isSelected = "month" == key
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isSelected) Mint500 else Color.Transparent)
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    color = if (isSelected) Color.White else Ink500,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(VitalRadius.Xl),
                    colors = CardDefaults.cardColors(
                        containerColor = AppSurface
                    ),
                    elevation = CardDefaults.cardElevation(0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {

                            Text(
                                "Biểu đồ cân nặng",
                                fontWeight = FontWeight.Bold,
                                color = Ink900
                            )

                            Text(
                                "TB: 77.4 kg",
                                fontSize = 12.sp,
                                color = Ink500
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        WeightLineChart(
                            data = mockData,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                    }
                }

                PersonalInfoCard(profile = mockProfile, onEdit = {})
                DietActivityCard(profile = mockProfile, onEdit = {})
                BodyGoalsCard(profile = mockProfile, onEdit = {})
                DailyTargetsCard(profile = mockProfile, onEdit = {})
                GoalTimelineCard(profile = mockProfile, onEdit = {})
            }
        }
    }
}
