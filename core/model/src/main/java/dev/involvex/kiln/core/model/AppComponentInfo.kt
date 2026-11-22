package dev.involvex.kiln.core.model

/** Kind of exported package component listed on App Detail (read-only). */
enum class AppComponentKind {
    SERVICE,
    RECEIVER,
    PROVIDER,
}

/**
 * Exported service / receiver / provider (Tier NONE listing).
 * Activities stay on [AppActivityInfo] because they support Start / pin shortcut.
 */
data class AppComponentInfo(
    val packageName: String,
    val className: String,
    val label: String,
    val exported: Boolean,
    val kind: AppComponentKind,
)

/** Pure filters for exported component lists (unit-tested). */
object AppComponentFilter {
    fun <T> exportedOnly(items: List<T>, isExported: (T) -> Boolean): List<T> = items.filter(isExported)

    fun matchesQuery(label: String, className: String, query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return label.contains(q, ignoreCase = true) || className.contains(q, ignoreCase = true)
    }
}
