package com.vitalai.data.repository

import com.vitalai.data.local.room.dao.HealthProfileDao
import com.vitalai.data.local.room.dao.UserProfileDao
import com.vitalai.data.mapper.toDto
import com.vitalai.data.mapper.toEntity
import com.vitalai.data.remote.UserApi
import com.vitalai.data.remote.model.HealthProfileDto
import com.vitalai.data.remote.model.UpdateProfileRequest
import com.vitalai.data.remote.model.UserDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

class HealthProfileMissingException : Exception("Health profile not found")

@Singleton
class UserRepository @Inject constructor(
    private val userApi: UserApi,
    private val userProfileDao: UserProfileDao,
    private val healthProfileDao: HealthProfileDao
) {
    // ── Room observation ──────────────────────────────────────────────────────

    fun observeCurrentUser(): Flow<UserDto?> =
        userProfileDao.observe().map { it?.toDto() }

    fun observeHealthProfile(): Flow<HealthProfileDto?> =
        healthProfileDao.observe().map { it?.toDto() }

    // ── Network refresh (cache-first + background refresh) ────────────────────

    suspend fun refreshCurrentUser() {
        runCatching { userApi.getMe() }
            .onSuccess { res -> res.body()?.data?.let { userProfileDao.upsert(it.toEntity()) } }
    }

    suspend fun refreshHealthProfile() {
        runCatching { userApi.getHealthProfile() }
            .onSuccess { res -> res.body()?.data?.let { healthProfileDao.upsert(it.toEntity()) } }
    }

    // ── Suspend functions (cache-first, fallback to network) ──────────────────

    suspend fun getCurrentUser(): Result<UserDto> {
        userProfileDao.get()?.toDto()?.let { return Result.success(it) }
        return try {
            val response = userApi.getMe()
            val body = response.body()?.data
            if (response.isSuccessful && body != null) {
                userProfileDao.upsert(body.toEntity())
                Result.success(body)
            } else {
                Result.failure(Exception("Cannot fetch user profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHealthProfile(): Result<HealthProfileDto> {
        healthProfileDao.get()?.toDto()?.let { return Result.success(it) }
        return try {
            val response = userApi.getHealthProfile()
            val body = response.body()?.data
            if (response.isSuccessful && body != null) {
                healthProfileDao.upsert(body.toEntity())
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

    // ── Write operations (network first, then update Room) ────────────────────

    suspend fun updateHealthProfile(profile: HealthProfileDto): Result<HealthProfileDto> {
        return try {
            val response = userApi.updateHealthProfile(profile)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) {
                healthProfileDao.upsert(body.toEntity())
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
                userProfileDao.upsert(body.toEntity())
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
                val fetchResponse = userApi.getMe()
                val body = fetchResponse.body()?.data
                if (fetchResponse.isSuccessful && body != null) {
                    userProfileDao.upsert(body.toEntity())
                    Result.success(body)
                } else {
                    getCurrentUser()
                }
            } else {
                val message = response.errorBody()?.string()
                    ?.takeIf { it.isNotBlank() }
                    ?: "Cannot upload avatar (${response.code()})"
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
