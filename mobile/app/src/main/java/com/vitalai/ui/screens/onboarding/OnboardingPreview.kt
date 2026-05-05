package com.vitalai.ui.screens.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.vitalai.ui.theme.VitalAITheme

@Preview(showBackground = true, name = "Step 1: Gender")
@Composable
fun OnboardingStep1Preview() {
    VitalAITheme {
        OnboardingScreen(step = 1, navController = rememberNavController())
    }
}

@Preview(showBackground = true, name = "Step 2: Metrics")
@Composable
fun OnboardingStep2Preview() {
    VitalAITheme {
        OnboardingScreen(step = 2, navController = rememberNavController())
    }
}

@Preview(showBackground = true, name = "Step 3: Activity")
@Composable
fun OnboardingStep3Preview() {
    VitalAITheme {
        OnboardingScreen(step = 3, navController = rememberNavController())
    }
}

@Preview(showBackground = true, name = "Step 4: Goal")
@Composable
fun OnboardingStep4Preview() {
    VitalAITheme {
        OnboardingScreen(step = 4, navController = rememberNavController())
    }
}
