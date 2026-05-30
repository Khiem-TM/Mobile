package com.vitalai

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import com.vitalai.core.monitoring.CrashReporter
import dagger.hilt.android.HiltAndroidApp
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
    }

    /**
     * App-wide Coil loader. Registering [SvgDecoder] is required because the
     * backend serves seeded food/exercise thumbnails as .svg files, which the
     * default Coil loader cannot decode.
     */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(SvgDecoder.Factory()) }
            .crossfade(true)
            .build()

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
