package dev.involvex.kiln.core.model

import kotlinx.coroutines.flow.Flow

data class WidgetUpdatePreviewLine(val packageName: String, val label: String, val availableVersionName: String)

data class WidgetSnapshot(
    val lastUpdateCount: Int = 0,
    val lastCheckedEpochMs: Long = 0L,
    /** True after Check now was queued until the next count publish. */
    val checkQueued: Boolean = false,
    /** Up to [WidgetUpdatePreview.MAX_LINES] installed apps with updates. */
    val updatePreview: List<WidgetUpdatePreviewLine> = emptyList(),
    /** Cache widget: awaiting confirm for destructive trim. */
    val cacheConfirmPending: Boolean = false,
    /** Processes widget: package awaiting force-stop confirm (null = none). */
    val killConfirmPackage: String? = null,
    val lastProcessRefreshEpochMs: Long = 0L,
)

interface WidgetPreferences {
    fun observe(): Flow<WidgetSnapshot>

    suspend fun current(): WidgetSnapshot

    suspend fun setUpdateCount(
        count: Int,
        preview: List<WidgetUpdatePreviewLine> = emptyList(),
        epochMs: Long = System.currentTimeMillis(),
    )

    suspend fun setCheckQueued(queued: Boolean)

    suspend fun setCacheConfirmPending(pending: Boolean)

    suspend fun setKillConfirmPackage(packageName: String?)

    suspend fun setLastProcessRefreshEpochMs(epochMs: Long)
}

/**
 * Publishes update counts to home/QS surfaces.
 * Implemented in `:feature:widgets`; injected into workers via `:core:model`.
 */
interface WidgetUpdatePublisher {
    suspend fun publishFromMap(updates: Map<String, AppUpdateInfo>)

    suspend fun publishCount(count: Int)

    /** Local recompute (no full F-Droid sync) for cold widget refresh. */
    suspend fun refreshFromLocalIndex()
}

/** Pure helper for unit tests and publishers. */
object WidgetUpdateCount {
    fun count(updates: Map<String, AppUpdateInfo>): Int = updates.values.count { !it.watchOnly }
}

object WidgetUpdatePreview {
    const val MAX_LINES = 5

    fun lines(
        updates: Map<String, AppUpdateInfo>,
        labels: Map<String, String> = emptyMap(),
        limit: Int = MAX_LINES,
    ): List<WidgetUpdatePreviewLine> = updates.values
        .asSequence()
        .filter { !it.watchOnly }
        .map { info ->
            val label = labels[info.packageName]?.takeIf { it.isNotBlank() }
                ?: info.packageName.substringAfterLast('.')
            WidgetUpdatePreviewLine(
                packageName = info.packageName,
                label = label,
                availableVersionName = info.availableVersionName,
            )
        }
        .sortedBy { it.label.lowercase() }
        .take(limit.coerceAtLeast(0))
        .toList()
}

/** Intent extras / actions shared by widget, tile, and MainActivity. */
object WidgetIntents {
    const val ACTION_CHECK_NOW = "dev.involvex.kiln.widget.CHECK_NOW"
    const val ACTION_CACHE_REQUEST = "dev.involvex.kiln.widget.CACHE_REQUEST"
    const val ACTION_CACHE_CONFIRM = "dev.involvex.kiln.widget.CACHE_CONFIRM"
    const val ACTION_CACHE_CANCEL = "dev.involvex.kiln.widget.CACHE_CANCEL"
    const val ACTION_PROCESS_REFRESH = "dev.involvex.kiln.widget.PROCESS_REFRESH"
    const val ACTION_KILL_REQUEST = "dev.involvex.kiln.widget.KILL_REQUEST"
    const val ACTION_KILL_CONFIRM = "dev.involvex.kiln.widget.KILL_CONFIRM"
    const val ACTION_KILL_CANCEL = "dev.involvex.kiln.widget.KILL_CANCEL"
    const val EXTRA_PACKAGE_NAME = "dev.involvex.kiln.extra.PACKAGE_NAME"
    const val EXTRA_OPEN_MANAGE = "dev.involvex.kiln.extra.OPEN_MANAGE"
    const val EXTRA_OPEN_PRIVILEGE_SETUP = "dev.involvex.kiln.extra.OPEN_PRIVILEGE_SETUP"

    /** Open Apps → Updates filter (notification / Updates widget). */
    const val EXTRA_OPEN_UPDATES = "dev.involvex.kiln.extra.OPEN_UPDATES"

    /** @deprecated Trim moved to Cache widget; kept so old APK broadcasts are ignored safely. */
    const val ACTION_TRIM_REQUEST = "dev.involvex.kiln.widget.TRIM_REQUEST"
    const val ACTION_TRIM_CONFIRM = "dev.involvex.kiln.widget.TRIM_CONFIRM"
    const val ACTION_TRIM_CANCEL = "dev.involvex.kiln.widget.TRIM_CANCEL"
}
