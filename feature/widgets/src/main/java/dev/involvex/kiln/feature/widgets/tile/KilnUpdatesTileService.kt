package dev.involvex.kiln.feature.widgets.tile

import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast
import dagger.hilt.android.AndroidEntryPoint
import dev.involvex.kiln.core.model.BackgroundUpdateScheduler
import dev.involvex.kiln.core.model.WidgetPreferences
import dev.involvex.kiln.core.model.WidgetSnapshot
import dev.involvex.kiln.feature.widgets.R
import dev.involvex.kiln.feature.widgets.WidgetSurfaceUpdater
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@AndroidEntryPoint
class KilnUpdatesTileService : TileService() {
    @Inject lateinit var widgetPreferences: WidgetPreferences

    @Inject lateinit var scheduler: BackgroundUpdateScheduler

    @Inject lateinit var surfaceUpdater: WidgetSurfaceUpdater

    private var listenJob: Job? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        super.onStartListening()
        listenJob?.cancel()
        listenJob = serviceScope.launch {
            widgetPreferences.observe().collectLatest { snapshot ->
                val tile = qsTile ?: return@collectLatest
                applySnapshot(tile, snapshot)
                tile.updateTile()
            }
        }
    }

    private fun applySnapshot(tile: Tile, snapshot: WidgetSnapshot) {
        val detail = when {
            snapshot.checkQueued -> getString(R.string.widget_updates_queued)

            snapshot.lastUpdateCount <= 0 -> getString(R.string.tile_updates_subtitle_zero)

            else -> resources.getQuantityString(
                R.plurals.tile_updates_subtitle_count,
                snapshot.lastUpdateCount,
                snapshot.lastUpdateCount,
            )
        }
        // Tile.setSubtitle is API 29+; fold detail into the label on older devices.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.label = getString(R.string.tile_updates_label)
            tile.subtitle = detail
        } else {
            tile.label = "${getString(R.string.tile_updates_label)} · $detail"
        }
        tile.state = if (snapshot.lastUpdateCount > 0) {
            Tile.STATE_ACTIVE
        } else {
            Tile.STATE_INACTIVE
        }
    }

    override fun onStopListening() {
        listenJob?.cancel()
        listenJob = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        serviceScope.launch(Dispatchers.IO) {
            try {
                scheduler.enqueueImmediate()
                widgetPreferences.setCheckQueued(true)
                surfaceUpdater.refreshAll()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        applicationContext,
                        getString(R.string.widget_updates_queued),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            } catch (@Suppress("TooGenericExceptionCaught") error: Exception) {
                Timber.w(error, "QS tile check now failed")
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
