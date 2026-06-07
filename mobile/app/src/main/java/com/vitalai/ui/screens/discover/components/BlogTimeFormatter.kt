package com.vitalai.ui.screens.discover.components

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val blogDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

fun formatBlogTime(
    value: String,
    now: Instant = Instant.now(),
    zoneId: ZoneId = ZoneId.systemDefault()
): String {
    val instant = value.toBlogInstant() ?: return value.fallbackBlogDate()
    val duration = Duration.between(instant, now)

    if (duration.isNegative || duration.toMinutes() < 1) return "vừa xong"
    if (duration.toHours() < 1) return "${duration.toMinutes()} phút"
    if (duration.toDays() < 1) return "${duration.toHours()} giờ"
    if (duration.toDays() < 7) return "${duration.toDays()} ngày"

    return instant.atZone(zoneId).toLocalDate().format(blogDateFormatter)
}

private fun String.toBlogInstant(): Instant? {
    return runCatching { Instant.parse(this) }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(this).toInstant() }.getOrNull()
        // Backend/Postgres timestamps without an offset are UTC.
        ?: runCatching { LocalDateTime.parse(take(19)).toInstant(ZoneOffset.UTC) }.getOrNull()
        ?: runCatching { LocalDate.parse(take(10)).atStartOfDay(ZoneOffset.UTC).toInstant() }.getOrNull()
}

private fun String.fallbackBlogDate(): String {
    return runCatching {
        LocalDate.parse(take(10)).format(blogDateFormatter)
    }.getOrElse {
        take(10)
    }
}
