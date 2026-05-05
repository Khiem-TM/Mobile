package com.vitalai.navigation

import kotlinx.serialization.Serializable

sealed class Screen {
    @Serializable object Welcome
    @Serializable object SignIn
    @Serializable object SignUp
    @Serializable data class Onboarding(val step: Int)
    @Serializable object Home
    @Serializable object Diary
    @Serializable object Scan
    @Serializable object Coach
    @Serializable object Profile
    @Serializable data class SearchFood(val mealType: String = "", val date: String = "")
    @Serializable data class FoodDetail(val id: String, val mealType: String = "", val date: String = "")
    @Serializable object CreateFood
    @Serializable object Workout
    @Serializable data class WorkoutSession(val id: String)
    @Serializable object Metrics
    @Serializable object Notifications
    @Serializable data class BlogDetail(val id: String)
    @Serializable object Discover
}
