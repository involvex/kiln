package dev.involvex.kiln.feature.sources

import dev.involvex.kiln.core.model.AppUpdateInfo
import dev.involvex.kiln.core.model.MatchConfidence
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class UpdateCandidateFilterTest {
    @Test
    fun matching_is_kept() {
        assertTrue(
            DefaultAppUpdateRepository.isInstallableUpdateCandidate(
                sample(signatureMatches = true, confidence = MatchConfidence.HIGH),
            ),
        )
    }

    @Test
    fun warning_mismatch_is_kept() {
        assertTrue(
            DefaultAppUpdateRepository.isInstallableUpdateCandidate(
                sample(signatureMatches = false, confidence = MatchConfidence.WARNING),
            ),
        )
    }

    @Test
    fun github_medium_with_url_is_kept() {
        assertTrue(
            DefaultAppUpdateRepository.isInstallableUpdateCandidate(
                sample(
                    signatureMatches = false,
                    confidence = MatchConfidence.MEDIUM,
                    downloadUrl = "https://example.com/app.apk",
                ),
            ),
        )
    }

    @Test
    fun medium_without_url_is_dropped() {
        assertFalse(
            DefaultAppUpdateRepository.isInstallableUpdateCandidate(
                sample(signatureMatches = false, confidence = MatchConfidence.MEDIUM, downloadUrl = null),
            ),
        )
    }

    private fun sample(signatureMatches: Boolean, confidence: MatchConfidence, downloadUrl: String? = "https://example.com/a.apk") =
        AppUpdateInfo(
            packageName = "com.example",
            availableVersionName = "2",
            availableVersionCode = 2,
            sourceId = "github:o/r",
            sourceLabel = "GitHub",
            confidence = confidence,
            signatureMatches = signatureMatches,
            downloadUrl = downloadUrl,
        )
}
