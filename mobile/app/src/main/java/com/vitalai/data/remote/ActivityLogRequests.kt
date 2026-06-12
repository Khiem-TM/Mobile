package com.vitalai.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UpdateStepsRequest(val logDate: String, val steps: Int)

@JsonClass(generateAdapter = true)
data class UpdateWaterRequest(val logDate: String, val waterMl: Int)

@JsonClass(generateAdapter = true)
data class UpdateSleepRequest(val logDate: String, val sleepHours: Float)

@JsonClass(generateAdapter = true)
data class UpdateMoodRequest(val logDate: String, val mood: String)

@JsonClass(generateAdapter = true)
data class UpdateNoteRequest(val logDate: String, val note: String)
