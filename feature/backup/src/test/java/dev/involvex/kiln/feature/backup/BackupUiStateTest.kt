package dev.involvex.kiln.feature.backup

import dev.involvex.kiln.core.model.BackupAppCandidate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BackupUiStateTest {
    @Test
    fun filteredApps_matches_label_or_package() {
        val apps = listOf(
            candidate("com.alpha.one", "Alpha"),
            candidate("com.beta.two", "Beta Tools"),
        )
        val state = BackupUiState(apps = apps, query = "beta")
        assertEquals(listOf("com.beta.two"), state.filteredApps.map { it.packageName })
    }

    private fun candidate(pkg: String, label: String) = BackupAppCandidate(
        packageName = pkg,
        label = label,
        versionName = "1.0",
        versionCode = 1,
        isSystem = false,
        hasSplits = false,
        apkSizeBytes = 1024,
    )
}
