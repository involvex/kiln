package dev.involvex.kiln.feature.permissions.background

import dev.involvex.kiln.core.model.AutostartListFilter
import dev.involvex.kiln.core.model.BackgroundControlsApp
import dev.involvex.kiln.core.model.BatteryUsageMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BackgroundControlsFilterTest {
    private val apps = listOf(
        app("com.a", "Alpha", autostart = true),
        app("com.b", "Beta", autostart = false),
        app("com.c", "Gamma", autostart = null),
    )

    @Test
    fun filter_byQuery() {
        val result = BackgroundControlsViewModel.filterApps(
            apps = apps,
            query = "bet",
            autostartFilter = AutostartListFilter.ALL,
            isHyperOs = true,
        )
        assertEquals(listOf("com.b"), result.map { it.packageName })
    }

    @Test
    fun filter_autostartEnabled() {
        val result = BackgroundControlsViewModel.filterApps(
            apps = apps,
            query = "",
            autostartFilter = AutostartListFilter.ENABLED,
            isHyperOs = true,
        )
        assertEquals(listOf("com.a"), result.map { it.packageName })
    }

    @Test
    fun filter_autostartDisabled() {
        val result = BackgroundControlsViewModel.filterApps(
            apps = apps,
            query = "",
            autostartFilter = AutostartListFilter.DISABLED,
            isHyperOs = true,
        )
        assertEquals(listOf("com.b"), result.map { it.packageName })
    }

    @Test
    fun filter_autostartIgnoredWhenNotHyperOs() {
        val result = BackgroundControlsViewModel.filterApps(
            apps = apps,
            query = "",
            autostartFilter = AutostartListFilter.ENABLED,
            isHyperOs = false,
        )
        assertEquals(3, result.size)
    }

    private fun app(pkg: String, label: String, autostart: Boolean?) = BackgroundControlsApp(
        packageName = pkg,
        label = label,
        isSystem = false,
        autostartAllowed = autostart,
        batteryMode = BatteryUsageMode.OPTIMIZED,
    )
}
