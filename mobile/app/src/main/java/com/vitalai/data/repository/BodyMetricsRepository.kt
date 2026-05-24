package com.vitalai.data.repository

import com.vitalai.data.remote.BodyMetricsApi
import com.vitalai.data.remote.model.BodyMetricDto
import com.vitalai.data.remote.model.BodyMetricsPeriodDto
import com.vitalai.data.remote.model.UpsertBodyMetricRequest
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
