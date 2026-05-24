package com.vitalai.data.remote

import com.vitalai.data.remote.model.ApiResponse
import com.vitalai.data.remote.model.BodyMetricDto
import com.vitalai.data.remote.model.UpsertBodyMetricRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface BodyMetricsApi {
    @GET("body-metrics/latest")
    suspend fun getLatest(): Response<ApiResponse<BodyMetricDto>>

    @GET("body-metrics/period/{period}")
    suspend fun getPeriod(@Path("period") period: String): Response<ApiResponse<List<BodyMetricDto>>>

    @POST("body-metrics")
    suspend fun addMetric(@Body metric: UpsertBodyMetricRequest): Response<ApiResponse<BodyMetricDto>>
}
