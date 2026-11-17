package dev.involvex.kiln.feature.widgets

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.model.AppUpdateInfo
import dev.involvex.kiln.core.model.AppUpdateRepository
import dev.involvex.kiln.core.model.WidgetPreferences
import dev.involvex.kiln.core.model.WidgetUpdateCount
import dev.involvex.kiln.core.model.WidgetUpdatePreview
import dev.involvex.kiln.core.model.WidgetUpdatePublisher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class WidgetCountPublisher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appUpdateRepository: AppUpdateRepository,
    private val widgetPreferences: WidgetPreferences,
    private val surfaceUpdater: WidgetSurfaceUpdater,
) : WidgetUpdatePublisher {
    override suspend fun publishFromMap(updates: Map<String, AppUpdateInfo>) {
        val labels = resolveLabels(updates.keys)
        val preview = WidgetUpdatePreview.lines(updates, labels)
        widgetPreferences.setUpdateCount(
            count = WidgetUpdateCount.count(updates),
            preview = preview,
        )
        surfaceUpdater.refreshAll()
    }

    override suspend fun publishCount(count: Int) {
        widgetPreferences.setUpdateCount(count = count, preview = emptyList())
        surfaceUpdater.refreshAll()
    }

    override suspend fun refreshFromLocalIndex() {
        appUpdateRepository.refreshUpdates()
        publishFromMap(appUpdateRepository.updates().first())
    }

    private fun resolveLabels(packageNames: Set<String>): Map<String, String> {
        val pm = context.packageManager
        return packageNames.associateWith { pkg ->
            runCatching {
                val info = pm.getApplicationInfo(pkg, 0)
                pm.getApplicationLabel(info).toString()
            }.getOrElse { pkg.substringAfterLast('.') }
        }
    }
}
