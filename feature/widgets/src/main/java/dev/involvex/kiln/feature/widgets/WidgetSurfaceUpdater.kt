package dev.involvex.kiln.feature.widgets

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.feature.widgets.glance.KilnCacheGlanceWidget
import dev.involvex.kiln.feature.widgets.glance.KilnProcessesGlanceWidget
import dev.involvex.kiln.feature.widgets.glance.KilnUpdatesGlanceWidget
import dev.involvex.kiln.feature.widgets.tile.KilnUpdatesTileService
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class WidgetSurfaceUpdater @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun refreshAll() {
        refreshUpdates()
        refreshCache()
        refreshProcesses()
        refreshTile()
    }

    suspend fun refreshUpdates() {
        try {
            KilnUpdatesGlanceWidget().updateAll(context)
        } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
            Timber.w(error, "Failed to update Updates Glance widget")
        }
    }

    suspend fun refreshCache() {
        try {
            KilnCacheGlanceWidget().updateAll(context)
        } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
            Timber.w(error, "Failed to update Cache Glance widget")
        }
    }

    suspend fun refreshProcesses() {
        try {
            KilnProcessesGlanceWidget().updateAll(context)
        } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
            Timber.w(error, "Failed to update Processes Glance widget")
        }
    }

    private fun refreshTile() {
        try {
            TileService.requestListeningState(
                context,
                ComponentName(context, KilnUpdatesTileService::class.java),
            )
        } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
            Timber.w(error, "Failed to request QS tile refresh")
        }
    }
}
