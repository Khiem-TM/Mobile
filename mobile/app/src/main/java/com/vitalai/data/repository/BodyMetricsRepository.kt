package com.vitalai.data.repository

import com.squareup.moshi.Moshi
import com.vitalai.core.sync.SyncScheduler
import com.vitalai.data.local.room.dao.BodyMetricDao
import com.vitalai.data.local.room.dao.PendingSyncActionDao
import com.vitalai.data.local.room.entity.BodyMetricEntity
import com.vitalai.data.local.room.entity.PendingSyncActionEntity
import com.vitalai.data.mapper.toDateRange
import com.vitalai.data.mapper.toDto
import com.vitalai.data.mapper.toEntity
import com.vitalai.data.remote.BodyMetricsApi
import com.vitalai.data.remote.model.AdvancedBodyMetricRequest
import com.vitalai.data.remote.model.BasicBodyMetricRequest
import com.vitalai.data.remote.model.BodyMetricDto
import com.vitalai.data.remote.model.BodyMetricsPeriodDto
import com.vitalai.data.remote.model.BodyMetricsSummaryDto
import com.vitalai.data.remote.model.ProgressPhotoDto
import com.vitalai.data.remote.model.UpsertBodyMetricRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BodyMetricsRepository @Inject constructor(
    private val bodyMetricsApi: BodyMetricsApi,
    private val bodyMetricDao: BodyMetricDao,
    private val pendingSyncActionDao: PendingSyncActionDao,
    private val moshi: Moshi,
    private val syncScheduler: SyncScheduler,
) {
    // ── Room observation (single source of truth) ─────────────────────────────

    fun observeLatest(): Flow<BodyMetricEntity?> = bodyMetricDao.observeLatest()

    fun observeLatestWithMeasurements(): Flow<BodyMetricEntity?> =
        bodyMetricDao.observeLatestWithMeasurements()

    fun observeEarliestDate(): Flow<LocalDate?> =
        bodyMetricDao.observeEarliestDate().map { raw ->
            raw?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        }

    fun observeWeekRange(from: LocalDate, to: LocalDate): Flow<BodyMetricsPeriodDto> =
        bodyMetricDao.observeByDateRange(from.toString(), to.toString())
            .map { entities -> entities.map { it.toDto() }.toPeriodDto() }

    fun observePeriod(period: String): Flow<BodyMetricsPeriodDto> {
        val (from, to) = period.toDateRange()
        return bodyMetricDao.observeByDateRange(from, to)
            .map { entities -> entities.map { it.toDto() }.toPeriodDto() }
    }

    fun observeSummary(): Flow<BodyMetricsSummaryDto> =
        bodyMetricDao.observeAll().map { entities ->
            if (entities.isEmpty()) return@map BodyMetricsSummaryDto()
            BodyMetricsSummaryDto(
                startWeight = entities.last().weightKg,
                currentWeight = entities.first().weightKg,
                weightChange = entities.first().weightKg - entities.last().weightKg,
                startDate = entities.last().measuredAt,
                latestDate = entities.first().measuredAt,
                totalRecords = entities.size
            )
        }

    fun observeHistory(): Flow<List<BodyMetricDto>> =
        bodyMetricDao.observeAll().map { entities -> entities.map { it.toDto() } }

    // ── Network refresh ───────────────────────────────────────────────────────

    suspend fun refreshFromNetwork() {
        try {
            val today = LocalDate.now().toString()
            val fromDate = LocalDate.now().minusYears(5).toString()
            val response = bodyMetricsApi.getHistory(
                fromDate = fromDate,
                toDate = today,
                page = 1,
                limit = 500
            )
            val items = response.body()?.data
            if (response.isSuccessful && items != null) {
                val pendingDates = bodyMetricDao.getAllPendingMetrics()
                    .map { it.measuredAt.take(10) }
                    .toSet()
                val toUpsert = items.filter { serverItem ->
                    !pendingDates.contains(serverItem.date.take(10))
                }
                bodyMetricDao.upsertAll(toUpsert.map { it.toEntity() })
            }
        } catch (_: IOException) {
            // offline — keep Room cache
        } catch (_: Exception) {
            // other errors — keep Room cache
        }
    }

    // ── Write operations (optimistic + sync queue) ────────────────────────────

    suspend fun addMetric(request: UpsertBodyMetricRequest): Result<BodyMetricDto> {
        val targetDate = request.recordedAt?.take(10) ?: LocalDate.now().toString()
        val existing = bodyMetricDao.getByDatePrefix(targetDate)
        if (existing != null) {
            if (existing.isPendingSync) {
                pendingSyncActionDao.deleteByActionType("ADD_BODY_METRIC:${existing.id}")
            }
            bodyMetricDao.deleteById(existing.id)
        }

        val tempId = "tmp_${UUID.randomUUID()}"
        bodyMetricDao.upsert(request.toEntity(id = tempId, isPendingSync = true))

        val payload = moshi.adapter(UpsertBodyMetricRequest::class.java).toJson(request)
        pendingSyncActionDao.insert(
            PendingSyncActionEntity(actionType = "ADD_BODY_METRIC:$tempId", payload = payload)
        )
        syncScheduler.requestSync()

        return try {
            val res = bodyMetricsApi.addMetric(request)
            val dto = res.body()?.data
            if (res.isSuccessful && dto != null) {
                bodyMetricDao.replaceEntity(tempId, dto.toEntity(isPendingSync = false))
                pendingSyncActionDao.deleteByActionType("ADD_BODY_METRIC:$tempId")
                Result.success(dto)
            } else {
                Result.success(request.toDto(tempId))
            }
        } catch (_: IOException) {
            Result.success(request.toDto(tempId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateBasic(metric: BasicBodyMetricRequest): Result<BodyMetricDto> {
        return try {
            val response = bodyMetricsApi.updateBasic(metric)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) {
                bodyMetricDao.upsert(body.toEntity())
                Result.success(body)
            } else Result.failure(Exception("Lỗi lưu số liệu cơ bản (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateAdvanced(metric: AdvancedBodyMetricRequest): Result<BodyMetricDto> {
        return try {
            val response = bodyMetricsApi.updateAdvanced(metric)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) {
                bodyMetricDao.upsert(body.toEntity())
                Result.success(body)
            } else Result.failure(Exception("Lỗi lưu số liệu nâng cao (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Photos (network-only) ─────────────────────────────────────────────────

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

    // ── Helpers ───────────────────────────────────────────────────────────────

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
