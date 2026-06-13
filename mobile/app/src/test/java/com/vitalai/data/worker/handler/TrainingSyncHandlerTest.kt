package com.vitalai.data.worker.handler

import com.vitalai.data.local.room.entity.PendingSyncActionEntity
import com.vitalai.data.remote.TrainingApi
import com.vitalai.data.remote.UpdateMoodRequest
import com.vitalai.data.remote.UpdateNoteRequest
import com.vitalai.data.remote.UpdateSleepRequest
import com.vitalai.data.remote.UpdateStepsRequest
import com.vitalai.data.remote.UpdateWaterRequest
import com.vitalai.data.remote.model.ActivityLogDto
import com.vitalai.data.remote.model.AddExerciseRequest
import com.vitalai.data.remote.model.ApiResponse
import com.vitalai.data.remote.model.CreateWorkoutSessionDto
import com.vitalai.data.remote.model.ExerciseDto
import com.vitalai.data.remote.model.SessionExerciseDto
import com.vitalai.data.remote.model.UpdateWorkoutSessionRequest
import com.vitalai.data.remote.model.WorkoutSessionDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class TrainingSyncHandlerTest {
    private val api = FakeTrainingApi()
    private val handler = TrainingSyncHandler(api)

    @Test
    fun `handles pending sleep update`() = runTest {
        val ok = handler.handle(
            PendingSyncActionEntity(actionType = "UPDATE_SLEEP:2026-06-13", payload = "7.5")
        )

        assertTrue(ok)
        assertEquals(UpdateSleepRequest("2026-06-13", 7.5f), api.lastSleep)
    }

    @Test
    fun `handles pending mood update`() = runTest {
        val ok = handler.handle(
            PendingSyncActionEntity(actionType = "UPDATE_MOOD:2026-06-13", payload = "good")
        )

        assertTrue(ok)
        assertEquals(UpdateMoodRequest("2026-06-13", "good"), api.lastMood)
    }

    @Test
    fun `handles pending note update`() = runTest {
        val ok = handler.handle(
            PendingSyncActionEntity(actionType = "UPDATE_NOTE:2026-06-13", payload = "Leg day")
        )

        assertTrue(ok)
        assertEquals(UpdateNoteRequest("2026-06-13", "Leg day"), api.lastNote)
    }
}

private class FakeTrainingApi : TrainingApi {
    var lastSleep: UpdateSleepRequest? = null
    var lastMood: UpdateMoodRequest? = null
    var lastNote: UpdateNoteRequest? = null

    override suspend fun getSessionHistory(
        limit: Int?,
        fromDate: String?,
        toDate: String?
    ): Response<ApiResponse<List<WorkoutSessionDto>>> = error("Unexpected call")

    override suspend fun getSessionsByDate(date: String): Response<ApiResponse<List<WorkoutSessionDto>>> =
        error("Unexpected call")

    override suspend fun getExercises(
        exerciseType: String?,
        name: String?,
        muscleGroup: String?,
        difficultyLevel: String?,
        page: Int?,
        limit: Int?
    ): Response<ApiResponse<List<ExerciseDto>>> = error("Unexpected call")

    override suspend fun getExerciseById(id: String): Response<ApiResponse<ExerciseDto>> =
        error("Unexpected call")

    override suspend fun getFavoriteExercises(): Response<ApiResponse<List<ExerciseDto>>> =
        error("Unexpected call")

    override suspend fun addFavorite(id: String): Response<ApiResponse<Map<String, String>>> =
        error("Unexpected call")

    override suspend fun removeFavorite(id: String): Response<ApiResponse<Map<String, String>>> =
        error("Unexpected call")

    override suspend fun createSession(request: CreateWorkoutSessionDto): Response<ApiResponse<WorkoutSessionDto>> =
        error("Unexpected call")

    override suspend fun deleteSession(id: String): Response<Unit> =
        error("Unexpected call")

    override suspend fun updateSession(
        id: String,
        request: UpdateWorkoutSessionRequest
    ): Response<ApiResponse<WorkoutSessionDto>> = error("Unexpected call")

    override suspend fun addExercise(
        sessionId: String,
        request: AddExerciseRequest
    ): Response<ApiResponse<SessionExerciseDto>> = error("Unexpected call")

    override suspend fun removeExercise(sessionId: String, detailId: String): Response<Unit> =
        error("Unexpected call")

    override suspend fun getActivityLog(date: String): Response<ApiResponse<ActivityLogDto>> =
        error("Unexpected call")

    override suspend fun updateSteps(request: UpdateStepsRequest): Response<ApiResponse<ActivityLogDto>> =
        Response.success(ApiResponse(true, 200, ActivityLogDto(logDate = request.logDate, steps = request.steps)))

    override suspend fun updateWater(request: UpdateWaterRequest): Response<ApiResponse<ActivityLogDto>> =
        Response.success(ApiResponse(true, 200, ActivityLogDto(logDate = request.logDate, waterMl = request.waterMl)))

    override suspend fun updateSleep(request: UpdateSleepRequest): Response<ApiResponse<ActivityLogDto>> {
        lastSleep = request
        return Response.success(ApiResponse(true, 200, ActivityLogDto(logDate = request.logDate, sleepHours = request.sleepHours)))
    }

    override suspend fun updateMood(request: UpdateMoodRequest): Response<ApiResponse<ActivityLogDto>> {
        lastMood = request
        return Response.success(ApiResponse(true, 200, ActivityLogDto(logDate = request.logDate, mood = request.mood)))
    }

    override suspend fun updateNote(request: UpdateNoteRequest): Response<ApiResponse<ActivityLogDto>> {
        lastNote = request
        return Response.success(ApiResponse(true, 200, ActivityLogDto(logDate = request.logDate, note = request.note)))
    }

    override suspend fun getActivityLogRange(
        fromDate: String,
        toDate: String
    ): Response<ApiResponse<List<ActivityLogDto>>> = error("Unexpected call")
}
