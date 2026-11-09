package dev.involvex.kiln.core.model

import java.util.UUID
import kotlinx.coroutines.flow.Flow

enum class ActivityLogType {
    INSTALL,
    BACKUP,
    RESTORE,
    CACHE,
    MAINTENANCE,
    DEBLOAT,
    SHELL,
    TERMUX,
    AUTOMATION,
}

/**
 * One durable audit row. Never store URLs, filesystem paths, tokens, or raw shell output.
 */
data class ActivityLogEvent(
    val id: String,
    val epochMs: Long,
    val type: ActivityLogType,
    val action: String,
    val packageName: String? = null,
    val versionName: String? = null,
    val versionCode: Long? = null,
    val success: Boolean,
    val detailKey: String? = null,
    val refId: String? = null,
)

data class ActivityLogQuery(val search: String = "", val type: ActivityLogType? = null, val limit: Int = ActivityLogDefaults.UI_LIMIT)

/** Writers: fire-and-forget; must never throw into user flows. */
interface ActivityLogRecorder {
    suspend fun record(event: ActivityLogEvent)
}

interface ActivityLogRepository {
    fun observe(query: ActivityLogQuery): Flow<List<ActivityLogEvent>>

    suspend fun clearAll()
}

object ActivityLogDefaults {
    const val MAX_ENTRIES = 500
    const val UI_LIMIT = 200
    const val DETAIL_KEY_PATH_PREFIX_MAX = 64
    const val DETAIL_KEY_MAX = 96
}

object ActivityLogEvents {
    fun create(
        type: ActivityLogType,
        action: String,
        success: Boolean,
        packageName: String? = null,
        versionName: String? = null,
        versionCode: Long? = null,
        detailKey: String? = null,
        refId: String? = null,
        epochMs: Long = System.currentTimeMillis(),
        id: String = UUID.randomUUID().toString(),
    ): ActivityLogEvent = ActivityLogEvent(
        id = id,
        epochMs = epochMs,
        type = type,
        action = action,
        packageName = packageName,
        versionName = versionName,
        versionCode = versionCode,
        success = success,
        detailKey = detailKey?.let { sanitizeDetailKey(it) },
        refId = refId,
    )

    /** Keeps only a short stable key; strips path-like / URL-like noise. */
    fun sanitizeDetailKey(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return "unknown"
        if (trimmed.contains("://") || trimmed.contains('/') || trimmed.contains('\\')) {
            val beforeColon = trimmed.substringBefore(':').ifBlank { "error" }
            return beforeColon.take(ActivityLogDefaults.DETAIL_KEY_PATH_PREFIX_MAX)
        }
        return trimmed.take(ActivityLogDefaults.DETAIL_KEY_MAX)
    }
}

/** Pure filter for unit tests and ViewModel refine. */
object ActivityLogFilter {
    fun filter(entries: List<ActivityLogEvent>, query: String, type: ActivityLogType? = null): List<ActivityLogEvent> {
        val needle = query.trim().lowercase()
        return entries.asSequence()
            .filter { type == null || it.type == type }
            .filter { event ->
                if (needle.isEmpty()) return@filter true
                listOfNotNull(
                    event.packageName,
                    event.action,
                    event.detailKey,
                    event.versionName,
                    event.refId,
                    event.type.name,
                ).any { it.lowercase().contains(needle) }
            }
            .toList()
    }
}
