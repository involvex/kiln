package dev.involvex.kiln.feature.installer

import dev.involvex.kiln.core.model.MatchConfidence

/**
 * Pure rules for whether an update with a non-matching / unknown signer may enter the install queue.
 */
object InstallEnqueueGate {
    sealed class Decision {
        data object Allow : Decision()

        /** Known mismatch; worker may skip ApkSignatureGate. */
        data object AllowSkipSignatureGate : Decision()

        data object Block : Decision()
    }

    fun decide(
        signatureMatches: Boolean,
        confidence: MatchConfidence,
        packageInstalled: Boolean,
        allowSignatureMismatch: Boolean,
    ): Decision {
        if (signatureMatches || !packageInstalled) return Decision.Allow
        return when (confidence) {
            MatchConfidence.WARNING -> {
                if (allowSignatureMismatch) Decision.AllowSkipSignatureGate else Decision.Block
            }

            // Unknown until download (e.g. GitHub MEDIUM): allow; worker verifies APK.
            else -> Decision.Allow
        }
    }
}
