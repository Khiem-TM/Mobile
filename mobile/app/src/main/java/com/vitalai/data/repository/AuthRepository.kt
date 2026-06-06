package com.vitalai.data.repository

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.vitalai.core.error.AppErrorMapper
import com.vitalai.core.notification.DeviceTokenRegistrar
import com.vitalai.data.local.datastore.TokenManager
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
    private val deviceTokenRegistrar: DeviceTokenRegistrar,
    private val moshi: Moshi
) {
    /** Đẩy FCM token lên backend sau khi đã có access token (best-effort). */
    private suspend fun syncDeviceToken() {
        runCatching { deviceTokenRegistrar.ensureRegistered() }
    }
    private fun parseErrorMessage(response: Response<*>, fallback: String): String {
        return try {
            val errorBody = response.errorBody()?.string()
            if (!errorBody.isNullOrBlank()) {
                val type = Types.newParameterizedType(ApiResponse::class.java, Any::class.java)
                val adapter = moshi.adapter<ApiResponse<Any>>(type)
                val parsed = adapter.fromJson(errorBody)
                parsed?.message ?: fallback
            } else fallback
        } catch (_: Exception) {
            fallback
        }
    }

    private fun loginErrorMessage(response: Response<*>): String {
        val fallback = when (response.code()) {
            400 -> "Thông tin đăng nhập không hợp lệ."
            401 -> "Email hoặc mật khẩu không đúng."
            403 -> "Tài khoản chưa được xác minh hoặc đã bị vô hiệu hóa."
            404 -> "Không tìm thấy tài khoản."
            429 -> "Bạn thử đăng nhập quá nhiều lần. Vui lòng thử lại sau."
            in 500..599 -> "Máy chủ đang gặp sự cố. Vui lòng thử lại sau."
            else -> "Đăng nhập thất bại (${response.code()})"
        }
        return parseErrorMessage(response, fallback)
    }

    private fun authExceptionMessage(exception: Exception): String {
        return AppErrorMapper.fromThrowable(exception).userMessage
    }

    private fun authMessage(response: ApiResponse<Map<String, String>>?, fallback: String): String {
        return response?.message ?: response?.data?.get("message") ?: fallback
    }

    suspend fun login(request: LoginRequest): Result<AuthResponseDto> {
        return try {
            val response = authApi.login(request)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) {
                tokenManager.saveTokens(body.accessToken, body.refreshToken)
                syncDeviceToken()
                Result.success(body)
            } else {
                Result.failure(Exception(loginErrorMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(authExceptionMessage(e), e))
        }
    }

    suspend fun register(request: RegisterRequest): Result<AuthResponseDto> {
        return try {
            val response = authApi.register(request)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) {
                tokenManager.saveTokens(body.accessToken, body.refreshToken)
                syncDeviceToken()
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
                syncDeviceToken()
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
        // Gỡ FCM token trước khi xoá access token (endpoint cần JWT).
        runCatching { deviceTokenRegistrar.unregisterCurrent() }
        try {
            val refreshToken = tokenManager.getRefreshToken()
            authApi.logout(mapOf("refresh_token" to refreshToken))
        } catch (_: Exception) {
        }
        tokenManager.clearTokens()
    }

    suspend fun forgotPassword(email: String): Result<String> {
        return try {
            val response = authApi.forgotPassword(mapOf("email" to email.trim()))
            if (response.isSuccessful) {
                Result.success(authMessage(response.body(), "Hướng dẫn đặt lại mật khẩu đã được gửi nếu email tồn tại."))
            } else {
                Result.failure(Exception(parseErrorMessage(response, "Không gửi được hướng dẫn (${response.code()})")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(authExceptionMessage(e), e))
        }
    }

    suspend fun resetPassword(token: String, newPassword: String): Result<String> {
        return try {
            val response = authApi.resetPassword(
                mapOf("token" to token.trim(), "newPassword" to newPassword)
            )
            if (response.isSuccessful) {
                Result.success(authMessage(response.body(), "Đặt lại mật khẩu thành công."))
            } else {
                Result.failure(Exception(parseErrorMessage(response, "Không đặt lại được mật khẩu (${response.code()})")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(authExceptionMessage(e), e))
        }
    }
}
