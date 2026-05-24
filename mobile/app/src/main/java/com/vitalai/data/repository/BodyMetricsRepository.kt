package com.vitalai.data.repository

import com.vitalai.data.remote.BodyMetricsApi
import com.vitalai.data.remote.model.BodyMetricDto
import com.vitalai.data.remote.model.BodyMetricsPeriodDto
import com.vitalai.data.remote.model.BodyMetricsSummaryDto
import com.vitalai.data.remote.model.ProgressPhotoDto
import com.vitalai.data.remote.model.UpsertBodyMetricRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BodyMetricsRepository @Inject constructor(
    private val bodyMetricsApi: BodyMetricsApi
) {
    suspend fun getLatest(): Result<BodyMetricDto> {
        return try {
            val response = bodyMetricsApi.getLatest()
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Không có dữ liệu"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSummary(): Result<BodyMetricsSummaryDto> {
        return try {
            val response = bodyMetricsApi.getSummary()
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Lỗi tải tổng quan cân nặng (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPeriod(period: String): Result<BodyMetricsPeriodDto> {
        return try {
            val response = bodyMetricsApi.getPeriod(period)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body.toPeriodDto())
            else Result.failure(Exception("Lỗi tải dữ liệu (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addMetric(metric: UpsertBodyMetricRequest): Result<BodyMetricDto> {
        return try {
            val response = bodyMetricsApi.addMetric(metric)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Lỗi lưu số liệu (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPhotos(limit: Int = 10): Result<List<ProgressPhotoDto>> {
        return try {
            val response = bodyMetricsApi.getPhotos(limit)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadPhoto(file: File, photoType: String, bodyMetricId: String? = null): Result<ProgressPhotoDto> {
        return try {
            val part = MultipartBody.Part.createFormData(
                name = "file",
                filename = file.name,
                body = file.asRequestBody("image/*".toMediaTypeOrNull())
            )
            val photoTypeBody = photoType.toRequestBody("text/plain".toMediaTypeOrNull())
            val metricBody = bodyMetricId?.toRequestBody("text/plain".toMediaTypeOrNull())
            val response = bodyMetricsApi.uploadPhoto(part, photoTypeBody, metricBody)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Lỗi tải ảnh tiến độ (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePhoto(id: String): Result<Unit> {
        return try {
            val response = bodyMetricsApi.deletePhoto(id)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Lỗi xóa ảnh tiến độ (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun List<BodyMetricDto>.toPeriodDto(): BodyMetricsPeriodDto {
        val weights = map { it.weightKg }
        return BodyMetricsPeriodDto(
            data = this,
            avgWeight = if (weights.isNotEmpty()) weights.average().toFloat() else 0f,
            minWeight = weights.minOrNull() ?: 0f,
            maxWeight = weights.maxOrNull() ?: 0f
        )
    }
}
