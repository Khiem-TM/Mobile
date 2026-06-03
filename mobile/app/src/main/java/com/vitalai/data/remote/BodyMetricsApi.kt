package com.vitalai.data.remote

import com.vitalai.data.remote.model.ApiResponse
import com.vitalai.data.remote.model.AdvancedBodyMetricRequest
import com.vitalai.data.remote.model.BasicBodyMetricRequest
import com.vitalai.data.remote.model.BodyMetricDto
import com.vitalai.data.remote.model.BodyMetricsSummaryDto
import com.vitalai.data.remote.model.ProgressPhotoDto
import com.vitalai.data.remote.model.UpsertBodyMetricRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface BodyMetricsApi {
    @GET("body-metrics/latest")
    suspend fun getLatest(): Response<ApiResponse<BodyMetricDto>>

    @GET("body-metrics/summary")
    suspend fun getSummary(): Response<ApiResponse<BodyMetricsSummaryDto>>

    @GET("body-metrics/period/{period}")
    suspend fun getPeriod(@Path("period") period: String): Response<ApiResponse<List<BodyMetricDto>>>

    @GET("body-metrics/history")
    suspend fun getHistory(
        @Query("fromDate") fromDate: String,
        @Query("toDate") toDate: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): Response<ApiResponse<List<BodyMetricDto>>>

    @POST("body-metrics")
    suspend fun addMetric(@Body metric: UpsertBodyMetricRequest): Response<ApiResponse<BodyMetricDto>>

    @POST("body-metrics/basic")
    suspend fun updateBasic(@Body metric: BasicBodyMetricRequest): Response<ApiResponse<BodyMetricDto>>

    @POST("body-metrics/advanced")
    suspend fun updateAdvanced(@Body metric: AdvancedBodyMetricRequest): Response<ApiResponse<BodyMetricDto>>

    @GET("body-metrics/photos")
    suspend fun getPhotos(@Query("limit") limit: Int = 10): Response<ApiResponse<List<ProgressPhotoDto>>>

    @Multipart
    @POST("body-metrics/photos")
    suspend fun uploadPhoto(
        @Part file: MultipartBody.Part,
        @Part("photoType") photoType: RequestBody,
        @Part("bodyMetricId") bodyMetricId: RequestBody? = null
    ): Response<ApiResponse<ProgressPhotoDto>>

    @DELETE("body-metrics/photos/{id}")
    suspend fun deletePhoto(@Path("id") id: String): Response<Unit>
}
