package com.vitalai.core.error

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

sealed class AppError(open val userMessage: String) {
    data class Network(override val userMessage: String) : AppError(userMessage)
    data class Unauthorized(override val userMessage: String = "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.") : AppError(userMessage)
    data class Server(override val userMessage: String = "Máy chủ đang gặp sự cố. Vui lòng thử lại sau.") : AppError(userMessage)
    data class Validation(override val userMessage: String) : AppError(userMessage)
    data class Unknown(override val userMessage: String = "Đã xảy ra lỗi. Vui lòng thử lại.") : AppError(userMessage)
}

object AppErrorMapper {
    fun fromHttpCode(code: Int, fallback: String? = null): AppError {
        return when (code) {
            400, 422 -> AppError.Validation(fallback ?: "Dữ liệu không hợp lệ.")
            401, 403 -> AppError.Unauthorized(fallback ?: "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.")
            in 500..599 -> AppError.Server(fallback ?: "Máy chủ đang gặp sự cố. Vui lòng thử lại sau.")
            else -> AppError.Unknown(fallback ?: "Yêu cầu thất bại ($code).")
        }
    }

    fun fromThrowable(throwable: Throwable): AppError {
        return when (throwable) {
            is UnknownHostException -> AppError.Network("Không thể kết nối máy chủ. Vui lòng kiểm tra mạng.")
            is SocketTimeoutException -> AppError.Network("Kết nối quá lâu. Vui lòng thử lại.")
            is IOException -> AppError.Network("Lỗi kết nối mạng. Vui lòng thử lại.")
            else -> AppError.Unknown(throwable.message ?: "Đã xảy ra lỗi. Vui lòng thử lại.")
        }
    }
}
