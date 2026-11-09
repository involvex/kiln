package dev.involvex.kiln.feature.widgets

import dev.involvex.kiln.core.model.AppUpdateInfo
import dev.involvex.kiln.core.model.AppUpdateRepository
import dev.involvex.kiln.core.model.WidgetPreferences
import dev.involvex.kiln.core.model.WidgetUpdateCount
import dev.involvex.kiln.core.model.WidgetUpdatePublisher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class WidgetCountPublisher @Inject constructor(
    private val appUpdateRepository: AppUpdateRepository,
    private val widgetPreferences: WidgetPreferences,
    private val surfaceUpdater: WidgetSurfaceUpdater,
) : WidgetUpdatePublisher {
    override suspend fun publishFromMap(updates: Map<String, AppUpdateInfo>) {
        publishCount(WidgetUpdateCount.count(updates))
    }

    override suspend fun publishCount(count: Int) {
        widgetPreferences.setUpdateCount(count)
        surfaceUpdater.refreshAll()
    }

    override suspend fun refreshFromLocalIndex() {
        appUpdateRepository.refreshUpdates()
        publishFromMap(appUpdateRepository.updates().first())
    }
}
