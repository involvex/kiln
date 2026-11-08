package dev.involvex.kiln.feature.sources

import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.common.fingerprintsEqual
import dev.involvex.kiln.core.database.IndexDao
import dev.involvex.kiln.core.database.ManagedAppDao
import dev.involvex.kiln.core.database.RepoDao
import dev.involvex.kiln.core.model.AppUpdateInfo
import dev.involvex.kiln.core.model.AppUpdateRepository
import dev.involvex.kiln.core.model.InstalledAppsReader
import dev.involvex.kiln.core.model.MatchConfidence
import dev.involvex.kiln.feature.sources.github.GitHubReleasesClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

@Singleton
class DefaultAppUpdateRepository @Inject constructor(
    private val managedAppDao: ManagedAppDao,
    private val indexDao: IndexDao,
    private val repoDao: RepoDao,
    private val installedAppsReader: InstalledAppsReader,
    private val gitHubReleasesClient: GitHubReleasesClient,
) : AppUpdateRepository {
    private val updatesState = MutableStateFlow<Map<String, AppUpdateInfo>>(emptyMap())

    override fun updates(): Flow<Map<String, AppUpdateInfo>> = updatesState.asStateFlow()

    override suspend fun refreshUpdates() {
        val installed = installedAppsReader.userApps().associateBy { it.packageName }
        val repos = repoDao.getAll().associateBy { it.id }
        val out = linkedMapOf<String, AppUpdateInfo>()
        managedAppDao.getAll()
            .filter { !it.ignoreUpdates }
            .mapNotNull { app -> installed[app.packageName]?.let { local -> app to local } }
            .forEach { (app, local) ->
                val fromIndex = findFdroidUpdate(
                    app.packageName,
                    app.sourceId,
                    local.versionCode,
                    local.versionName,
                    local.signerSha256,
                    repos,
                )
                val ownerRepo = app.githubOwnerRepo
                val fromGitHub = if (ownerRepo != null) {
                    findGitHubUpdate(app.packageName, ownerRepo, app.githubAssetRegex, app.includePreReleases, local)
                } else {
                    null
                }
                val best = listOfNotNull(fromIndex, fromGitHub)
                    .filter { it.signatureMatches || it.confidence == MatchConfidence.WARNING }
                    .maxWithOrNull(
                        compareBy<AppUpdateInfo> { it.signatureMatches }
                            .thenBy { it.availableVersionCode },
                    )
                if (best != null) {
                    out[app.packageName] = best
                }
            }
        updatesState.value = out
    }

    private suspend fun findFdroidUpdate(
        packageName: String,
        preferredSourceId: String,
        installedCode: Long,
        installedName: String?,
        installedSigners: List<String>,
        repos: Map<String, dev.involvex.kiln.core.database.RepoEntity>,
    ): AppUpdateInfo? {
        val releases = indexDao.releasesFor(packageName)
        if (releases.isEmpty()) return null
        val ordered = releases.sortedWith(
            compareBy<dev.involvex.kiln.core.database.IndexedReleaseEntity> {
                if (it.repoId == preferredSourceId) 0 else 1
            }.thenBy { repos[it.repoId]?.priority ?: Int.MAX_VALUE }
                .thenByDescending { it.versionCode },
        )
        val latest = ordered.first()
        if (!isRemoteNewer(installedCode, installedName, latest.versionCode, latest.versionName)) {
            return null
        }
        val indexSigners = latest.signerSha256Csv.split(',').filter { it.isNotBlank() }
        val matches = indexSigners.isEmpty() || installedSigners.any { inst ->
            indexSigners.any { fingerprintsEqual(inst, it) }
        }
        val confidence = when {
            matches && indexSigners.isNotEmpty() -> MatchConfidence.HIGH
            indexSigners.isEmpty() -> MatchConfidence.MEDIUM
            else -> MatchConfidence.WARNING
        }
        if (!matches) {
            // Still surface as WARNING so the UI can explain signature mismatch.
        }
        return AppUpdateInfo(
            packageName = packageName,
            availableVersionName = latest.versionName,
            availableVersionCode = latest.versionCode,
            sourceId = latest.repoId,
            sourceLabel = repos[latest.repoId]?.name ?: latest.repoId,
            confidence = confidence,
            signatureMatches = matches,
        )
    }

    private suspend fun findGitHubUpdate(
        packageName: String,
        ownerRepo: String,
        assetRegex: String?,
        includePre: Boolean,
        local: dev.involvex.kiln.core.model.InstalledAppMatchInput,
    ): AppUpdateInfo? {
        val regex = runCatching { Regex(assetRegex ?: ".*\\.apk$") }.getOrElse { Regex(".*\\.apk$") }
        return when (
            val result = gitHubReleasesClient.latestMatchingRelease(ownerRepo, regex, includePre)
        ) {
            is Result.Failure -> {
                Timber.d("GitHub update check failed for %s", packageName)
                null
            }

            is Result.Success -> {
                val pick = result.value
                val remoteCode = parseVersionCodeFromTag(pick.tagName)
                if (!isRemoteNewer(local.versionCode, local.versionName, remoteCode, pick.tagName)) {
                    null
                } else {
                    AppUpdateInfo(
                        packageName = packageName,
                        availableVersionName = pick.tagName,
                        availableVersionCode = remoteCode,
                        sourceId = "github:$ownerRepo",
                        sourceLabel = "GitHub $ownerRepo",
                        confidence = MatchConfidence.MEDIUM,
                        signatureMatches = false,
                    )
                }
            }
        }
    }

    private fun parseVersionCodeFromTag(tag: String): Long {
        val digits = Regex("""\d+""").findAll(tag.trimStart('v', 'V')).mapNotNull { it.value.toLongOrNull() }.toList()
        if (digits.isEmpty()) return 0L
        return digits.fold(0L) { acc, n -> acc * VERSION_SEGMENT_BASE + n.coerceAtMost(VERSION_SEGMENT_MAX) }
    }

    private companion object {
        const val VERSION_SEGMENT_BASE = 1000L
        const val VERSION_SEGMENT_MAX = 999L
    }
}
