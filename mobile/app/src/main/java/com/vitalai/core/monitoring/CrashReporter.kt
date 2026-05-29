package com.vitalai.core.monitoring

import com.vitalai.BuildConfig
import com.vitalai.core.logging.AppLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrashReporter @Inject constructor(
    private val logger: AppLogger
) {
    fun initialize() {
        logger.debug(TAG, "Crash reporting skeleton initialized. Configure Firebase before enabling collection.")
    }

    fun recordNonFatal(throwable: Throwable) {
        logger.error(TAG, throwable.message ?: "Non-fatal exception", throwable)
        // TODO: Forward to Firebase Crashlytics after google-services.json and dashboard setup are available.
    }

    fun setUserId(userId: String?) {
        if (BuildConfig.DEBUG) {
            logger.debug(TAG, if (userId.isNullOrBlank()) "Crash user cleared" else "Crash user set")
        }
        // TODO: Set Crashlytics user id after Firebase configuration is added.
    }

    private companion object {
        const val TAG = "CrashReporter"
    }
}
