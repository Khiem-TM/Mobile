package com.vitalai.di

import android.content.Context
import androidx.room.Room
import com.vitalai.data.local.room.AppDatabase
import com.vitalai.data.local.room.dao.BodyMetricDao
import com.vitalai.data.local.room.dao.HealthProfileDao
import com.vitalai.data.local.room.dao.MealLogDao
import com.vitalai.data.local.room.dao.PendingSyncActionDao
import com.vitalai.data.local.room.dao.UserProfileDao
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
}
