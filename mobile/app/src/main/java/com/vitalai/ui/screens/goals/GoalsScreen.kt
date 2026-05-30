package com.vitalai.ui.screens.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.vitalai.ui.theme.AppMutedBackground
import com.vitalai.ui.theme.AppSurface
import com.vitalai.ui.theme.Ink500
import com.vitalai.ui.theme.Ink900
import com.vitalai.ui.theme.Mint500
import com.vitalai.ui.theme.VitalRadius

private data class GoalTypeOption(val value: String, val label: String)

private val goalTypeOptions = listOf(
    GoalTypeOption("lose_weight", "Giảm cân"),
    GoalTypeOption("maintain", "Giữ cân"),
    GoalTypeOption("gain_weight", "Tăng cân")
)

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

    Column(modifier = Modifier.fillMaxSize().background(AppMutedBackground)) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(VitalRadius.Pill))
                    .background(AppSurface)
                    .clickable { navController.popBackStack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = Ink900)
            }
            Spacer(Modifier.width(12.dp))
            Text("Mục tiêu của tôi", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink900)
        }

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
                SectionLabel("LOẠI MỤC TIÊU")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    goalTypeOptions.forEach { opt ->
                        val selected = uiState.goalType == opt.value
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(VitalRadius.Pill))
                                .background(if (selected) Mint500 else AppSurface)
                                .clickable { viewModel.onGoalType(opt.value) }
                                .padding(horizontal = 18.dp, vertical = 10.dp)
                        ) {
                            Text(
                                opt.label,
                                color = if (selected) AppSurface else Ink900,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                SectionLabel("DINH DƯỠNG HẰNG NGÀY")
                GoalField("Calo mục tiêu (kcal)", uiState.dailyCaloriesGoal, viewModel::onDailyCalories)
                GoalField("Protein (g)", uiState.proteinGoalG, viewModel::onProtein)
                GoalField("Carbs (g)", uiState.carbsGoalG, viewModel::onCarbs)
                GoalField("Chất béo (g)", uiState.fatGoalG, viewModel::onFat)
                GoalField("Nước (ml)", uiState.waterGoalMl, viewModel::onWater)

                Spacer(Modifier.height(20.dp))
                SectionLabel("CÂN NẶNG")
                GoalField("Cân nặng mục tiêu (kg)", uiState.targetWeightKg, viewModel::onTargetWeight)
                GoalField("Tốc độ thay đổi mỗi tuần (kg)", uiState.weeklyRateKg, viewModel::onWeeklyRate)

                Spacer(Modifier.height(28.dp))
                VitalButton(
                    text = if (uiState.isSaving) "Đang lưu..." else "Lưu mục tiêu",
                    onClick = { if (!uiState.isSaving) viewModel.save() },
                    mint = true,
                    modifier = Modifier.fillMaxWidth()
                )
                uiState.error?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
                Spacer(Modifier.height(40.dp))
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
