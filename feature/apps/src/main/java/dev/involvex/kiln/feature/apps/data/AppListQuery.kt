package dev.involvex.kiln.feature.apps.data

import dev.involvex.kiln.core.model.AppListFilter
import dev.involvex.kiln.core.model.AppSortField
import dev.involvex.kiln.core.model.InstalledAppSummary
import dev.involvex.kiln.core.model.SortDirection

/**
 * Pure filter + sort for the installed-apps list. Unit-tested without Android.
 */
fun filterAndSortApps(
    apps: List<InstalledAppSummary>,
    query: String,
    filter: AppListFilter,
    sortField: AppSortField,
    sortDirection: SortDirection,
): List<InstalledAppSummary> {
    val normalizedQuery = query.trim()
    val filtered = apps.asSequence()
        .filter { app ->
            when (filter) {
                AppListFilter.USER -> !app.isSystem
                AppListFilter.SYSTEM -> app.isSystem
                AppListFilter.ALL -> true
            }
        }
        .filter { app ->
            if (normalizedQuery.isEmpty()) {
                true
            } else {
                app.label.contains(normalizedQuery, ignoreCase = true) ||
                    app.packageName.contains(normalizedQuery, ignoreCase = true)
            }
        }
        .toList()

    val comparator = when (sortField) {
        AppSortField.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.label }
        AppSortField.SIZE -> compareBy<InstalledAppSummary> { it.apkSizeBytes ?: -1L }
        AppSortField.INSTALL_DATE -> compareBy { it.firstInstallTime }
        AppSortField.UPDATE_DATE -> compareBy { it.lastUpdateTime }
    }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.label }
        .thenBy { it.packageName }

    val sorted = filtered.sortedWith(comparator)
    return if (sortDirection == SortDirection.DESCENDING) {
        sorted.asReversed()
    } else {
        sorted
    }
}
