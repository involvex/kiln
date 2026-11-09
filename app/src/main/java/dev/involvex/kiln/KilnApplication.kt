package dev.involvex.kiln

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dagger.hilt.android.HiltAndroidApp
import dev.involvex.kiln.core.model.AppearancePreferences
import dev.involvex.kiln.core.model.BackgroundUpdatePreferences
import dev.involvex.kiln.core.model.BackgroundUpdateScheduler
import dev.involvex.kiln.core.model.DebugLogController
import dev.involvex.kiln.core.model.DevPreferences
import dev.involvex.kiln.core.model.MaintenancePreferences
import dev.involvex.kiln.core.model.MaintenanceScheduler
import dev.involvex.kiln.feature.apps.icon.PackageIconFetcher
import dev.involvex.kiln.feature.apps.icon.PackageIconKeyer
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@HiltAndroidApp
class KilnApplication :
    Application(),
    Configuration.Provider,
    SingletonImageLoader.Factory {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var backgroundUpdatePreferences: BackgroundUpdatePreferences

    @Inject
    lateinit var backgroundUpdateScheduler: BackgroundUpdateScheduler

    @Inject
    lateinit var maintenancePreferences: MaintenancePreferences

    @Inject
    lateinit var maintenanceScheduler: MaintenanceScheduler

    @Inject
    lateinit var appearancePreferences: AppearancePreferences

    @Inject
    lateinit var devPreferences: DevPreferences

    @Inject
    lateinit var debugLogController: DebugLogController

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Apply locale once from persisted prefs, then on preference changes only.
        // Never apply from Compose with a default AppearancePrefs() — that races
        // DataStore and toggles en ↔ system locale on HyperOS (~6 relaunches/s).
        runBlocking {
            applyKilnLanguage(appearancePreferences.current().language)
        }
        applicationScope.launch {
            appearancePreferences.observe().collectLatest { prefs ->
                applyKilnLanguage(prefs.language)
            }
        }
        applicationScope.launch {
            // Plant immediately from persisted prefs (and always for debuggable builds).
            debugLogController.apply(devPreferences.current().debugLogEnabled)
            devPreferences.observe().collectLatest { prefs ->
                debugLogController.apply(prefs.debugLogEnabled)
            }
        }
        applicationScope.launch {
            backgroundUpdateScheduler.apply(backgroundUpdatePreferences.current())
        }
        applicationScope.launch {
            maintenanceScheduler.apply(maintenancePreferences.current())
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
