package com.vitalai.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.POST

/** Đăng ký/gỡ FCM token với backend (Notification Service). */
interface DeviceTokenApi {
    @POST("devices/token")
    suspend fun register(@Body body: Map<String, String>): Response<Unit>

    // DELETE có body -> dùng @HTTP(hasBody = true)
    @HTTP(method = "DELETE", path = "devices/token", hasBody = true)
    suspend fun unregister(@Body body: Map<String, String>): Response<Unit>
}
