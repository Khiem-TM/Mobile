package com.vitalai.data.mapper

import com.vitalai.data.remote.model.BodyMetricDto
import com.vitalai.data.remote.model.ProgressPhotoDto
import com.vitalai.ui.screens.metrics.viewmodels.MetricEventType
import com.vitalai.ui.screens.metrics.viewmodels.MetricTimelineEvent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun BodyMetricDto.toTimelineEvent(photos: List<ProgressPhotoDto>): MetricTimelineEvent {
    val parsedDate = metricDateOrNull()
    val displayDate = parsedDate?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("vi"))) ?: date
    val month = parsedDate?.format(DateTimeFormatter.ofPattern("'Tháng' M/yyyy", Locale("vi"))) ?: "Số liệu"

    val level = when {
        bodyFatPct != null || waistCm != null || hipCm != null || armCm != null ->
            MetricEventType.MEASUREMENT
        else -> MetricEventType.WEIGHT
    }

    val details = when (level) {
        MetricEventType.MEASUREMENT -> listOfNotNull(
            waistCm?.let { "Eo %.1f cm".format(it) },
            hipCm?.let { "Hông %.1f cm".format(it) },
            chestCm?.let { "Ngực %.1f cm".format(it) },
            armCm?.let { "Bắp tay %.1f cm".format(it) },
            neckCm?.let { "Cổ %.1f cm".format(it) },
            notes
        )
        else -> listOfNotNull(
            bmi?.let { "BMI %.1f".format(it) },
            bodyFatPct?.let { "Mỡ %.1f%%".format(it) },
            waistCm?.let { "Eo %.1f cm".format(it) },
            hipCm?.let { "Hông %.1f cm".format(it) },
            notes
        )
    }.joinToString(" · ").ifBlank { null }

    val title = when (level) {
        MetricEventType.MEASUREMENT -> "Cập nhật số đo cơ thể"
        else -> "Cập nhật cân nặng"
    }

    return MetricTimelineEvent(
        id = id,
        type = level,
        title = title,
        value = when (level) {
            MetricEventType.MEASUREMENT -> bodyFatPct?.let { "%.1f%% mỡ".format(it) } ?: "--% mỡ"
            else -> "%.1f kg".format(weightKg)
        },
        note = details,
        photoUrl = photos.firstOrNull()?.photoUrl,
        photoUrls = photos.map { it.photoUrl },
        date = displayDate,
        monthGroup = month,
        rawMetric = this
    )
}

fun BodyMetricDto.metricDateOrNull(): LocalDate? =
    runCatching { LocalDate.parse(date.take(10)) }.getOrNull()
