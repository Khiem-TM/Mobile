package com.vitalai.navigation

import kotlinx.serialization.Serializable

sealed class Screen {
    @Serializable object Welcome
    @Serializable object SignIn
    @Serializable object SignUp
    @Serializable object ForgotPassword
    @Serializable data class ResetPassword(val token: String = "")
    @Serializable data class Onboarding(val step: Int)
    @Serializable object Home
    @Serializable object Diary
    @Serializable object Scan
    @Serializable object Coach
    @Serializable object Profile
    @Serializable data class SearchFood(val mealType: String = "", val date: String = "", val initialTab: Int = 0)
    @Serializable data class FoodDetail(val id: String, val mealType: String = "", val date: String = "")
    @Serializable object CreateFood
    @Serializable object MealHistory
    @Serializable object Workout
    @Serializable object WorkoutHistory
    @Serializable data class WorkoutSession(val id: String, val date: String = "")
    @Serializable object TrainBodyMetrics
    @Serializable object Metrics
    @Serializable object Goals
    @Serializable object Notifications
    @Serializable data class BlogDetail(val id: String)
    @Serializable data class AuthorBlogs(
        val authorId: String,
        val authorName: String = "",
        val avatarUrl: String? = null
    )
    @Serializable object Discover
    @Serializable object BlogSearch
    @Serializable data class Activity(val date: String = "")
    @Serializable object ExerciseLibrary
    @Serializable data class ExerciseDetail(val id: String = "")
    @Serializable object WorkoutBuilder
    @Serializable object MetricsHistory
    @Serializable data class BlogComposer(val blogId: String? = null)
    @Serializable object MyBlogs
}
