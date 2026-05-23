package com.vitalai.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vitalai.ui.theme.AppBackground
import com.vitalai.ui.theme.AppSurface2
import com.vitalai.ui.theme.Ink100
import com.vitalai.ui.theme.Ink500
import com.vitalai.ui.theme.Ink900
import com.vitalai.ui.theme.Mint500
import com.vitalai.ui.theme.VitalAITheme
import com.vitalai.ui.theme.VitalRadius

@Preview(showBackground = true, showSystemUi = true, name = "Onboarding Step 1 - Gender")
@Composable
fun OnboardingStep1Preview() {
    var gender by remember { mutableStateOf("male") }
    var birthYear by remember { mutableIntStateOf(1996) }

    VitalAITheme {
        OnboardingPreviewShell(
            step = 1,
            title = "Bạn là?",
            subtitle = "Giúp chúng tôi cá nhân hóa kế hoạch dinh dưỡng phù hợp với bạn"
        ) {
            Step1Gender(
                gender = gender,
                birthYear = birthYear,
                onGenderChange = { gender = it },
                onBirthYearChange = { birthYear = it }
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Onboarding Step 2 - Metrics")
@Composable
fun OnboardingStep2Preview() {
    var heightCm by remember { mutableFloatStateOf(172f) }
    var weightKg by remember { mutableFloatStateOf(68f) }

    VitalAITheme {
        OnboardingPreviewShell(
            step = 2,
            title = "Số đo cơ thể",
            subtitle = "Chiều cao và cân nặng hiện tại của bạn"
        ) {
            Step2BodyMetrics(
                heightCm = heightCm,
                weightKg = weightKg,
                onHeightChange = { heightCm = it },
                onWeightChange = { weightKg = it }
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Onboarding Step 3 - Goal")
@Composable
fun OnboardingStep3Preview() {
    var goalType by remember { mutableStateOf("lose_weight") }

    VitalAITheme {
        OnboardingPreviewShell(
            step = 3,
            title = "Mục tiêu chính của bạn?",
            subtitle = "Chúng tôi sẽ điều chỉnh kế hoạch calo theo mục tiêu này"
        ) {
            Step3Goal(
                goalType = goalType,
                weightKg = 68f,
                onGoalChange = { goalType = it }
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Onboarding Step 4 - Activity")
@Composable
fun OnboardingStep4Preview() {
    var activityLevel by remember { mutableStateOf("moderately_active") }

    VitalAITheme {
        OnboardingPreviewShell(
            step = 4,
            title = "Mức độ vận động",
            subtitle = "Tần suất bạn tập luyện trong tuần",
            nextLabel = "Hoàn tất"
        ) {
            Step4Activity(
                activityLevel = activityLevel,
                birthDate = "1996-01-01",
                gender = "male",
                heightCm = 172f,
                weightKg = 68f,
                goalType = "lose_weight",
                onActivityChange = { activityLevel = it }
            )
        }
    }
}

@Composable
private fun OnboardingPreviewShell(
    step: Int,
    title: String,
    subtitle: String,
    nextLabel: String = "Tiếp tục",
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .padding(horizontal = 28.dp)
            .padding(top = 60.dp, bottom = 28.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Ink100),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Back",
                    tint = Ink900
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

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
                        .fillMaxWidth(step / 4f)
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

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            content()
        }

        Button(
            onClick = {},
            colors = ButtonDefaults.buttonColors(containerColor = Ink900),
            shape = RoundedCornerShape(VitalRadius.Pill),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text(
                text = nextLabel,
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
