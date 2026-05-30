package com.vitalai.data.repository

import com.vitalai.data.remote.UserApi
import com.vitalai.data.remote.model.HealthProfileDto
import com.vitalai.data.remote.model.UpdateProfileRequest
import com.vitalai.data.remote.model.UserDto
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

class HealthProfileMissingException : Exception("Health profile not found")

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
            } else if (response.code() == 404) {
                Result.failure(HealthProfileMissingException())
            } else {
                Result.failure(Exception("Cannot fetch health profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateHealthProfile(profile: HealthProfileDto): Result<HealthProfileDto> {
        return try {
            val response = userApi.updateHealthProfile(profile)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception("Cannot update health profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        userId: String,
        displayName: String? = null,
        avatarUrl: String? = null
    ): Result<UserDto> {
        return try {
            val response = userApi.updateProfile(
                userId,
                UpdateProfileRequest(displayName = displayName, avatarUrl = avatarUrl)
            )
            val body = response.body()?.data
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(Exception("Cannot update user profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadAvatar(file: File, mimeType: String): Result<UserDto> {
        return try {
            val part = MultipartBody.Part.createFormData(
                name = "file",
                filename = file.name,
                body = file.asRequestBody(mimeType.toMediaTypeOrNull())
            )
            val response = userApi.uploadAvatar(part)
            if (response.isSuccessful) {
                getCurrentUser()
            } else {
                val errorBody = response.errorBody()?.string()
                val message = errorBody
                    ?.takeIf { it.isNotBlank() }
                    ?: "Cannot upload avatar (${response.code()})"
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
