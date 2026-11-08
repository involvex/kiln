package dev.involvex.kiln.feature.sources.updatecheck

import dev.involvex.kiln.core.model.AppUpdateInfo
import dev.involvex.kiln.core.model.MatchConfidence
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AutoInstallPolicyTest {
    @Test
    fun disabled_returns_empty() {
        val updates = listOf(matching("a"), mismatch("b"))
        assertTrue(AutoInstallPolicy.selectForEnqueue(updates, autoInstallMatching = false).isEmpty())
    }

    @Test
    fun enabled_only_signature_matches() {
        val updates = listOf(matching("ok"), mismatch("warn"), matching("ok2"))
        val selected = AutoInstallPolicy.selectForEnqueue(updates, autoInstallMatching = true)
        assertEquals(listOf("ok", "ok2"), selected.map { it.packageName })
    }

    private fun matching(pkg: String) = AppUpdateInfo(
        packageName = pkg,
        availableVersionName = "1.1",
        availableVersionCode = 2,
        sourceId = "fdroid",
        sourceLabel = "F-Droid",
        confidence = MatchConfidence.HIGH,
        signatureMatches = true,
        downloadUrl = "https://example.com/$pkg.apk",
        apkSha256 = "abc",
        sizeBytes = 1,
    )

    private fun mismatch(pkg: String) = matching(pkg).copy(
        signatureMatches = false,
        confidence = MatchConfidence.WARNING,
    )
}
