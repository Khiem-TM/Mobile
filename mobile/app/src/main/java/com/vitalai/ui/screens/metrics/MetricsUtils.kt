package com.vitalai.ui.screens.metrics

import androidx.compose.ui.graphics.Color
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.util.Locale

fun bmiColor(bmi: Float): Color = when {
    bmi < 18.5f -> Color(0xFF60A5FA)
    bmi < 25f -> Color(0xFF34D399)
    bmi < 30f -> Color(0xFFFBBF24)
    else -> Color(0xFFF87171)
}

fun bmiLabel(bmi: Float): String = when {
    bmi < 18.5f -> "Thiếu cân"
    bmi < 25f -> "Bình thường"
    bmi < 30f -> "Thừa cân"
    else -> "Béo phì"
}

fun formatGender(gender: String?): String = when (gender?.lowercase(Locale.US)) {
    "male", "nam" -> "Nam"
    "female", "nu", "nữ" -> "Nữ"
    "other", "khac", "khác" -> "Khác"
    null, "" -> "--"
    else -> gender
}

fun normalizeGender(gender: String?): String = when (gender?.lowercase(Locale.US)) {
    "male", "nam" -> "male"
    "female", "nu", "nữ" -> "female"
    "other", "khac", "khác" -> "other"
    else -> "male"
}

fun genderDisplay(gender: String): String = when (gender) {
    "male" -> "Nam"
    "female" -> "Nữ"
    "other" -> "Khác"
    else -> formatGender(gender)
}

fun calculateAge(birthDate: String?): Int? {
    val date = birthDate?.takeIf { it.isNotBlank() } ?: return null
    return runCatching {
        Period.between(LocalDate.parse(date.take(10)), LocalDate.now()).years
    }.getOrNull()?.takeIf { it >= 0 }
}

fun ageToBirthDate(age: Int, currentBirthDate: String?): String {
    val today = LocalDate.now()
    val currentMonthDay = runCatching {
        LocalDate.parse(currentBirthDate?.take(10)).let { it.monthValue to it.dayOfMonth }
    }.getOrNull()
    val month = currentMonthDay?.first ?: today.monthValue
    val day = currentMonthDay?.second ?: today.dayOfMonth
    val candidate = runCatching { LocalDate.of(today.year - age, month, day) }
        .getOrElse { LocalDate.of(today.year - age, month, 1) }
    return candidate.toString()
}

fun calculateBmi(weightKg: Float?, heightCm: Float?): Float? {
    if (weightKg == null || heightCm == null || heightCm <= 0f) return null
    val heightM = heightCm / 100f
    return weightKg / (heightM * heightM)
}

fun calculateWeightGoalProgress(
    initialWeightKg: Float?,
    currentWeightKg: Float?,
    targetWeightKg: Float?
): Float {
    if (currentWeightKg == null || targetWeightKg == null || targetWeightKg <= 0f) return 0f
    if (initialWeightKg != null && initialWeightKg != targetWeightKg) {
        val totalChange = kotlin.math.abs(targetWeightKg - initialWeightKg)
        val currentChange = kotlin.math.abs(currentWeightKg - initialWeightKg)
        return (currentChange / totalChange).coerceIn(0.04f, 1f)
    }
    return (currentWeightKg / targetWeightKg).coerceIn(0.04f, 1f)
}

fun formatPhotoType(type: String): String = when (type) {
    "front" -> "Mặt trước"
    "back" -> "Mặt sau"
    "side" -> "Mặt bên"
    else -> type
}

fun formatDateDisplay(dateStr: String): String {
    val date = runCatching {
        Instant.parse(dateStr).atZone(ZoneId.systemDefault()).toLocalDate()
    }.recoverCatching {
        LocalDate.parse(dateStr.take(10))
    }.getOrNull()
    return if (date != null) {
        "%02d/%02d/%04d".format(date.dayOfMonth, date.monthValue, date.year)
    } else {
        dateStr
    }
}
