package com.vitalai.data.repository

import com.vitalai.data.remote.UserApi
import com.vitalai.data.remote.model.HealthProfileDto
import com.vitalai.data.remote.model.UserDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userApi: UserApi
) {
    suspend fun getCurrentUser(): Result<UserDto> {
        return try {
            val response = userApi.getMe()
            val body = response.body()?.data
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception("Cannot fetch user profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHealthProfile(): Result<HealthProfileDto> {
        return try {
            val response = userApi.getHealthProfile()
            val body = response.body()?.data
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception("Cannot fetch health profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
