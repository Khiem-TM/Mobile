package com.vitalai.di

import android.content.Context
import androidx.room.Room
import com.vitalai.data.local.room.AppDatabase
import com.vitalai.data.local.room.dao.ActivityLogDao
import com.vitalai.data.local.room.dao.BodyMetricDao
import com.vitalai.data.local.room.dao.HealthProfileDao
import com.vitalai.data.local.room.dao.MealLogDao
import com.vitalai.data.local.room.dao.PendingSyncActionDao
import com.vitalai.data.local.room.dao.ExerciseDao
import com.vitalai.data.local.room.dao.FoodCacheDao
import com.vitalai.data.local.room.dao.StreakDao
import com.vitalai.data.local.room.dao.UserProfileDao
import com.vitalai.data.local.room.dao.WorkoutSessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "vital_ai_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun providePendingSyncActionDao(db: AppDatabase): PendingSyncActionDao = db.pendingSyncActionDao()

    @Provides
    @Singleton
    fun provideMealLogDao(db: AppDatabase): MealLogDao = db.mealLogDao()

    @Provides
    @Singleton
    fun provideBodyMetricDao(db: AppDatabase): BodyMetricDao = db.bodyMetricDao()

    @Provides
    @Singleton
    fun provideUserProfileDao(db: AppDatabase): UserProfileDao = db.userProfileDao()

    @Provides
    @Singleton
    fun provideHealthProfileDao(db: AppDatabase): HealthProfileDao = db.healthProfileDao()

    @Provides
    @Singleton
    fun provideStreakDao(db: AppDatabase): StreakDao = db.streakDao()

    @Provides
    @Singleton
    fun provideExerciseDao(db: AppDatabase): ExerciseDao = db.exerciseDao()

    @Provides
    @Singleton
    fun provideFoodCacheDao(db: AppDatabase): FoodCacheDao = db.foodCacheDao()

    @Provides
    @Singleton
    fun provideWorkoutSessionDao(db: AppDatabase): WorkoutSessionDao = db.workoutSessionDao()

    @Provides
    @Singleton
    fun provideActivityLogDao(db: AppDatabase): ActivityLogDao = db.activityLogDao()
}
