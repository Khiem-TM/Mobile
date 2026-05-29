package com.vitalai.core.logging

import android.util.Log
import com.vitalai.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLogger @Inject constructor() {
    fun debug(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.d(tag, sanitize(message))
    }

    fun warning(tag: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) Log.w(tag, sanitize(message), throwable)
    }

    fun error(tag: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) Log.e(tag, sanitize(message), throwable)
    }

    private fun sanitize(message: String): String {
        return SENSITIVE_PATTERN.replace(message) { match ->
            "${match.groupValues[1]}=<redacted>"
        }
    }

    private companion object {
        val SENSITIVE_PATTERN = Regex(
            "(access[_-]?token|refresh[_-]?token|authorization|password)\\s*[=:]\\s*([^\\s,}]+)",
            RegexOption.IGNORE_CASE
        )
    }
}
