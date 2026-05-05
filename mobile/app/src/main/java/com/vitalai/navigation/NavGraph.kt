package com.vitalai.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.vitalai.ui.screens.auth.SignInScreen
import com.vitalai.ui.screens.auth.SignUpScreen
import com.vitalai.ui.screens.auth.WelcomeScreen
import com.vitalai.ui.screens.coach.CoachScreen
import com.vitalai.ui.screens.diary.CreateFoodScreen
import com.vitalai.ui.screens.diary.DiaryScreen
import com.vitalai.ui.screens.diary.FoodDetailScreen
import com.vitalai.ui.screens.diary.SearchFoodScreen
import com.vitalai.ui.screens.discover.BlogDetailScreen
import com.vitalai.ui.screens.discover.DiscoverScreen
import com.vitalai.ui.screens.home.HomeScreen
import com.vitalai.ui.screens.metrics.MetricsScreen
import com.vitalai.ui.screens.notifications.NotificationsScreen
import com.vitalai.ui.screens.onboarding.OnboardingScreen
import com.vitalai.ui.screens.profile.ProfileScreen
import com.vitalai.ui.screens.scan.ScanScreen
import com.vitalai.ui.screens.workout.WorkoutScreen

@Composable
fun VitalNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Welcome
    ) {
        // ── Auth ──────────────────────────────────────────────
        composable<Screen.Welcome> {
            WelcomeScreen(navController)
        }
        composable<Screen.SignIn> {
            SignInScreen(navController)
        }
        composable<Screen.SignUp> {
            SignUpScreen(navController)
        }
        composable<Screen.Onboarding> { backStackEntry ->
            val route: Screen.Onboarding = backStackEntry.toRoute()
            OnboardingScreen(route.step, navController)
        }

        // ── Main tabs ─────────────────────────────────────────
        composable<Screen.Home> {
            HomeScreen(navController)
        }
        composable<Screen.Diary> {
            DiaryScreen(navController)
        }
        composable<Screen.Coach> {
            CoachScreen(navController)
        }
        composable<Screen.Profile> {
            ProfileScreen(navController)
        }

        // ── Food / Diary sub-screens ──────────────────────────
        composable<Screen.SearchFood> { backStackEntry ->
            val route: Screen.SearchFood = backStackEntry.toRoute()
            SearchFoodScreen(navController, mealType = route.mealType, date = route.date)
        }
        composable<Screen.FoodDetail> { backStackEntry ->
            val route: Screen.FoodDetail = backStackEntry.toRoute()
            FoodDetailScreen(foodId = route.id, mealType = route.mealType, date = route.date, navController = navController)
        }
        composable<Screen.CreateFood> {
            CreateFoodScreen(navController)
        }

        // ── Workout / Metrics ─────────────────────────────────
        composable<Screen.Workout> {
            WorkoutScreen(navController)
        }
        composable<Screen.WorkoutSession> { backStackEntry ->
            val route: Screen.WorkoutSession = backStackEntry.toRoute()
            // Placeholder until WorkoutSessionScreen is implemented
            WorkoutScreen(navController)
        }
        composable<Screen.Metrics> {
            MetricsScreen(navController)
        }

        // ── Discover / Blog ───────────────────────────────────
        composable<Screen.Discover> {
            DiscoverScreen(navController)
        }
        composable<Screen.BlogDetail> { backStackEntry ->
            val route: Screen.BlogDetail = backStackEntry.toRoute()
            BlogDetailScreen(blogId = route.id, navController = navController)
        }

        // ── Notifications ─────────────────────────────────────
        composable<Screen.Notifications> {
            NotificationsScreen(navController)
        }

        // ── AI Scan ───────────────────────────────────────────
        composable<Screen.Scan> {
            ScanScreen(navController)
        }
    }
}
