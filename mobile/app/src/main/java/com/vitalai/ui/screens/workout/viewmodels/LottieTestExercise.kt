package com.vitalai.ui.screens.workout.viewmodels

import com.vitalai.data.remote.model.ExerciseDto

const val LOTTIE_TEST_EXERCISE_ID = "lottie_test_plank_chu_t"

val LOTTIE_TEST_EXERCISE = ExerciseDto(
    id = LOTTIE_TEST_EXERCISE_ID,
    name = "[Test Lottie] Plank chữ T",
    primaryMuscleGroup = "FULL_BODY",
    category = "Test",
    difficultyLevel = "BEGINNER",
    description = "Bài tập tạm để kiểm tra hoạt ảnh Lottie (Plank_chu_T.lottie).",
    exerciseType = "GYM",
    favoritesCount = 999_999,
    lottieAsset = "Plank_chu_T.lottie"
)
