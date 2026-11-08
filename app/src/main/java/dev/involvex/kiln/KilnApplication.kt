package dev.involvex.kiln

import android.app.Application
import android.content.pm.ApplicationInfo
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class KilnApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // FLAG_DEBUGGABLE avoids BuildConfig (disabled by default in modern AGP).
        val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebuggable) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
