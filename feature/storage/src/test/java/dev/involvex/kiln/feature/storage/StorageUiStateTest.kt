package dev.involvex.kiln.feature.storage

import dev.involvex.kiln.core.model.AppStorageStats
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StorageUiStateTest {
    @Test
    fun filteredApps_matches_label_or_package() {
        val apps = listOf(
            AppStorageStats("com.a.one", "Alpha", false, 1, 2, 3),
            AppStorageStats("com.b.two", "Beta", false, 4, 5, 6),
        )
        val state = StorageUiState(apps = apps, query = "beta")
        assertEquals(listOf(apps[1]), state.filteredApps)
        assertEquals(listOf(apps[0]), state.copy(query = "com.a").filteredApps)
        assertEquals(21L, state.copy(totalAppBytes = 10, totalDataBytes = 5, totalCacheBytes = 6).totalBytes)
    }
}
