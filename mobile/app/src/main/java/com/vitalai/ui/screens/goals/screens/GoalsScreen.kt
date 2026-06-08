package com.vitalai.ui.screens.goals.screens

import com.vitalai.ui.screens.goals.components.*
import com.vitalai.ui.screens.goals.viewmodels.*
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
import com.vitalai.ui.components.SectionHeader
import com.vitalai.ui.components.SegmentedPills
import com.vitalai.ui.components.VitalButton
import com.vitalai.ui.components.VitalScreenHeader
import com.vitalai.ui.theme.AppLine
import com.vitalai.ui.theme.AppMutedBackground
import com.vitalai.ui.theme.AppSurface
import com.vitalai.ui.theme.Ink500
import com.vitalai.ui.theme.Ink900
import com.vitalai.ui.theme.MacroCarbs
import com.vitalai.ui.theme.MacroFat
import com.vitalai.ui.theme.MacroProtein
import com.vitalai.ui.theme.Mint100
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
            VitalScreenHeader(
                title = "Mục tiêu của tôi",
                onBackClick = { navController.popBackStack() }
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
                SectionHeader(
                    title = "Loại mục tiêu",
                    modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
                )
                SegmentedPills(
                    options = goalTypeOptions.map { Pair(it.value, it.label) },
                    selected = uiState.goalType,
                    onSelected = { viewModel.onGoalType(it) },
                    modifier = Modifier.fillMaxWidth(),
                    selectedColor = Mint500
                )

                Spacer(Modifier.height(28.dp))
                SectionHeader(
                    title = "Dinh dưỡng hằng ngày",
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
                SectionHeader(
                    title = "Cân nặng",
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                GoalField("Cân nặng mục tiêu (kg)", uiState.targetWeightKg, viewModel::onTargetWeight)
                GoalField("Tốc độ thay đổi mỗi tuần (kg)", uiState.weeklyRateKg, viewModel::onWeeklyRate)

                Spacer(Modifier.height(28.dp))
                VitalButton(
                    text = if (uiState.isSaving) "Đang lưu..." else "Lưu mục tiêu",
                    onClick = { if (!uiState.isSaving) viewModel.save() },
                    mint = true,
                    textColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary,
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
}


