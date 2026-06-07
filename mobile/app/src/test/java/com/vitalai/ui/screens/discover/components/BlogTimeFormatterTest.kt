package com.vitalai.ui.screens.discover.components

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class BlogTimeFormatterTest {
    private val now = Instant.parse("2026-06-07T12:00:00Z")
    private val utc = ZoneId.of("UTC")

    @Test
    fun `formats relative time boundaries`() {
        assertEquals("vừa xong", formatBlogTime("2026-06-07T11:59:30Z", now, utc))
        assertEquals("1 phút", formatBlogTime("2026-06-07T11:59:00Z", now, utc))
        assertEquals("59 phút", formatBlogTime("2026-06-07T11:01:00Z", now, utc))
        assertEquals("1 giờ", formatBlogTime("2026-06-07T11:00:00Z", now, utc))
        assertEquals("23 giờ", formatBlogTime("2026-06-06T13:00:00Z", now, utc))
        assertEquals("1 ngày", formatBlogTime("2026-06-06T12:00:00Z", now, utc))
        assertEquals("6 ngày", formatBlogTime("2026-06-01T12:00:00Z", now, utc))
        assertEquals("31/05/2026", formatBlogTime("2026-05-31T12:00:00Z", now, utc))
    }

    @Test
    fun `supports offset and local timestamps`() {
        assertEquals("1 giờ", formatBlogTime("2026-06-07T13:00:00+02:00", now, utc))
        assertEquals("1 giờ", formatBlogTime("2026-06-07T11:00:00", now, utc))
        assertEquals(
            "vừa xong",
            formatBlogTime("2026-06-07T12:00:00", now, ZoneId.of("Asia/Bangkok"))
        )
    }

    @Test
    fun `handles future and invalid timestamps safely`() {
        assertEquals("vừa xong", formatBlogTime("2026-06-07T12:05:00Z", now, utc))
        assertEquals("07/06/2025", formatBlogTime("2025-06-07", now, utc))
        assertEquals("không rõ", formatBlogTime("không rõ", now, utc))
    }
}
