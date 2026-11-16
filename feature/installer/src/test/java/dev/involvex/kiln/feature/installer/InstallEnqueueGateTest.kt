package dev.involvex.kiln.feature.installer

import dev.involvex.kiln.core.model.MatchConfidence
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InstallEnqueueGateTest {
    @Test
    fun matchingSigner_allows() {
        assertEquals(
            InstallEnqueueGate.Decision.Allow,
            InstallEnqueueGate.decide(
                signatureMatches = true,
                confidence = MatchConfidence.HIGH,
                packageInstalled = true,
                allowSignatureMismatch = false,
            ),
        )
    }

    @Test
    fun unknownSigner_medium_installed_allows_without_override() {
        assertEquals(
            InstallEnqueueGate.Decision.Allow,
            InstallEnqueueGate.decide(
                signatureMatches = false,
                confidence = MatchConfidence.MEDIUM,
                packageInstalled = true,
                allowSignatureMismatch = false,
            ),
        )
    }

    @Test
    fun warning_installed_blocks_without_override() {
        assertEquals(
            InstallEnqueueGate.Decision.Block,
            InstallEnqueueGate.decide(
                signatureMatches = false,
                confidence = MatchConfidence.WARNING,
                packageInstalled = true,
                allowSignatureMismatch = false,
            ),
        )
    }

    @Test
    fun warning_installed_with_override_skips_gate() {
        assertEquals(
            InstallEnqueueGate.Decision.AllowSkipSignatureGate,
            InstallEnqueueGate.decide(
                signatureMatches = false,
                confidence = MatchConfidence.WARNING,
                packageInstalled = true,
                allowSignatureMismatch = true,
            ),
        )
    }

    @Test
    fun warning_not_installed_allows() {
        assertEquals(
            InstallEnqueueGate.Decision.Allow,
            InstallEnqueueGate.decide(
                signatureMatches = false,
                confidence = MatchConfidence.WARNING,
                packageInstalled = false,
                allowSignatureMismatch = false,
            ),
        )
    }
}
