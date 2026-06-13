package com.vitalai.data.repository

import android.content.Context
import com.squareup.moshi.Moshi
import com.vitalai.core.sync.SyncScheduler
import com.vitalai.data.local.room.dao.ActivityLogDao
import com.vitalai.data.local.room.dao.ExerciseDao
import com.vitalai.data.local.room.dao.PendingSyncActionDao
import com.vitalai.data.local.room.dao.WorkoutSessionDao
import com.vitalai.data.local.room.entity.PendingSyncActionEntity
import com.vitalai.data.mapper.toDto
import com.vitalai.data.mapper.toEntity
import com.vitalai.data.remote.TrainingApi
import com.vitalai.data.remote.UpdateStepsRequest
import com.vitalai.data.remote.UpdateWaterRequest
import com.vitalai.data.remote.model.ActivityLogDto
import com.vitalai.data.remote.model.AddExerciseRequest
import com.vitalai.data.remote.model.CreateWorkoutSessionDto
import com.vitalai.data.remote.model.ExerciseDto
import com.vitalai.data.remote.model.SessionExerciseDto
import com.vitalai.data.remote.model.UpdateWorkoutSessionRequest
import com.vitalai.data.remote.model.WorkoutDetailDto
import com.vitalai.data.remote.model.WorkoutSessionDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val EXERCISE_CACHE_TTL_MS = 24 * 60 * 60 * 1000L // 24 hours
private const val EXERCISE_SYNC_PAGE_SIZE = 100
private const val KEY_EXERCISE_SYNCED_AT = "exercise_synced_at"

@Singleton
class TrainingRepository @Inject constructor(
    private val trainingApi: TrainingApi,
    private val pendingSyncActionDao: PendingSyncActionDao,
    private val exerciseDao: ExerciseDao,
    private val workoutSessionDao: WorkoutSessionDao,
    private val activityLogDao: ActivityLogDao,
    private val moshi: Moshi,
    private val syncScheduler: SyncScheduler,
    @ApplicationContext private val context: Context
) {
    private val sharedPrefs = context.getSharedPreferences("training_prefs", Context.MODE_PRIVATE)

    // ── Ghi optimistic: chỉ đẩy vào hàng đợi (last-write-wins theo ngày) rồi để
    //    SyncWorker gửi nền. KHÔNG await network, KHÔNG refetch -> UI mượt. ──────
    suspend fun enqueueWater(waterMl: Int, logDate: String = LocalDate.now().toString()) {
        val type = "UPDATE_WATER:$logDate"
        pendingSyncActionDao.deleteByActionType(type)
        pendingSyncActionDao.insert(PendingSyncActionEntity(actionType = type, payload = waterMl.toString()))
        val current = activityLogDao.getByDate(logDate)?.toDto() ?: ActivityLogDto(logDate = logDate)
        activityLogDao.upsert(current.copy(waterMl = waterMl).toEntity())
        syncScheduler.requestSync()
    }

    suspend fun enqueueSteps(steps: Int, logDate: String = LocalDate.now().toString()) {
        val type = "UPDATE_STEPS:$logDate"
        pendingSyncActionDao.deleteByActionType(type)
        pendingSyncActionDao.insert(PendingSyncActionEntity(actionType = type, payload = steps.toString()))
        val current = activityLogDao.getByDate(logDate)?.toDto() ?: ActivityLogDto(logDate = logDate)
        activityLogDao.upsert(current.copy(steps = steps).toEntity())
        syncScheduler.requestSync()
    }

    suspend fun enqueueSleep(sleepHours: Float, logDate: String = LocalDate.now().toString()) {
        val type = "UPDATE_SLEEP:$logDate"
        pendingSyncActionDao.deleteByActionType(type)
        pendingSyncActionDao.insert(PendingSyncActionEntity(actionType = type, payload = sleepHours.toString()))
        val current = activityLogDao.getByDate(logDate)?.toDto() ?: ActivityLogDto(logDate = logDate)
        activityLogDao.upsert(current.copy(sleepHours = sleepHours).toEntity())
        syncScheduler.requestSync()
    }

    suspend fun enqueueMood(mood: String, logDate: String = LocalDate.now().toString()) {
        val type = "UPDATE_MOOD:$logDate"
        pendingSyncActionDao.deleteByActionType(type)
        pendingSyncActionDao.insert(PendingSyncActionEntity(actionType = type, payload = mood))
        val current = activityLogDao.getByDate(logDate)?.toDto() ?: ActivityLogDto(logDate = logDate)
        activityLogDao.upsert(current.copy(mood = mood).toEntity())
        syncScheduler.requestSync()
    }

    suspend fun enqueueNote(note: String, logDate: String = LocalDate.now().toString()) {
        val type = "UPDATE_NOTE:$logDate"
        pendingSyncActionDao.deleteByActionType(type)
        pendingSyncActionDao.insert(PendingSyncActionEntity(actionType = type, payload = note))
        val current = activityLogDao.getByDate(logDate)?.toDto() ?: ActivityLogDto(logDate = logDate)
        activityLogDao.upsert(current.copy(note = note).toEntity())
        syncScheduler.requestSync()
    }

    /** Giá trị nước/bước người dùng vừa đổi nhưng CHƯA đồng bộ (giữ qua restart). */
    suspend fun pendingWater(date: String): Int? =
        pendingSyncActionDao.getByActionType("UPDATE_WATER:$date")?.payload?.toIntOrNull()

    suspend fun pendingSteps(date: String): Int? =
        pendingSyncActionDao.getByActionType("UPDATE_STEPS:$date")?.payload?.toIntOrNull()

    private suspend fun pendingSleep(date: String): Float? =
        pendingSyncActionDao.getByActionType("UPDATE_SLEEP:$date")?.payload?.toFloatOrNull()

    private suspend fun pendingMood(date: String): String? =
        pendingSyncActionDao.getByActionType("UPDATE_MOOD:$date")?.payload

    private suspend fun pendingNote(date: String): String? =
        pendingSyncActionDao.getByActionType("UPDATE_NOTE:$date")?.payload

    /** Ghi đè field nóng bằng giá trị optimistic còn chờ đồng bộ (chống "snap back"). */
    private suspend fun applyPendingOverride(dto: ActivityLogDto, date: String): ActivityLogDto =
        dto.copy(
            waterMl = pendingWater(date) ?: dto.waterMl,
            steps = pendingSteps(date) ?: dto.steps,
            sleepHours = pendingSleep(date) ?: dto.sleepHours,
            mood = pendingMood(date) ?: dto.mood,
            note = pendingNote(date) ?: dto.note
        )

    private suspend fun enrichActivityLog(dto: ActivityLogDto, date: String): ActivityLogDto {
        val sessions = workoutSessionDao.getByDate(date).map { it.toDto(moshi) }
        val workoutCalories = sessions.sumOf { it.totalCaloriesBurned.toDouble() }.toFloat()
        val workoutMinutes = sessions.sumOf { it.totalDurationMinutes }
        val manualCalories = sharedPrefs.getFloat("manual_calories_$date", 0f)
        val manualMinutes = sharedPrefs.getInt("manual_minutes_$date", 0)
        return applyPendingOverride(
            dto.copy(
                caloriesBurned = workoutCalories + manualCalories,
                activeMinutes = workoutMinutes + manualMinutes
            ),
            date
        )
    }

    fun observeSessionsBetween(from: String, to: String): Flow<List<WorkoutSessionDto>> =
        workoutSessionDao.observeBetween(from, to).map { list -> list.map { it.toDto(moshi) } }

    fun observeRecentSessions(limit: Int): Flow<List<WorkoutSessionDto>> =
        workoutSessionDao.observeRecent(limit).map { list -> list.map { it.toDto(moshi) } }

    fun observeSessionsByDate(date: String): Flow<List<WorkoutSessionDto>> =
        workoutSessionDao.observeByDate(date).map { list -> list.map { it.toDto(moshi) } }

    fun observeActivityLog(date: String): Flow<ActivityLogDto?> =
        activityLogDao.observeByDate(date).map { it?.toDto() }

    fun observeActivityLogsBetween(from: String, to: String): Flow<List<ActivityLogDto>> =
        activityLogDao.observeBetween(from, to).map { list -> list.map { it.toDto() } }

    suspend fun refreshSessions(fromDate: String, toDate: String) {
        runCatching {
            trainingApi.getSessionHistory(limit = null, fromDate = fromDate, toDate = toDate)
        }.onSuccess { response ->
            val body = response.body()?.data ?: return@onSuccess
            cacheServerSessions(body)
        }
    }

    suspend fun refreshRecentSessions(limit: Int = 60) {
        runCatching { trainingApi.getSessionHistory(limit = limit) }
            .onSuccess { response ->
                val body = response.body()?.data ?: return@onSuccess
                cacheServerSessions(body)
            }
    }

    /** Ghi cache session từ server NHƯNG bỏ qua ngày đang có bản ghi pending
     *  (chống "snap back" đè lên bài tập chưa đồng bộ). */
    private suspend fun cacheServerSessions(body: List<WorkoutSessionDto>) {
        val pendingDates = workoutSessionDao.getPendingDates().toSet()
        val toUpsert = body.filter { it.sessionDate !in pendingDates }
        if (toUpsert.isNotEmpty()) workoutSessionDao.upsertAll(toUpsert.map { it.toEntity(moshi) })
        cacheEmbeddedExercises(body)
    }

    /** Lưu bài tập nhúng trong session vào cache để xem chi tiết offline.
     *  Chỉ thêm bài CÒN THIẾU — không đè bản thư viện (giữ cờ favorite & dữ liệu đầy đủ hơn). */
    private suspend fun cacheEmbeddedExercises(body: List<WorkoutSessionDto>) {
        val embedded = body.flatMap { it.details }.mapNotNull { it.exercise }.distinctBy { it.id }
        if (embedded.isEmpty()) return
        val existingIds = exerciseDao.getByIds(embedded.map { it.id }).map { it.id }.toSet()
        val toAdd = embedded.filter { it.id !in existingIds }
        if (toAdd.isNotEmpty()) exerciseDao.upsertAll(toAdd.map { it.toEntity() })
    }

    suspend fun refreshActivityLog(date: String) {
        getActivityLog(date)
    }

    suspend fun refreshActivityLogRange(from: String, to: String) {
        getActivityLogRange(from, to)
    }

    suspend fun getSessions(
        date: String? = null,
        limit: Int? = null,
        fromDate: String? = null,
        toDate: String? = null
    ): Result<List<WorkoutSessionDto>> {
        return try {
            val response = if (date != null) {
                trainingApi.getSessionsByDate(date)
            } else {
                trainingApi.getSessionHistory(limit = limit, fromDate = fromDate, toDate = toDate)
            }
            val body = response.body()?.data
            if (response.isSuccessful && body != null) {
                cacheServerSessions(body)
                Result.success(body)
            }
            else Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Exercise cache ────────────────────────────────────────────────────────

    fun observeExercises(): Flow<List<ExerciseDto>> =
        exerciseDao.observeAll().map { list -> list.map { it.toDto() } }

    fun observeFavoriteExercises(): Flow<List<ExerciseDto>> =
        exerciseDao.observeFavorites().map { list -> list.map { it.toDto() } }

    fun observeExercise(id: String): Flow<ExerciseDto?> =
        exerciseDao.observeById(id).map { it?.toDto() }

    suspend fun refreshExercises(force: Boolean = false) {
        val syncedAt = sharedPrefs.getLong(KEY_EXERCISE_SYNCED_AT, 0L)
        val isCacheStale = syncedAt == 0L || System.currentTimeMillis() - syncedAt > EXERCISE_CACHE_TTL_MS
        if (!force && !isCacheStale) return

        runCatching {
            val exercises = mutableListOf<ExerciseDto>()
            var page = 1
            do {
                val response = trainingApi.getExercises(
                    page = page,
                    limit = EXERCISE_SYNC_PAGE_SIZE
                )
                if (!response.isSuccessful) {
                    error("Lỗi đồng bộ bài tập (${response.code()})")
                }
                val batch = response.body()?.data
                    ?: error("Phản hồi đồng bộ bài tập không hợp lệ")
                exercises += batch
                page += 1
            } while (batch.size == EXERCISE_SYNC_PAGE_SIZE)
            exercises.distinctBy { it.id }
        }.onSuccess { exercises ->
            exerciseDao.replaceAll(exercises.map { it.toEntity() })
            sharedPrefs.edit().putLong(KEY_EXERCISE_SYNCED_AT, System.currentTimeMillis()).apply()
        }
    }

    suspend fun refreshFavoriteExercises() {
        runCatching { trainingApi.getFavoriteExercises() }
            .onSuccess { res ->
                val favorites = res.body()?.data ?: return@onSuccess
                exerciseDao.clearAllFavoriteFlags()
                exerciseDao.upsertAll(favorites.map { it.toEntity(isFavorite = true) })
            }
    }

    suspend fun getExercises(
        type: String? = null,
        name: String? = null,
        muscleGroup: String? = null,
        difficultyLevel: String? = null
    ): Result<List<ExerciseDto>> {
        return try {
            val response = trainingApi.getExercises(
                exerciseType = type,
                name = name,
                muscleGroup = muscleGroup,
                difficultyLevel = difficultyLevel,
                limit = EXERCISE_SYNC_PAGE_SIZE
            )
            val body = response.body()?.data
            if (response.isSuccessful && body != null) {
                exerciseDao.upsertAll(body.map { it.toEntity() })
                Result.success(body)
            } else Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getExerciseById(id: String): Result<ExerciseDto> {
        return try {
            val response = trainingApi.getExerciseById(id)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) {
                exerciseDao.upsert(body.toEntity())
                Result.success(body)
            }
            // Không tìm thấy trên server → thử cache (bài đã từng xem/đã tập).
            else exerciseDao.getById(id)?.toDto()?.let { Result.success(it) }
                ?: Result.failure(Exception("Không tìm thấy bài tập (${response.code()})"))
        } catch (e: Exception) {
            // Offline → fallback cache.
            exerciseDao.getById(id)?.toDto()?.let { Result.success(it) } ?: Result.failure(e)
        }
    }

    suspend fun getFavorites(): Result<List<ExerciseDto>> {
        return try {
            val response = trainingApi.getFavoriteExercises()
            val body = response.body()?.data
            if (response.isSuccessful && body != null) {
                exerciseDao.upsertAll(body.map { it.toEntity(isFavorite = true) })
                Result.success(body)
            }
            else Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addFavorite(id: String): Result<Unit> {
        return try {
            val response = trainingApi.addFavorite(id)
            if (response.isSuccessful) {
                exerciseDao.setFavorite(id, true)
                Result.success(Unit)
            } else Result.failure(Exception("Lỗi thêm yêu thích (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFavorite(id: String): Result<Unit> {
        return try {
            val response = trainingApi.removeFavorite(id)
            if (response.isSuccessful) {
                exerciseDao.setFavorite(id, false)
                Result.success(Unit)
            } else Result.failure(Exception("Lỗi bỏ yêu thích (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createSession(request: CreateWorkoutSessionDto): Result<WorkoutSessionDto> {
        return try {
            val response = trainingApi.createSession(request)
            val body = response.body()?.data
            when {
                response.isSuccessful && body != null -> {
                    workoutSessionDao.upsert(body.toEntity(moshi))
                    Result.success(body)
                }
                // Backend enforces one training session per day (HTTP 409). Instead of
                // failing, append the new exercises to the day's existing session.
                response.code() == 409 -> mergeIntoExistingSession(request)
                else -> Result.failure(Exception("Lỗi tạo phiên tập (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun enqueueSession(request: CreateWorkoutSessionDto): Result<WorkoutSessionDto> {
        val date = request.sessionDate
        // Bài tập user vừa nhập, kèm tên + ước lượng calo/phút từ cache bài tập.
        val newDetails = buildOptimisticDetails(request.details)
        val estNewMinutes = newDetails.sumOf { it.durationMinutes }
        val estNewCalories = newDetails.sumOf { it.caloriesBurned.toDouble() }.toFloat()

        // Bất biến 1 card/ngày: gộp vào buổi tập đã có trong ngày, nếu chưa có thì tạo tạm.
        val existing = workoutSessionDao.getByDate(date).firstOrNull()?.toDto(moshi)
        val optimistic = if (existing != null) {
            existing.copy(
                sessionName = existing.sessionName ?: request.sessionName,
                totalDurationMinutes = existing.totalDurationMinutes + estNewMinutes,
                totalCaloriesBurned = existing.totalCaloriesBurned + estNewCalories,
                details = existing.details + newDetails
            )
        } else {
            WorkoutSessionDto(
                id = "tmp_${UUID.randomUUID()}",
                sessionName = request.sessionName,
                sessionDate = date,
                totalDurationMinutes = estNewMinutes,
                totalCaloriesBurned = estNewCalories,
                details = newDetails
            )
        }
        workoutSessionDao.upsert(optimistic.toEntity(moshi, isPendingSync = true))

        // Action key dùng UUID riêng (không trùng), payload = đúng phần details vừa thêm.
        val actionType = "CREATE_SESSION:${UUID.randomUUID()}"
        val payload = moshi.adapter(CreateWorkoutSessionDto::class.java).toJson(request)
        pendingSyncActionDao.insert(PendingSyncActionEntity(actionType = actionType, payload = payload))
        syncScheduler.requestSync()

        return Result.success(optimistic)
    }

    /** Đẩy buổi tập lên server (create hoặc merge-409) rồi reconcile cache theo NGÀY:
     *  xoá mọi row khác ngày đó (temp/rỗng/trùng), giữ đúng 1 card = bản server. */
    suspend fun pushPendingSession(request: CreateWorkoutSessionDto): Boolean {
        val server = createSession(request).getOrNull() ?: return false
        workoutSessionDao.deleteByDateExcept(server.sessionDate, server.id)
        workoutSessionDao.upsert(server.toEntity(moshi, isPendingSync = false))
        return true
    }

    /** WorkoutDetailDto -> SessionExerciseDto cho hiển thị optimistic offline:
     *  resolve tên bài + ước lượng calo từ ExerciseDto đã cache. */
    private suspend fun buildOptimisticDetails(details: List<WorkoutDetailDto>): List<SessionExerciseDto> {
        val ids = details.map { it.exerciseId }.distinct()
        if (ids.isEmpty()) return emptyList()
        val exMap = exerciseDao.getByIds(ids).associate { it.id to it.toDto() }
        return details.mapIndexed { i, d ->
            val ex = exMap[d.exerciseId]
            val minutes = d.durationMinutes ?: ex?.defaultDurationMinutes ?: 0
            val calPerMin = ex?.caloriesPerMin?.takeIf { it > 0f }
                ?: ex?.estimatedCaloriesPerMinute?.takeIf { it > 0f }
                ?: 0f
            SessionExerciseDto(
                id = "tmpdet_$i",
                exerciseId = d.exerciseId,
                exercise = ex,
                sets = d.sets,
                repsPerSet = d.repsPerSet,
                weightKg = d.weightKg,
                durationMinutes = minutes,
                caloriesBurned = calPerMin * minutes,
                exerciseType = d.exerciseType,
                intensityLevel = d.intensityLevel,
                distanceKm = d.distanceKm,
                avgSpeedKmh = d.avgSpeedKmh,
                restTimeSeconds = d.restTimeSeconds
            )
        }
    }

    private suspend fun mergeIntoExistingSession(request: CreateWorkoutSessionDto): Result<WorkoutSessionDto> {
        val existing = trainingApi.getSessionsByDate(request.sessionDate).body()?.data?.firstOrNull()
            ?: return Result.failure(Exception("Không tìm thấy buổi tập hôm nay để thêm bài tập."))
        val startIndex = existing.details.size
        request.details.forEachIndexed { index, d ->
            val addResp = trainingApi.addExercise(
                existing.id,
                AddExerciseRequest(
                    exerciseId = d.exerciseId,
                    exerciseType = d.exerciseType,
                    sets = d.sets,
                    repsPerSet = d.repsPerSet,
                    weightKg = d.weightKg,
                    durationMinutes = d.durationMinutes,
                    orderIndex = startIndex + index,
                    intensityLevel = d.intensityLevel,
                    distanceKm = d.distanceKm,
                    avgSpeedKmh = d.avgSpeedKmh,
                    restTimeSeconds = d.restTimeSeconds
                )
            )
            if (!addResp.isSuccessful) {
                return Result.failure(Exception("Lỗi thêm bài tập vào buổi tập (${addResp.code()})"))
            }
        }
        val refreshed = trainingApi.getSessionsByDate(request.sessionDate).body()?.data?.firstOrNull()
        val merged = refreshed ?: existing
        workoutSessionDao.upsert(merged.toEntity(moshi))
        return Result.success(merged)
    }

    suspend fun deleteSession(id: String): Result<Unit> {
        return try {
            val response = trainingApi.deleteSession(id)
            if (response.isSuccessful) {
                workoutSessionDao.deleteById(id)
                Result.success(Unit)
            }
            else Result.failure(Exception("Lỗi xóa phiên tập (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSession(id: String, request: UpdateWorkoutSessionRequest): Result<WorkoutSessionDto> {
        return try {
            val response = trainingApi.updateSession(id, request)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) {
                workoutSessionDao.upsert(body.toEntity(moshi))
                Result.success(body)
            }
            else Result.failure(Exception("Lỗi cập nhật phiên tập (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addExercise(sessionId: String, request: AddExerciseRequest): Result<SessionExerciseDto> {
        return try {
            val response = trainingApi.addExercise(sessionId, request)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) Result.success(body)
            else Result.failure(Exception("Lỗi thêm bài tập (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeExercise(sessionId: String, detailId: String): Result<Unit> {
        return try {
            val response = trainingApi.removeExercise(sessionId, detailId)
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Lỗi xóa bài tập khỏi phiên (${response.code()})"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActivityLog(date: String): Result<ActivityLogDto> {
        return try {
            val response = trainingApi.getActivityLog(date)
            val body = response.body()?.data
            if (response.isSuccessful && body != null) {
                val augmented = enrichActivityLog(body, date)
                activityLogDao.upsert(augmented.toEntity())
                Result.success(augmented)
            } else {
                Result.failure(Exception("Lỗi tải hoạt động"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSteps(steps: Int, logDate: String = LocalDate.now().toString()): Result<ActivityLogDto> {
        return try {
            val response = trainingApi.updateSteps(UpdateStepsRequest(logDate, steps))
            val body = response.body()?.data
            if (response.isSuccessful && body != null) {
                val sessionsResponse = trainingApi.getSessionsByDate(logDate)
                val sessions = sessionsResponse.body()?.data ?: emptyList()
                val workoutCalories = sessions.sumOf { it.totalCaloriesBurned.toDouble() }.toFloat()
                val workoutMinutes = sessions.sumOf { it.totalDurationMinutes }

                val manualCalories = sharedPrefs.getFloat("manual_calories_$logDate", 0f)
                val manualMinutes = sharedPrefs.getInt("manual_minutes_$logDate", 0)

                val updated = body.copy(
                    caloriesBurned = workoutCalories + manualCalories,
                    activeMinutes = workoutMinutes + manualMinutes
                )
                activityLogDao.upsert(updated.toEntity())
                Result.success(updated)
            } else {
                Result.failure(Exception("Lỗi cập nhật bước chân"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateWater(waterMl: Int, logDate: String = LocalDate.now().toString()): Result<ActivityLogDto> {
        return try {
            val response = trainingApi.updateWater(UpdateWaterRequest(logDate, waterMl))
            val body = response.body()?.data
            if (response.isSuccessful && body != null) {
                val sessionsResponse = trainingApi.getSessionsByDate(logDate)
                val sessions = sessionsResponse.body()?.data ?: emptyList()
                val workoutCalories = sessions.sumOf { it.totalCaloriesBurned.toDouble() }.toFloat()
                val workoutMinutes = sessions.sumOf { it.totalDurationMinutes }

                val manualCalories = sharedPrefs.getFloat("manual_calories_$logDate", 0f)
                val manualMinutes = sharedPrefs.getInt("manual_minutes_$logDate", 0)

                val updated = body.copy(
                    caloriesBurned = workoutCalories + manualCalories,
                    activeMinutes = workoutMinutes + manualMinutes
                )
                activityLogDao.upsert(updated.toEntity())
                Result.success(updated)
            } else {
                Result.failure(Exception("Lỗi cập nhật nước uống"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActivityLogRange(fromDate: String, toDate: String): Result<List<ActivityLogDto>> {
        return try {
            val response = trainingApi.getActivityLogRange(fromDate, toDate)
            val body = response.body()?.data ?: emptyList()

            val start = java.time.LocalDate.parse(fromDate)
            val end = java.time.LocalDate.parse(toDate)
            val allDates = generateSequence(start) { d -> d.plusDays(1).takeIf { !it.isAfter(end) } }.map { it.toString() }.toList()

            val logsByDate = body.associateBy { it.logDate }.toMutableMap()
            allDates.forEach { date ->
                val existing = logsByDate[date] ?: ActivityLogDto(logDate = date)
                val pendingW = pendingWater(date)
                val pendingS = pendingSteps(date)
                val pendingSleep = pendingSleep(date)
                val pendingMood = pendingMood(date)
                val pendingNote = pendingNote(date)
                if (
                    pendingW != null ||
                    pendingS != null ||
                    pendingSleep != null ||
                    pendingMood != null ||
                    pendingNote != null ||
                    logsByDate.containsKey(date)
                ) {
                    logsByDate[date] = existing.copy(
                        waterMl = pendingW ?: existing.waterMl,
                        steps = pendingS ?: existing.steps,
                        sleepHours = pendingSleep ?: existing.sleepHours,
                        mood = pendingMood ?: existing.mood,
                        note = pendingNote ?: existing.note
                    )
                }
            }
            val logs = logsByDate.values.map { enrichActivityLog(it, it.logDate) }
            activityLogDao.upsertAll(logs.map { it.toEntity() })
            Result.success(logs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSleep(sleepHours: Float, logDate: String = LocalDate.now().toString()): Result<ActivityLogDto> {
        return try {
            enqueueSleep(sleepHours, logDate)
            Result.success(activityLogDao.getByDate(logDate)?.toDto() ?: ActivityLogDto(logDate = logDate, sleepHours = sleepHours))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMood(mood: String, logDate: String = LocalDate.now().toString()): Result<ActivityLogDto> {
        return try {
            enqueueMood(mood, logDate)
            Result.success(activityLogDao.getByDate(logDate)?.toDto() ?: ActivityLogDto(logDate = logDate, mood = mood))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateNote(note: String, logDate: String = LocalDate.now().toString()): Result<ActivityLogDto> {
        return try {
            enqueueNote(note, logDate)
            Result.success(activityLogDao.getByDate(logDate)?.toDto() ?: ActivityLogDto(logDate = logDate, note = note))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    suspend fun updateCaloriesBurned(
        caloriesBurned: Float,
        activeMinutes: Int,
        logDate: String = LocalDate.now().toString(),
        _exerciseNotes: String? = null
    ): Result<ActivityLogDto> {
        return try {
            // Save manual inputs to local SharedPreferences
            sharedPrefs.edit()
                .putFloat("manual_calories_$logDate", caloriesBurned)
                .putInt("manual_minutes_$logDate", activeMinutes)
                .apply()

            val current = activityLogDao.getByDate(logDate)?.toDto() ?: ActivityLogDto(logDate = logDate)
            val updated = enrichActivityLog(current, logDate)
            activityLogDao.upsert(updated.toEntity())
            Result.success(updated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
