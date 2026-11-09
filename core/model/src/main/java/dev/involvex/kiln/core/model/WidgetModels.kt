package dev.involvex.kiln.core.model

import kotlinx.coroutines.flow.Flow

data class WidgetSnapshot(
    val lastUpdateCount: Int = 0,
    val lastCheckedEpochMs: Long = 0L,
    /** True after Check now was queued until the next count publish. */
    val checkQueued: Boolean = false,
    /** Widget UI: awaiting confirm for destructive trim. */
    val trimConfirmPending: Boolean = false,
)

interface WidgetPreferences {
    fun observe(): Flow<WidgetSnapshot>

    suspend fun current(): WidgetSnapshot

    suspend fun setUpdateCount(count: Int, epochMs: Long = System.currentTimeMillis())

    suspend fun setCheckQueued(queued: Boolean)

    suspend fun setTrimConfirmPending(pending: Boolean)
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

/** Intent extras / actions shared by widget, tile, and MainActivity. */
object WidgetIntents {
    const val ACTION_CHECK_NOW = "dev.involvex.kiln.widget.CHECK_NOW"
    const val ACTION_TRIM_REQUEST = "dev.involvex.kiln.widget.TRIM_REQUEST"
    const val ACTION_TRIM_CONFIRM = "dev.involvex.kiln.widget.TRIM_CONFIRM"
    const val ACTION_TRIM_CANCEL = "dev.involvex.kiln.widget.TRIM_CANCEL"
    const val EXTRA_OPEN_MANAGE = "dev.involvex.kiln.extra.OPEN_MANAGE"
}
