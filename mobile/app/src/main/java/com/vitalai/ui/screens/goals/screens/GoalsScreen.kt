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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
    GoalTypeOption("gain_weight", "Tăng cân"),
    GoalTypeOption("gain_muscle", "Tăng cơ"),
    GoalTypeOption("improve_endurance", "Tăng sức bền"),
    GoalTypeOption("bulking", "Xả cơ"),
    GoalTypeOption("cutting", "Siết cơ"),
    GoalTypeOption("maintain", "Giữ cân")
)

private val activityLevelOptions = listOf(
    GoalTypeOption("sedentary", "Ít vận động"),
    GoalTypeOption("lightly_active", "Vận động nhẹ"),
    GoalTypeOption("moderately_active", "Vận động vừa"),
    GoalTypeOption("very_active", "Vận động nhiều"),
    GoalTypeOption("extra_active", "Vận động rất nhiều")
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WrapSegmentedPills(
    options: List<Pair<String, String>>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = Ink900
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (key, label) ->
            val isSelected = selected == key
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(VitalRadius.Pill))
                    .background(if (isSelected) selectedColor else com.vitalai.ui.theme.AppSurface2)
                    .clickable { onSelected(key) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) Color.White else com.vitalai.ui.theme.Ink700
                )
            }
        }
    }
}

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
                WrapSegmentedPills(
                    options = goalTypeOptions.map { Pair(it.value, it.label) },
                    selected = uiState.goalType,
                    onSelected = { viewModel.onGoalType(it) },
                    modifier = Modifier.fillMaxWidth(),
                    selectedColor = Mint500
                )

                Spacer(Modifier.height(28.dp))
                SectionHeader(
                    title = "Mức độ vận động",
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                WrapSegmentedPills(
                    options = activityLevelOptions.map { Pair(it.value, it.label) },
                    selected = uiState.activityLevel,
                    onSelected = { viewModel.onActivityLevel(it) },
                    modifier = Modifier.fillMaxWidth(),
                    selectedColor = Mint500
                )

                Spacer(Modifier.height(28.dp))
                SectionHeader(
                    title = "Dinh dưỡng hằng ngày",
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                val tdeeVal = uiState.tdee
                val caloriesDisplay = tdeeVal?.let { "%.0f".format(it) } ?: uiState.dailyCaloriesGoal
                GoalField(
                    label = "Calo mục tiêu (kcal)",
                    value = caloriesDisplay,
                    onValueChange = {},
                    readOnly = true
                )
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


