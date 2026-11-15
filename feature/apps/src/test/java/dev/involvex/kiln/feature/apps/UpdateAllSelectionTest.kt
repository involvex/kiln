package dev.involvex.kiln.feature.apps

import dev.involvex.kiln.core.model.AppUpdateInfo
import dev.involvex.kiln.core.model.MatchConfidence
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UpdateAllSelectionTest {
    @Test
    fun partition_keepsOnlySignatureSafeWithUrl() {
        val result = UpdateAllSelection.partition(
            listOf(
                update("a", signatureMatches = true, url = "https://example.com/a.apk"),
                update("b", signatureMatches = false, url = "https://example.com/b.apk"),
                update("c", signatureMatches = true, url = null),
                update("d", signatureMatches = true, url = "https://example.com/d.apk", watchOnly = true),
            ),
        )
        assertEquals(listOf("a"), result.installable.map { it.packageName })
        assertEquals(2, result.skippedCount)
    }

    @Test
    fun partition_emptyWhenOnlyWatchOnly() {
        val result = UpdateAllSelection.partition(
            listOf(update("w", signatureMatches = true, url = "https://example.com/w.apk", watchOnly = true)),
        )
        assertEquals(0, result.installable.size)
        assertEquals(0, result.skippedCount)
    }

    private fun update(packageName: String, signatureMatches: Boolean, url: String?, watchOnly: Boolean = false) = AppUpdateInfo(
        packageName = packageName,
        availableVersionName = "2.0",
        availableVersionCode = 2,
        sourceId = "fdroid",
        sourceLabel = "F-Droid",
        confidence = MatchConfidence.HIGH,
        signatureMatches = signatureMatches,
        downloadUrl = url,
        watchOnly = watchOnly,
    )
}
