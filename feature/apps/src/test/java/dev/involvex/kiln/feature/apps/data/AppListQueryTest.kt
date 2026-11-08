package dev.involvex.kiln.feature.apps.data

import dev.involvex.kiln.core.model.AppListFilter
import dev.involvex.kiln.core.model.AppSortField
import dev.involvex.kiln.core.model.InstalledAppSummary
import dev.involvex.kiln.core.model.SortDirection
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AppListQueryTest {
    private val apps = listOf(
        summary("com.a", "Alpha", isSystem = false, size = 100, install = 10, update = 30),
        summary("com.b", "Beta", isSystem = true, size = 300, install = 20, update = 10),
        summary("com.c", "Charlie", isSystem = false, size = 200, install = 5, update = 40),
    )

    @Test
    fun `filters user apps by default semantics`() {
        val result = filterAndSortApps(
            apps = apps,
            query = "",
            filter = AppListFilter.USER,
            sortField = AppSortField.NAME,
            sortDirection = SortDirection.ASCENDING,
        )
        assertEquals(listOf("com.a", "com.c"), result.map { it.packageName })
    }

    @Test
    fun `search matches label or package case insensitive`() {
        val result = filterAndSortApps(
            apps = apps,
            query = "COM.B",
            filter = AppListFilter.ALL,
            sortField = AppSortField.NAME,
            sortDirection = SortDirection.ASCENDING,
        )
        assertEquals(listOf("com.b"), result.map { it.packageName })
    }

    @Test
    fun `sorts by size descending`() {
        val result = filterAndSortApps(
            apps = apps,
            query = "",
            filter = AppListFilter.ALL,
            sortField = AppSortField.SIZE,
            sortDirection = SortDirection.DESCENDING,
        )
        assertEquals(listOf("com.b", "com.c", "com.a"), result.map { it.packageName })
    }

    @Test
    fun `sorts by install date`() {
        val result = filterAndSortApps(
            apps = apps,
            query = "",
            filter = AppListFilter.ALL,
            sortField = AppSortField.INSTALL_DATE,
            sortDirection = SortDirection.ASCENDING,
        )
        assertEquals(listOf("com.c", "com.a", "com.b"), result.map { it.packageName })
    }

    @Test
    fun `updates filter keeps only packages with known updates`() {
        val result = filterAndSortApps(
            apps = apps,
            query = "",
            filter = AppListFilter.UPDATES,
            sortField = AppSortField.NAME,
            sortDirection = SortDirection.ASCENDING,
            updatePackageNames = setOf("com.b", "com.c"),
        )
        assertEquals(listOf("com.b", "com.c"), result.map { it.packageName })
    }

    private fun summary(packageName: String, label: String, isSystem: Boolean, size: Long, install: Long, update: Long) =
        InstalledAppSummary(
            packageName = packageName,
            label = label,
            versionName = "1.0",
            versionCode = 1,
            isSystem = isSystem,
            isUpdatedSystemApp = false,
            apkSizeBytes = size,
            hasSplits = false,
            lastUpdateTime = update,
            firstInstallTime = install,
        )
}
