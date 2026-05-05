package com.vitalai.data.repository

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.vitalai.data.local.TokenManager
import com.vitalai.data.remote.AuthApi
import com.vitalai.data.remote.model.ApiResponse
import com.vitalai.data.remote.model.AuthResponseDto
import com.vitalai.data.remote.model.LoginRequest
import com.vitalai.data.remote.model.RegisterRequest
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val tokenManager: TokenManager,
    private val moshi: Moshi
) {
    private fun parseErrorMessage(response: Response<*>, fallback: String): String {
        return try {
            val errorBody = response.errorBody()?.string()
            if (!errorBody.isNullOrBlank()) {
                val adapter = moshi.adapter(ApiResponse::class.java)
                val parsed = adapter.fromJson(errorBody)
                parsed?.message ?: fallback
            } else fallback
        } catch (_: Exception) {
            fallback
        }
    }

    suspend fun login(request: LoginRequest): Result<AuthResponseDto> {
        return try {
            val response = authApi.login(request)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) {
                tokenManager.saveTokens(body.accessToken, body.refreshToken)
                Result.success(body)
            } else {
                val msg = parseErrorMessage(response, "Đăng nhập thất bại (${response.code()})")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(request: RegisterRequest): Result<AuthResponseDto> {
        return try {
            val response = authApi.register(request)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) {
                tokenManager.saveTokens(body.accessToken, body.refreshToken)
                Result.success(body)
            } else {
                val msg = parseErrorMessage(response, "Đăng ký thất bại (${response.code()})")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginWithGoogle(idToken: String): Result<AuthResponseDto> {
        return try {
            val response = authApi.googleMobileLogin(mapOf("id_token" to idToken))
            val body = response.body()?.data
            if (response.isSuccessful && body != null) {
                tokenManager.saveTokens(body.accessToken, body.refreshToken)
                Result.success(body)
            } else {
                val msg = parseErrorMessage(response, "Đăng nhập Google thất bại (${response.code()})")
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        tokenManager.clearTokens()
    }
}
