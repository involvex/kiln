package dev.involvex.kiln.feature.onboarding.match

import dev.involvex.kiln.core.common.fingerprintsEqual
import dev.involvex.kiln.core.database.IndexedPackageEntity
import dev.involvex.kiln.core.model.InstalledAppMatchInput
import dev.involvex.kiln.core.model.MatchConfidence
import dev.involvex.kiln.feature.onboarding.github.GitHubMap

data class MatchCandidate(
    val packageName: String,
    val label: String,
    val repoId: String,
    val sourceLabel: String,
    val confidence: MatchConfidence,
    val githubOwnerRepo: String? = null,
    val githubAssetRegex: String? = null,
    val sourceCodeSuggestion: String? = null,
    /** Other index repos for this package (excluding the chosen [repoId]), for manual pick. */
    val alternateRepoIds: List<String> = emptyList(),
    val alternateRepoLabels: List<String> = emptyList(),
    /** Stable key for UI rationale strings (not user-facing by itself). */
    val rationaleKey: String = Rationale.NONE,
) {
    object Rationale {
        const val NONE = "none"
        const val SIGNER_MATCH = "signer_match"
        const val INDEX_NO_SIGNERS = "index_no_signers"
        const val GITHUB_MAP = "github_map"
        const val SIGNER_MISMATCH = "signer_mismatch"
        const val NOT_FOUND = "not_found"
    }
}

object AutoMatchEngine {
    fun match(
        installed: List<InstalledAppMatchInput>,
        indexed: List<IndexedPackageEntity>,
        repoPriority: Map<String, Int>,
        repoNames: Map<String, String>,
        githubMap: GitHubMap,
    ): List<MatchCandidate> {
        val byPackage = indexed.groupBy { it.packageName }
        return installed.map { app ->
            val candidates = byPackage[app.packageName].orEmpty()
            if (candidates.isEmpty()) {
                githubOnlyCandidate(app, githubMap)
            } else {
                indexCandidate(app, candidates, repoPriority, repoNames)
            }
        }
    }

    private fun githubOnlyCandidate(app: InstalledAppMatchInput, githubMap: GitHubMap): MatchCandidate {
        val mapped = githubMap.lookup(app.packageName)
        return if (mapped != null) {
            MatchCandidate(
                packageName = app.packageName,
                label = app.label,
                repoId = "github:${mapped.ownerRepo}",
                sourceLabel = "GitHub ${mapped.ownerRepo}",
                confidence = MatchConfidence.MEDIUM,
                githubOwnerRepo = mapped.ownerRepo,
                githubAssetRegex = mapped.assetRegex,
                rationaleKey = MatchCandidate.Rationale.GITHUB_MAP,
            )
        } else {
            MatchCandidate(
                packageName = app.packageName,
                label = app.label,
                repoId = "",
                sourceLabel = "",
                confidence = MatchConfidence.NONE,
                rationaleKey = MatchCandidate.Rationale.NOT_FOUND,
            )
        }
    }

    private fun indexCandidate(
        app: InstalledAppMatchInput,
        candidates: List<IndexedPackageEntity>,
        repoPriority: Map<String, Int>,
        repoNames: Map<String, String>,
    ): MatchCandidate {
        val scored = candidates.map { pkg ->
            ScoredIndexHit(
                pkg = pkg,
                confidence = confidenceFor(app, pkg),
                priority = repoPriority[pkg.repoId] ?: Int.MAX_VALUE,
            )
        }
        // Prefer matching signers (HIGH), then MEDIUM, then WARNING; tie-break by repo priority.
        val best = scored.minWith(
            compareBy<ScoredIndexHit> { it.confidence.rank }
                .thenBy { it.priority },
        )
        val alternates = scored
            .filter { it.pkg.repoId != best.pkg.repoId }
            .sortedWith(compareBy({ it.confidence.rank }, { it.priority }))
        val githubSuggestion = best.pkg.sourceCodeUrl
            ?.takeIf { it.contains("github.com", ignoreCase = true) }
        return MatchCandidate(
            packageName = app.packageName,
            label = app.label,
            repoId = best.pkg.repoId,
            sourceLabel = repoNames[best.pkg.repoId] ?: best.pkg.repoId,
            confidence = best.confidence,
            sourceCodeSuggestion = githubSuggestion,
            alternateRepoIds = alternates.map { it.pkg.repoId },
            alternateRepoLabels = alternates.map { repoNames[it.pkg.repoId] ?: it.pkg.repoId },
            rationaleKey = when (best.confidence) {
                MatchConfidence.HIGH -> MatchCandidate.Rationale.SIGNER_MATCH
                MatchConfidence.MEDIUM -> MatchCandidate.Rationale.INDEX_NO_SIGNERS
                MatchConfidence.WARNING -> MatchCandidate.Rationale.SIGNER_MISMATCH
                MatchConfidence.NONE -> MatchCandidate.Rationale.NOT_FOUND
            },
        )
    }

    private fun confidenceFor(app: InstalledAppMatchInput, pkg: IndexedPackageEntity): MatchConfidence {
        val indexSigners = pkg.signerSha256Csv.split(',').filter { it.isNotBlank() }
        return when {
            indexSigners.isEmpty() -> MatchConfidence.MEDIUM

            app.signerSha256.any { installedFp ->
                indexSigners.any { fingerprintsEqual(installedFp, it) }
            } -> MatchConfidence.HIGH

            else -> MatchConfidence.WARNING
        }
    }

    private data class ScoredIndexHit(val pkg: IndexedPackageEntity, val confidence: MatchConfidence, val priority: Int)

    private val MatchConfidence.rank: Int
        get() = when (this) {
            MatchConfidence.HIGH -> 0
            MatchConfidence.MEDIUM -> 1
            MatchConfidence.WARNING -> 2
            MatchConfidence.NONE -> 3
        }
}
