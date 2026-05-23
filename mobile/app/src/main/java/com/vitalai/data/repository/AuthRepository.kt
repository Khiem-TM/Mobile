package com.vitalai.data.repository

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.vitalai.data.local.TokenManager
import com.vitalai.data.remote.AuthApi
import com.vitalai.data.remote.model.ApiResponse
import com.vitalai.data.remote.model.AuthResponseDto
import com.vitalai.data.remote.model.LoginRequest
import com.vitalai.data.remote.model.RegisterRequest
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
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
        return when (exception) {
            is UnknownHostException -> "Không thể kết nối máy chủ. Vui lòng kiểm tra mạng hoặc địa chỉ API."
            is SocketTimeoutException -> "Kết nối quá lâu. Vui lòng thử lại."
            is IOException -> "Lỗi kết nối mạng. Vui lòng thử lại."
            else -> exception.message ?: "Đã xảy ra lỗi. Vui lòng thử lại."
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
