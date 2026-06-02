package com.vitalai.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TrackChanges
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
import com.vitalai.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun OnboardingScreen(
    initialStep: Int = 1,
    navController: NavController,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    var step by remember { mutableIntStateOf(initialStep) }
    val uiState by viewModel.uiState.collectAsState()
    val isLoading = uiState is OnboardingState.Loading
    val canContinue = viewModel.isStepValid(step)

    LaunchedEffect(uiState) {
        if (uiState is OnboardingState.Success) {
            navController.navigate(Screen.Home) {
                popUpTo(Screen.Welcome::class.qualifiedName!!) { inclusive = true }
            }
        }
    }

    BackHandler(enabled = !isLoading) {
        if (step > 1) {
            step--
        } else {
            navController.popBackStack()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .padding(top = 60.dp, bottom = 28.dp)
        ) {
            // Header: Back button + Progress bar + Step count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                // Back button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Ink100)
                        .clickable(enabled = !isLoading) {
                            if (step > 1) {
                                step--
                            } else {
                                navController.popBackStack()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Back",
                        tint = Ink900
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Progress Bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(100))
                        .background(AppSurface2)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = step / 4f)
                            .clip(RoundedCornerShape(100))
                            .background(Mint500)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "$step/4",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink500
                )
            }

            // Title & Subtitle based on step
            val title = when (step) {
                1 -> "Bạn là?"
                2 -> "Số đo cơ thể"
                3 -> "Mục tiêu chính của bạn?"
                4 -> "Mức độ vận động"
                else -> ""
            }
            val subtitle = when (step) {
                1 -> "Giúp chúng tôi cá nhân hóa kế hoạch dinh dưỡng phù hợp với bạn"
                2 -> "Chiều cao và cân nặng hiện tại của bạn"
                3 -> "Chúng tôi sẽ điều chỉnh kế hoạch calo theo mục tiêu này"
                4 -> "Tần suất bạn tập luyện trong tuần"
                else -> ""
            }

            Text(
                text = title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Ink900,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = subtitle,
                fontSize = 15.sp,
                color = Ink500,
                modifier = Modifier.padding(bottom = 28.dp)
            )

            // Scrollable Content with Animation
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        if (targetState > initialState) {
                            slideInHorizontally(
                                animationSpec = tween(240),
                                initialOffsetX = { fullWidth -> fullWidth }
                            ) togetherWith slideOutHorizontally(
                                animationSpec = tween(240),
                                targetOffsetX = { fullWidth -> -fullWidth }
                            )
                        } else {
                            slideInHorizontally(
                                animationSpec = tween(240),
                                initialOffsetX = { fullWidth -> -fullWidth }
                            ) togetherWith slideOutHorizontally(
                                animationSpec = tween(240),
                                targetOffsetX = { fullWidth -> fullWidth }
                            )
                        }
                    },
                    label = "step_transition"
                ) { currentStep ->
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        when (currentStep) {
                            1 -> Step1Gender(
                                gender = viewModel.gender,
                                birthYear = birthYearFromDate(viewModel.birthDate),
                                onGenderChange = { viewModel.gender = it },
                                onBirthYearChange = { viewModel.birthDate = "$it-01-01" }
                            )
                            2 -> Step2BodyMetrics(
                                heightCm = viewModel.heightCm,
                                weightKg = viewModel.weightKg,
                                onHeightChange = { viewModel.heightCm = it },
                                onWeightChange = { viewModel.weightKg = it }
                            )
                            3 -> Step3Goal(
                                goalType = viewModel.goalType,
                                weightKg = viewModel.weightKg,
                                onGoalChange = { viewModel.goalType = it }
                            )
                            4 -> Step4Activity(
                                activityLevel = viewModel.activityLevel,
                                birthDate = viewModel.birthDate,
                                gender = viewModel.gender,
                                heightCm = viewModel.heightCm,
                                weightKg = viewModel.weightKg,
                                goalType = viewModel.goalType,
                                onActivityChange = { viewModel.activityLevel = it }
                            )
                        }
                    }
                }
            }

            if (uiState is OnboardingState.Error) {
                Text(
                    text = (uiState as OnboardingState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp),
                    fontSize = 13.sp
                )
            }

            // Next Button
            Button(
                onClick = {
                    if (canContinue && !isLoading) {
                        if (step < 4) {
                            step++
                        } else {
                            viewModel.submitProfile()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Ink900),
                shape = RoundedCornerShape(VitalRadius.Pill),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                enabled = canContinue && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (step == 4) "Hoàn tất" else "Tiếp tục",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Step1Gender(
    gender: String,
    birthYear: Int,
    onGenderChange: (String) -> Unit,
    onBirthYearChange: (Int) -> Unit
) {
    val options = listOf(
        Triple("male", "Nam", "👨"),
        Triple("female", "Nữ", "👩")
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        options.forEach { (id, label, emoji) ->
            val isSelected = gender == id
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(VitalRadius.Lg))
                    .background(if (isSelected) Mint50 else AppSurface2)
                    .border(
                        2.dp,
                        if (isSelected) Mint500 else Color.Transparent,
                        RoundedCornerShape(VitalRadius.Lg)
                    )
                    .clickable { onGenderChange(id) }
                    .padding(vertical = 28.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = emoji, fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = label,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink900
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Year of birth
    Text(
        text = "NĂM SINH",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Ink500,
        letterSpacing = 0.sp
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(VitalRadius.Md))
            .background(AppSurface2)
            .padding(18.dp, 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = birthYear.toString(),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Ink900
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Minus button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AppSurface)
                    .border(1.dp, AppLine, CircleShape)
                    .clickable { if (birthYear > 1900) onBirthYearChange(birthYear - 1) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Minus", modifier = Modifier.size(16.dp))
            }
            // Plus button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Ink900)
                    .clickable { if (birthYear < 2020) onBirthYearChange(birthYear + 1) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "Plus", tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun Step2BodyMetrics(
    heightCm: Float,
    weightKg: Float,
    onHeightChange: (Float) -> Unit,
    onWeightChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Height Card
        Card(
            shape = RoundedCornerShape(VitalRadius.Lg),
            colors = CardDefaults.cardColors(containerColor = AppSurface2),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Chiều cao (Height)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(VitalRadius.Pill))
                            .background(AppSurface2)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("cm", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink500)
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = heightCm.toInt().toString(),
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                        color = Mint500
                    )
                    Text(
                        text = "cm",
                        fontSize = 20.sp,
                        color = Ink500,
                        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                    )
                }

                Slider(
                    value = heightCm,
                    onValueChange = onHeightChange,
                    valueRange = 140f..210f,
                    colors = SliderDefaults.colors(
                        thumbColor = Mint500,
                        activeTrackColor = Mint500
                    ),
                    modifier = Modifier.padding(top = 12.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("140", fontSize = 11.sp, color = Ink500)
                    Text("175", fontSize = 11.sp, color = Ink500)
                    Text("210", fontSize = 11.sp, color = Ink500)
                }
            }
        }

        // Weight Card
        Card(
            shape = RoundedCornerShape(VitalRadius.Lg),
            colors = CardDefaults.cardColors(containerColor = AppSurface2),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cân nặng hiện tại", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(VitalRadius.Pill))
                            .background(AppSurface2)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("kg", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink500)
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = weightKg.toInt().toString(),
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                        color = Mint500
                    )
                    Text(
                        text = "kg",
                        fontSize = 20.sp,
                        color = Ink500,
                        modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
                    )
                }

                Slider(
                    value = weightKg,
                    onValueChange = onWeightChange,
                    valueRange = 40f..150f,
                    colors = SliderDefaults.colors(
                        thumbColor = Mint500,
                        activeTrackColor = Mint500
                    ),
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}

@Composable
fun Step3Goal(
    goalType: String,
    weightKg: Float,
    onGoalChange: (String) -> Unit
) {
    val goals = listOf(
        Triple("lose_weight", "Giảm cân", "🎯" to "Burn fat, get lean"),
        Triple("maintain", "Duy trì", "⚖️" to "Stay healthy & balanced"),
        Triple("gain_muscle", "Tăng cơ", "💪" to "Build strength & muscle")
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        goals.forEach { (id, title, extra) ->
            val (emoji, sub) = extra
            val isSelected = goalType == id
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(VitalRadius.Lg))
                    .background(if (isSelected) Mint50 else AppSurface2)
                    .border(
                        2.dp,
                        if (isSelected) Mint500 else Color.Transparent,
                        RoundedCornerShape(VitalRadius.Lg)
                    )
                    .clickable { onGoalChange(id) }
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = emoji, fontSize = 38.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(text = sub, fontSize = 13.sp, color = Ink500)
                    }
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .border(2.dp, if (isSelected) Mint500 else AppLine, CircleShape)
                            .background(if (isSelected) Mint500 else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(VitalRadius.Lg),
            colors = CardDefaults.cardColors(containerColor = AppSurface2),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TrackChanges,
                    contentDescription = null,
                    tint = Mint600,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Cân nặng mục tiêu: ${targetWeightKg(weightKg, goalType)} kg · trong 12 tuần",
                    fontSize = 13.sp,
                    color = Ink700
                )
            }
        }
    }
}

@Composable
fun Step4Activity(
    activityLevel: String,
    birthDate: String,
    gender: String,
    heightCm: Float,
    weightKg: Float,
    goalType: String,
    onActivityChange: (String) -> Unit
) {
    data class ActivityOption(
        val id: String,
        val title: String,
        val subtitle: String,
        val icon: androidx.compose.ui.graphics.vector.ImageVector
    )

    val opts = listOf(
        ActivityOption("sedentary", "Ít vận động", "Văn phòng, ít đi lại", Icons.Default.Person),
        ActivityOption("lightly_active", "Vận động nhẹ", "1-2 buổi tập / tuần", Icons.AutoMirrored.Filled.DirectionsWalk),
        ActivityOption("moderately_active", "Vừa phải", "3-5 buổi tập / tuần", Icons.AutoMirrored.Filled.DirectionsRun),
        ActivityOption("very_active", "Cao", "6+ buổi tập / tuần", Icons.Default.FitnessCenter)
    )

    Column {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            opts.forEach { option ->
                val isSelected = activityLevel == option.id
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(VitalRadius.Lg))
                        .background(if (isSelected) Mint50 else AppSurface2)
                        .border(
                            2.dp,
                            if (isSelected) Mint500 else Color.Transparent,
                            RoundedCornerShape(VitalRadius.Lg)
                        )
                        .clickable { onActivityChange(option.id) }
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Mint500 else AppSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = option.icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else Ink700,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = option.title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(text = option.subtitle, fontSize = 12.sp, color = Ink500)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Recommendation Card
        Card(
            shape = RoundedCornerShape(VitalRadius.Lg),
            colors = CardDefaults.cardColors(containerColor = Mint50),
            border = BorderStroke(1.dp, Mint200),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "KẾ HOẠCH ĐỀ XUẤT",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Mint700,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("${recommendedCalories(birthDate, gender, heightCm, weightKg, activityLevel, goalType)}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Mint700)
                        Text("kcal / ngày", fontSize = 12.sp, color = Mint700)
                    }
                    Column {
                        Text("${recommendedProtein(weightKg, goalType)}g", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Mint700)
                        Text("protein", fontSize = 12.sp, color = Mint700)
                    }
                    Column {
                        Text("${recommendedCarbs(birthDate, gender, heightCm, weightKg, activityLevel, goalType)}g", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Mint700)
                        Text("carbs", fontSize = 12.sp, color = Mint700)
                    }
                }
            }
        }
    }
}

private fun recommendedCalories(
    birthDate: String,
    gender: String,
    heightCm: Float,
    weightKg: Float,
    activityLevel: String,
    goalType: String
): Int {
    val age = birthDate.take(4).toIntOrNull()?.let { (java.time.LocalDate.now().year - it).coerceIn(14, 90) } ?: 30
    val bmr = if (gender == "female") {
        10f * weightKg + 6.25f * heightCm - 5f * age - 161f
    } else {
        10f * weightKg + 6.25f * heightCm - 5f * age + 5f
    }
    val activityFactor = when (activityLevel) {
        "sedentary" -> 1.2f
        "lightly_active" -> 1.375f
        "moderately_active" -> 1.55f
        "very_active" -> 1.725f
        else -> 1.45f
    }
    val goalAdjustment = when (goalType) {
        "lose_weight" -> -350
        "gain_muscle" -> 250
        else -> 0
    }
    return ((bmr * activityFactor) + goalAdjustment).roundToInt().coerceIn(1200, 4200)
}

private fun recommendedProtein(weightKg: Float, goalType: String): Int {
    val multiplier = if (goalType == "gain_muscle") 1.8f else 1.6f
    return (weightKg * multiplier).roundToInt().coerceAtLeast(40)
}

private fun recommendedCarbs(
    birthDate: String,
    gender: String,
    heightCm: Float,
    weightKg: Float,
    activityLevel: String,
    goalType: String
): Int {
    return ((recommendedCalories(birthDate, gender, heightCm, weightKg, activityLevel, goalType) * 0.45f) / 4f)
        .roundToInt()
        .coerceAtLeast(80)
}

private fun targetWeightKg(weightKg: Float, goalType: String): Int {
    val adjustment = when (goalType) {
        "lose_weight" -> -6
        "gain_muscle" -> 4
        else -> 0
    }
    return (weightKg + adjustment).roundToInt().coerceIn(40, 150)
}

fun birthYearFromDate(birthDate: String): Int {
    return birthDate.take(4).toIntOrNull() ?: 1996
}
