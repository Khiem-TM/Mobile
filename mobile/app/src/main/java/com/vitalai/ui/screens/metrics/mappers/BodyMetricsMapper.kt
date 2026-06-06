package com.vitalai.ui.screens.metrics.mappers

import com.vitalai.data.remote.model.BodyMetricDto
import com.vitalai.data.remote.model.ProgressPhotoDto
import com.vitalai.ui.screens.metrics.formatCompact
import com.vitalai.ui.screens.metrics.viewmodels.MetricEventType
import com.vitalai.ui.screens.metrics.viewmodels.MetricTimelineEvent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun BodyMetricDto.toTimelineEvent(
    photos: List<ProgressPhotoDto>,
    previousMetric: BodyMetricDto? = null
): MetricTimelineEvent {
    val parsedDate = metricDateOrNull()
    val displayDate = parsedDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("vi"))) ?: date
    val month = parsedDate?.format(DateTimeFormatter.ofPattern("'Tháng' M/yyyy", Locale("vi"))) ?: "Số liệu"

    val weightDelta = previousMetric?.weightKg?.let { prev -> weightKg - prev }
    val bodyFatDelta = bodyFatPct?.let { fat -> previousMetric?.bodyFatPct?.let { prev -> fat - prev } }

    val note = listOfNotNull(
        bmi?.let { "BMI ${it.formatCompact()}" },
        bmr?.let { "BMR ${"%.0f".format(it)} kcal" },
        tdee?.let { "TDEE ${"%.0f".format(it)} kcal" },
        notes
    ).joinToString(" · ").ifBlank { null }

    return MetricTimelineEvent(
        id = id,
        type = MetricEventType.MEASUREMENT,
        title = "Cập nhật chỉ số",
        value = weightKg.formatCompact(),
        note = note,
        photoUrl = photos.firstOrNull()?.photoUrl,
        photoUrls = photos.map { it.photoUrl },
        date = displayDate,
        monthGroup = month,
        rawMetric = this,
        weightDelta = weightDelta,
        bodyFatDelta = bodyFatDelta
    )
}

fun BodyMetricDto.metricDateOrNull(): LocalDate? =
    runCatching { LocalDate.parse(date.take(10)) }.getOrNull()
