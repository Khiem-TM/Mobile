package com.vitalai

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import coil.util.DebugLogger
import com.vitalai.core.monitoring.CrashReporter
import com.vitalai.core.notification.NotificationChannels
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class VitalAIApp : Application(), Configuration.Provider, ImageLoaderFactory {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var crashReporter: CrashReporter

    override fun onCreate() {
        super.onCreate()
        crashReporter.initialize()
        NotificationChannels.ensure(this)
    }

    /**
     * App-wide Coil loader. Keep image loading independent from the Retrofit
     * client so third-party public image hosts are not sent API auth headers.
     */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient { imageOkHttpClient() }
            .components { add(SvgDecoder.Factory()) }
            .crossfade(true)
            .respectCacheHeaders(false)
            .logger(if (BuildConfig.DEBUG) DebugLogger(Log.DEBUG) else null)
            .build()

    private fun imageOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(IMAGE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(IMAGE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(IMAGE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", IMAGE_USER_AGENT)
                    .header("Accept", IMAGE_ACCEPT_HEADER)
                    .build()
                chain.proceed(request)
            }
            .build()

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private companion object {
        const val IMAGE_TIMEOUT_SECONDS = 30L
        const val IMAGE_ACCEPT_HEADER =
            "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8"
        const val IMAGE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/125.0 Mobile Safari/537.36"
    }
}
