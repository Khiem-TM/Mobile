package com.vitalai.ui.screens.workout.screens

import com.vitalai.ui.screens.workout.components.*
import com.vitalai.ui.screens.workout.viewmodels.*

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.vitalai.navigation.Screen
import com.vitalai.ui.screens.workout.components.WorkoutDashboard

@Composable
fun WorkoutScreen(
    navController: NavController,
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Reload session history whenever the screen resumes after saving a workout.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.loadData()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    WorkoutDashboard(
        uiState = uiState,
        onNavigateWorkoutBuilder = { navController.navigate(Screen.WorkoutBuilder) },
        onNavigateLibrary = { navController.navigate(Screen.ExerciseLibrary) },
        onNavigateMetrics = { navController.navigate(Screen.TrainBodyMetrics) },
        onNavigateWorkoutHistory = { navController.navigate(Screen.WorkoutHistory) },
        onPreviousWeek = viewModel::showPreviousWeek,
        onNextWeek = viewModel::showNextWeek,
        onSessionClick = { session ->
            navController.navigate(Screen.WorkoutSession(session.id, session.sessionDate))
        },
        onRetry = viewModel::loadData,
        onBackClick = { navController.popBackStack() }
    )
}
