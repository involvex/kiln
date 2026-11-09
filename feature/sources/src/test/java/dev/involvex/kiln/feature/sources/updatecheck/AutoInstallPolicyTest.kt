package dev.involvex.kiln.feature.sources.updatecheck

import dev.involvex.kiln.core.model.AppUpdateInfo
import dev.involvex.kiln.core.model.MatchConfidence
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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

    @Test
    fun watch_only_skipped() {
        val updates = listOf(matching("watched").copy(watchOnly = true), matching("ok"))
        val selected = AutoInstallPolicy.selectForEnqueue(updates, autoInstallMatching = true)
        assertEquals(listOf("ok"), selected.map { it.packageName })
    }

    @Test
    fun major_bump_skipped() {
        val updates = listOf(matching("app").copy(availableVersionName = "2.0.0"))
        val selected = AutoInstallPolicy.selectForEnqueue(
            updates = updates,
            autoInstallMatching = true,
            installedVersionNames = mapOf("app" to "1.9.0"),
        )
        assertTrue(selected.isEmpty())
    }

    @Test
    fun minor_bump_allowed() {
        val updates = listOf(matching("app").copy(availableVersionName = "1.10.0"))
        val selected = AutoInstallPolicy.selectForEnqueue(
            updates = updates,
            autoInstallMatching = true,
            installedVersionNames = mapOf("app" to "1.9.0"),
        )
        assertEquals(listOf("app"), selected.map { it.packageName })
    }

    @Test
    fun cooldown_blocks_until_elapsed() {
        val updates = listOf(matching("app"))
        val now = 1_000_000L
        val firstSeen = mapOf("app:2" to now - 12 * 60 * 60 * 1000L) // 12h ago
        val blocked = AutoInstallPolicy.selectForEnqueue(
            updates = updates,
            autoInstallMatching = true,
            firstSeenEpochMs = firstSeen,
            cooldownDays = 1,
            nowEpochMs = now,
        )
        assertTrue(blocked.isEmpty())
        val allowed = AutoInstallPolicy.selectForEnqueue(
            updates = updates,
            autoInstallMatching = true,
            firstSeenEpochMs = firstSeen,
            cooldownDays = 1,
            nowEpochMs = now + 25 * 60 * 60 * 1000L,
        )
        assertEquals(listOf("app"), allowed.map { it.packageName })
    }

    @Test
    fun isMajorBump_parses_leading_number() {
        assertTrue(AutoInstallPolicy.isMajorBump("1.2.3", "2.0.0"))
        assertFalse(AutoInstallPolicy.isMajorBump("2.0.0", "2.1.0"))
        assertFalse(AutoInstallPolicy.isMajorBump(null, "2.0.0"))
        assertFalse(AutoInstallPolicy.isMajorBump("nightly", "2.0.0"))
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
