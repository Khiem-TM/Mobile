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
        val trimmed = normalizeRawUrl(url) ?: return null
        if (trimmed.startsWith("//")) {
            return "https:$trimmed"
        }
        val safeUrl = trimmed.replace(" ", "%20")
        if (safeUrl.startsWith("http://", ignoreCase = true) ||
            safeUrl.startsWith("https://", ignoreCase = true)
        ) {
            return safeUrl
        }
        val base = BuildConfig.BASE_URL.trimEnd('/')
        val path = if (safeUrl.startsWith("/")) safeUrl else "/$safeUrl"
        return "$base$path"
    }

    fun resolveFirst(vararg urls: String?): String? =
        urls.firstNotNullOfOrNull { resolve(it) }

    fun resolveFirst(urls: Iterable<String?>?): String? =
        urls?.firstNotNullOfOrNull { resolve(it) }

    fun compactResolvable(urls: Iterable<String?>?): List<String>? =
        urls
            ?.mapNotNull { url -> url?.takeIf { resolve(it) != null } }
            ?.takeIf { it.isNotEmpty() }

    private fun normalizeRawUrl(raw: String): String? {
        var value = raw.trim()
        if (value.isBlank() || value == "{}") return null

        // Be tolerant of PostgreSQL text-array values accidentally delivered as
        // a raw string, e.g. "{https://...}" or "{\"https://...\"}".
        if (value.startsWith("{") && value.endsWith("}")) {
            value = value.removePrefix("{").removeSuffix("}").trim()
            if (value.startsWith("\"")) {
                val closingQuote = value.indexOf('"', startIndex = 1)
                if (closingQuote > 0) value = value.substring(1, closingQuote)
            }
        }

        value = value
            .trim()
            .trim('"')
            .trim('\'')
            .trim()

        return value.takeIf { it.isNotBlank() && it != "{}" }
    }
}
