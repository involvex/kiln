package dev.involvex.kiln.feature.widgets

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.feature.widgets.glance.KilnUpdatesGlanceWidget
import dev.involvex.kiln.feature.widgets.tile.KilnUpdatesTileService
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class WidgetSurfaceUpdater @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun refreshAll() {
        try {
            KilnUpdatesGlanceWidget().updateAll(context)
        } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
            Timber.w(error, "Failed to update Glance widget")
        }
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
