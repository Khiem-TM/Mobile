package com.vitalai.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.vitalai.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    step: Int,
    navController: NavController,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is OnboardingState.Success) {
            navController.navigate(Screen.Home) {
                popUpTo(Screen.Welcome::class.qualifiedName!!) { inclusive = true }
            }
        }
    }

    // Handle back press to go to previous step
    BackHandler(enabled = step > 1) {
        navController.popBackStack()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(4) { index ->
                            Box(
                                modifier = Modifier
                                    .size(width = 40.dp, height = 4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        if (index + 1 <= step) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outlineVariant
                                    )
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (step > 1) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Text("←", fontSize = 24.sp)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                when (step) {
                    1 -> Step1Gender(viewModel)
                    2 -> Step2BodyMetrics(viewModel)
                    3 -> Step3Activity(viewModel)
                    4 -> Step4Goal(viewModel)
                }
            }

            if (uiState is OnboardingState.Error) {
                Text(
                    text = (uiState as OnboardingState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Button(
                onClick = {
                    if (step < 4) {
                        navController.navigate(Screen.Onboarding(step + 1))
                    } else {
                        viewModel.submitProfile()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = uiState !is OnboardingState.Loading
            ) {
                if (uiState is OnboardingState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(if (step == 4) "Hoàn tất" else "Tiếp tục")
                }
            }
        }
    }
}

@Composable
fun Step1Gender(viewModel: OnboardingViewModel) {
    Column {
        Text("Giới tính của bạn là gì?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Thông tin này giúp chúng tôi tính toán nhu cầu calo chính xác hơn.", modifier = Modifier.padding(top = 8.dp, bottom = 32.dp))

        listOf("male" to "Nam", "female" to "Nữ", "other" to "Khác").forEach { (id, label) ->
            val isSelected = viewModel.gender == id
            OutlinedCard(
                onClick = { viewModel.gender = id },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                border = if (isSelected) ButtonDefaults.outlinedButtonBorder.copy(width = 2.dp) else CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = isSelected, onClick = { viewModel.gender = id })
                    Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
                }
            }
        }
    }
}

@Composable
fun Step2BodyMetrics(viewModel: OnboardingViewModel) {
    Column {
        Text("Chỉ số cơ thể", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))

        Text("Chiều cao (cm): ${viewModel.heightCm.toInt()}")
        Slider(
            value = viewModel.heightCm,
            onValueChange = { viewModel.heightCm = it },
            valueRange = 100f..250f
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text("Cân nặng hiện tại (kg): ${viewModel.weightKg.toInt()}")
        Slider(
            value = viewModel.weightKg,
            onValueChange = { viewModel.weightKg = it },
            valueRange = 30f..200f
        )
    }
}

@Composable
fun Step3Activity(viewModel: OnboardingViewModel) {
    val levels = listOf(
        "sedentary" to "Ít vận động",
        "lightly_active" to "Vận động nhẹ",
        "moderately_active" to "Vận động vừa phải",
        "very_active" to "Vận động nhiều",
        "extra_active" to "Vận động rất nhiều"
    )

    Column {
        Text("Mức độ hoạt động", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        levels.forEach { (id, label) ->
            val isSelected = viewModel.activityLevel == id
            FilterChip(
                selected = isSelected,
                onClick = { viewModel.activityLevel = id },
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
fun Step4Goal(viewModel: OnboardingViewModel) {
    val goals = listOf(
        "lose_weight" to "Giảm cân",
        "maintain" to "Duy trì vóc dáng",
        "gain_muscle" to "Tăng cơ"
    )

    Column {
        Text("Mục tiêu của bạn", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        goals.forEach { (id, label) ->
            val isSelected = viewModel.goalType == id
            OutlinedCard(
                onClick = { viewModel.goalType = id },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = if (isSelected) CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer) else CardDefaults.outlinedCardColors()
            ) {
                Text(label, modifier = Modifier.padding(24.dp), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
