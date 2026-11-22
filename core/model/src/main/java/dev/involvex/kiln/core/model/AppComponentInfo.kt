package dev.involvex.kiln.core.model

/** Kind of package component listed on App Detail. */
enum class AppComponentKind {
    SERVICE,
    RECEIVER,
    PROVIDER,
}

/**
 * Service / receiver / provider on App Detail.
 * Activities stay on [AppActivityInfo] because they support Start / pin shortcut.
 * Non-exported rows are listed read-only; toggles require privilege + exported.
 */
data class AppComponentInfo(
    val packageName: String,
    val className: String,
    val label: String,
    val exported: Boolean,
    val enabled: Boolean,
    val kind: AppComponentKind,
)

/** Pure filters for component lists (unit-tested). */
object AppComponentFilter {
    fun <T> exportedOnly(items: List<T>, isExported: (T) -> Boolean): List<T> = items.filter(isExported)

    fun matchesQuery(label: String, className: String, query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return label.contains(q, ignoreCase = true) || className.contains(q, ignoreCase = true)
    }
}
