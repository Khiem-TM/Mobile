package com.vitalai.ui.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SportsMartialArts
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
                .padding(top = 60.dp, bottom = 44.dp)
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
                        .background(ForestGreen)
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
                        tint = Color.White
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
                1 -> "Cho chúng tôi biết thêm về bạn"
                2 -> "Số đo cơ thể"
                3 -> "Mục tiêu chính của bạn?"
                4 -> "Mức độ vận động"
                else -> ""
            }
            val subtitle = when (step) {
                1 -> "Dữ liệu này sẽ được dùng để xây dựng kế hoạch dinh dưỡng cá nhân hóa cho bạn"
                2 -> "Chiều cao và cân nặng hiện tại của bạn"
                3 -> ""
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
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 15.sp,
                    color = Ink500,
                    modifier = Modifier.padding(bottom = 28.dp)
                )
            }

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
                                onGoalChange = { viewModel.goalType = it }
                            )
                            4 -> Step4Activity(
                                activityLevel = viewModel.activityLevel,
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
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
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
    data class GenderOption(
        val id: String,
        val label: String,
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val selectedColor: Color
    )

    val options = listOf(
        GenderOption("male", "Nam", Icons.Default.Male, ForestGreen),
        GenderOption("female", "Nữ", Icons.Default.Female, ForestGreen)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        options.forEach { option ->
            val isSelected = gender == option.id
            val bg = if (isSelected) option.selectedColor else Color(0xFFF3F4F6)
            val fg = if (isSelected) Color.White else Ink500
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(VitalRadius.Lg))
                    .background(bg)
                    .clickable { onGenderChange(option.id) }
                    .padding(vertical = 28.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = option.label,
                        tint = fg,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = option.label,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = fg
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
            .background(Color(0xFFF3F4F6))
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
                    .background(ForestGreen)
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
    onGoalChange: (String) -> Unit
) {
    val goals = listOf(
        Triple("lose_weight", "Giảm cân", "Đốt mỡ, giảm cân nặng"),
        Triple("gain_weight", "Tăng cân", "Tăng cân nặng tổng thể"),
        Triple("gain_muscle", "Tăng cơ", "Xây dựng cơ bắp, tăng sức mạnh"),
        Triple("improve_endurance", "Tăng sức bền", "Cải thiện thể lực và sức bền"),
        Triple("bulking", "Xả cơ", "Tăng cơ kèm tăng cân"),
        Triple("cutting", "Siết cơ", "Giảm mỡ, giữ cơ bắp"),
        Triple("maintain", "Giữ cân", "Duy trì vóc dáng cân đối")
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        goals.forEach { (id, title, sub) ->
            val isSelected = goalType == id
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(VitalRadius.Lg))
                    .background(if (isSelected) ForestGreen else Color(0xFFF3F4F6))
                    .clickable { onGoalChange(id) }
                    .padding(vertical = 12.dp, horizontal = 16.dp)
            ) {
                Column {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else Ink900
                    )
                    Text(
                        text = sub,
                        fontSize = 12.sp,
                        color = if (isSelected) Color.White else Ink500
                    )
                }
            }
        }
    }
}

@Composable
fun Step4Activity(
    activityLevel: String,
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
        ActivityOption("very_active", "Cao", "6+ buổi tập / tuần", Icons.Default.FitnessCenter),
        ActivityOption("extra_active", "Cực cao", "Vận động viên / Lao động nặng", Icons.Default.SportsMartialArts)
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        opts.forEach { option ->
            val isSelected = activityLevel == option.id
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(VitalRadius.Lg))
                    .background(if (isSelected) ForestGreen else Color(0xFFF3F4F6))
                    .clickable { onActivityChange(option.id) }
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else Ink500,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = option.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Ink900
                        )
                        Text(
                            text = option.subtitle,
                            fontSize = 12.sp,
                            color = if (isSelected) Color.White else Ink500
                        )
                    }
                }
            }
        }

        Text(
            text = "Lưu ý: Bạn vẫn có thể chỉnh sửa những thông tin trên bất kỳ lúc nào!",
            fontSize = 12.sp,
            color = Ink500,
            modifier = Modifier.padding(top = 15.dp)
        )
    }
}

fun birthYearFromDate(birthDate: String): Int {
    return birthDate.take(4).toIntOrNull() ?: 1996
}
