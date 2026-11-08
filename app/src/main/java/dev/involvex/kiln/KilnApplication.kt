package dev.involvex.kiln

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dagger.hilt.android.HiltAndroidApp
import dev.involvex.kiln.feature.apps.icon.PackageIconFetcher
import dev.involvex.kiln.feature.apps.icon.PackageIconKeyer
import javax.inject.Inject
import timber.log.Timber

@HiltAndroidApp
class KilnApplication :
    Application(),
    Configuration.Provider,
    SingletonImageLoader.Factory {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        // FLAG_DEBUGGABLE avoids BuildConfig (disabled by default in modern AGP).
        val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebuggable) {
            Timber.plant(Timber.DebugTree())
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun newImageLoader(context: PlatformContext): ImageLoader = ImageLoader.Builder(context)
        .components {
            add(PackageIconFetcher.Factory(this@KilnApplication))
            add(PackageIconKeyer())
        }
        .build()
}
