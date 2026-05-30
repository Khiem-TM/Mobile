package com.vitalai.core.network

import com.vitalai.BuildConfig

/**
 * Central helper that turns image references coming from the backend into URLs
 * Coil can actually load.
 *
 * The backend mixes two kinds of references:
 *  - Absolute URLs (e.g. Cloudinary uploads): returned untouched.
 *  - Relative paths for seeded assets (e.g. "/seed-images/foods/pho.svg"): these
 *    are served by the NestJS static handler and must be prefixed with the API
 *    base URL, otherwise Coil receives a bare path and fails to load anything.
 *
 * All image fields should be routed through [resolve] at the DTO boundary so URL
 * normalization lives in exactly one place.
 */
object ImageUrlResolver {

    fun resolve(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val trimmed = url.trim()
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return trimmed
        }
        val base = BuildConfig.BASE_URL.trimEnd('/')
        val path = if (trimmed.startsWith("/")) trimmed else "/$trimmed"
        return "$base$path"
    }
}
