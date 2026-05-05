package com.vitalai.di

import com.vitalai.data.remote.AuthApi
import com.vitalai.data.remote.BlogApi
import com.vitalai.data.remote.BodyMetricsApi
import com.vitalai.data.remote.ChatbotApi
import com.vitalai.data.remote.DashboardApi
import com.vitalai.data.remote.FoodApi
import com.vitalai.data.remote.MealLogApi
import com.vitalai.data.remote.NotificationsApi
import com.vitalai.data.remote.TrainingApi
import com.vitalai.data.remote.UserApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi = retrofit.create(UserApi::class.java)

    @Provides
    @Singleton
    fun provideDashboardApi(retrofit: Retrofit): DashboardApi = retrofit.create(DashboardApi::class.java)

    @Provides
    @Singleton
    fun provideMealLogApi(retrofit: Retrofit): MealLogApi = retrofit.create(MealLogApi::class.java)

    @Provides
    @Singleton
    fun provideFoodApi(retrofit: Retrofit): FoodApi = retrofit.create(FoodApi::class.java)

    @Provides
    @Singleton
    fun provideTrainingApi(retrofit: Retrofit): TrainingApi = retrofit.create(TrainingApi::class.java)

    @Provides
    @Singleton
    fun provideBodyMetricsApi(retrofit: Retrofit): BodyMetricsApi = retrofit.create(BodyMetricsApi::class.java)

    @Provides
    @Singleton
    fun provideChatbotApi(retrofit: Retrofit): ChatbotApi = retrofit.create(ChatbotApi::class.java)

    @Provides
    @Singleton
    fun provideNotificationsApi(retrofit: Retrofit): NotificationsApi = retrofit.create(NotificationsApi::class.java)

    @Provides
    @Singleton
    fun provideBlogApi(retrofit: Retrofit): BlogApi = retrofit.create(BlogApi::class.java)
}
