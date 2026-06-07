package com.vitalai.ui.screens.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.border
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vitalai.ui.components.ErrorState
import com.vitalai.ui.components.LoadingState
import com.vitalai.ui.components.VitalButton
import com.vitalai.ui.theme.AppLine
import com.vitalai.ui.theme.AppMutedBackground
import com.vitalai.ui.theme.AppSurface
import com.vitalai.ui.theme.Ink500
import com.vitalai.ui.theme.Ink900
import com.vitalai.ui.theme.MacroCarbs
import com.vitalai.ui.theme.MacroFat
import com.vitalai.ui.theme.MacroProtein
import com.vitalai.ui.theme.Mint500
import com.vitalai.ui.theme.VitalRadius

private data class GoalTypeOption(val value: String, val label: String)

private val goalTypeOptions = listOf(
    GoalTypeOption("lose_weight", "Giảm cân"),
    GoalTypeOption("maintain", "Giữ cân"),
    GoalTypeOption("gain_weight", "Tăng cân")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    navController: NavController,
    viewModel: GoalsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            android.widget.Toast.makeText(context, "Đã lưu mục tiêu", android.widget.Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Mục tiêu của tôi", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Ink900)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = Ink900)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppSurface)
            )
        },
        containerColor = AppSurface
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
            uiState.isLoading -> LoadingState(modifier = Modifier.weight(1f))
            uiState.error != null && uiState.profile == null -> ErrorState(
                message = uiState.error!!,
                onRetry = { viewModel.load() },
                modifier = Modifier.weight(1f)
            )
            else -> Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    "LOẠI MỤC TIÊU",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink900,
                    modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(VitalRadius.Pill))
                        .background(AppSurface)
                        .border(1.dp, AppLine, RoundedCornerShape(VitalRadius.Pill))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    goalTypeOptions.forEach { opt ->
                        val selected = uiState.goalType == opt.value
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(VitalRadius.Pill))
                                .background(if (selected) Color(0xFF0F3E17) else Color.Transparent)
                                .clickable { viewModel.onGoalType(opt.value) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                opt.label,
                                color = if (selected) Color.White else Ink500,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))
                Text(
                    "DINH DƯỠNG HẰNG NGÀY",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink900,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                GoalField("Calo mục tiêu (kcal)", uiState.dailyCaloriesGoal, viewModel::onDailyCalories)
                Spacer(Modifier.height(12.dp))
                MacroSliderField("Protein", uiState.proteinGoalG, viewModel::onProtein, 1000f)
                MacroSliderField("Carbs", uiState.carbsGoalG, viewModel::onCarbs, 1000f)
                MacroSliderField("Chất béo", uiState.fatGoalG, viewModel::onFat, 500f)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        GoalField("Nước (ml)", uiState.waterGoalMl, viewModel::onWater)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        GoalField("Bước chân/ngày", uiState.stepGoal, viewModel::onStepGoal)
                    }
                }

                Spacer(Modifier.height(28.dp))
                Text(
                    "CÂN NẶNG",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Ink900,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                GoalField("Cân nặng mục tiêu (kg)", uiState.targetWeightKg, viewModel::onTargetWeight)
                GoalField("Tốc độ thay đổi mỗi tuần (kg)", uiState.weeklyRateKg, viewModel::onWeeklyRate)

                Spacer(Modifier.height(28.dp))
                androidx.compose.material3.Button(
                    onClick = { if (!uiState.isSaving) viewModel.save() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(VitalRadius.Md),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0F3E17),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = if (uiState.isSaving) "Đang lưu..." else "Lưu mục tiêu",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
                uiState.error?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
                Spacer(Modifier.height(40.dp))
            }
        }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Ink500,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
private fun GoalField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    )
}

@Composable
private fun MacroSliderField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    maxValue: Float
) {
    val floatValue = value.toFloatOrNull() ?: 0f
    
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Ink500, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text("${floatValue.toInt()}g", color = Ink900, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }
        androidx.compose.material3.Slider(
            value = floatValue,
            onValueChange = { onValueChange(it.toInt().toString()) },
            valueRange = 0f..maxValue,
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = Color(0xFF0F3E17),
                activeTrackColor = Color(0xFF0F3E17),
                inactiveTrackColor = Color(0xFFE4EDE7)
            ),
            modifier = Modifier.fillMaxWidth().height(32.dp)
        )
    }
}
