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
)

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
                .sortedBy { repoPriority[it.repoId] ?: Int.MAX_VALUE }
            if (candidates.isEmpty()) {
                val mapped = githubMap.lookup(app.packageName)
                if (mapped != null) {
                    MatchCandidate(
                        packageName = app.packageName,
                        label = app.label,
                        repoId = "github:${mapped.ownerRepo}",
                        sourceLabel = "GitHub ${mapped.ownerRepo}",
                        confidence = MatchConfidence.MEDIUM,
                        githubOwnerRepo = mapped.ownerRepo,
                        githubAssetRegex = mapped.assetRegex,
                    )
                } else {
                    MatchCandidate(
                        packageName = app.packageName,
                        label = app.label,
                        repoId = "",
                        sourceLabel = "",
                        confidence = MatchConfidence.NONE,
                    )
                }
            } else {
                val best = candidates.first()
                val indexSigners = best.signerSha256Csv.split(',').filter { it.isNotBlank() }
                val confidence = when {
                    indexSigners.isEmpty() -> MatchConfidence.MEDIUM

                    app.signerSha256.any { installedFp ->
                        indexSigners.any { fingerprintsEqual(installedFp, it) }
                    } -> MatchConfidence.HIGH

                    else -> MatchConfidence.WARNING
                }
                val githubSuggestion = best.sourceCodeUrl
                    ?.takeIf { it.contains("github.com", ignoreCase = true) }
                MatchCandidate(
                    packageName = app.packageName,
                    label = app.label,
                    repoId = best.repoId,
                    sourceLabel = repoNames[best.repoId] ?: best.repoId,
                    confidence = confidence,
                    sourceCodeSuggestion = githubSuggestion,
                )
            }
        }
    }
}
