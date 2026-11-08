package dev.involvex.kiln.feature.sources

import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.common.fingerprintsEqual
import dev.involvex.kiln.core.database.IndexDao
import dev.involvex.kiln.core.database.ManagedAppDao
import dev.involvex.kiln.core.database.ManagedAppEntity
import dev.involvex.kiln.core.database.RepoDao
import dev.involvex.kiln.core.model.AppUpdateInfo
import dev.involvex.kiln.core.model.AppUpdateRepository
import dev.involvex.kiln.core.model.InstalledAppsReader
import dev.involvex.kiln.core.model.MatchConfidence
import dev.involvex.kiln.core.model.UpdateCheckResult
import dev.involvex.kiln.feature.sources.fdroid.FDroidRepoSync
import dev.involvex.kiln.feature.sources.github.GitHubReleasesClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import timber.log.Timber

@Singleton
class DefaultAppUpdateRepository @Inject constructor(
    private val managedAppDao: ManagedAppDao,
    private val indexDao: IndexDao,
    private val repoDao: RepoDao,
    private val installedAppsReader: InstalledAppsReader,
    private val gitHubReleasesClient: GitHubReleasesClient,
    private val fdroidRepoSync: FDroidRepoSync,
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

    override suspend fun checkForUpdates(): Result<UpdateCheckResult> {
        val repos = repoDao.getAll().filter { it.enabled }
        var ok = 0
        var fail = 0
        for (repo in repos) {
            when (fdroidRepoSync.sync(repo)) {
                is Result.Success -> ok++
                is Result.Failure -> fail++
            }
        }
        refreshUpdates()
        return Result.success(
            UpdateCheckResult(
                reposSynced = ok,
                reposFailed = fail,
                updatesFound = updatesState.value.size,
            ),
        )
    }

    override fun observeIgnoreUpdates(packageName: String): Flow<Boolean?> = managedAppDao.observe(packageName).map { it?.ignoreUpdates }

    override suspend fun setIgnoreUpdates(packageName: String, ignoreUpdates: Boolean): Result<Unit> {
        val existing = managedAppDao.get(packageName)
        val update = updatesState.value[packageName]
        val entity = existing?.copy(ignoreUpdates = ignoreUpdates)
            ?: ManagedAppEntity(
                packageName = packageName,
                sourceId = update?.sourceId ?: "manual",
                confidence = (update?.confidence ?: MatchConfidence.NONE).name,
                pinned = false,
                ignoreUpdates = ignoreUpdates,
                githubOwnerRepo = update?.sourceId
                    ?.takeIf { it.startsWith("github:") }
                    ?.removePrefix("github:"),
                githubAssetRegex = ".*\\.apk$",
                includePreReleases = false,
            )
        managedAppDao.upsert(entity)
        refreshUpdates()
        return Result.success(Unit)
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
        val repoAddress = repos[latest.repoId]?.address
        val downloadUrl = repoAddress?.let { FdroidApkUrl.build(it, latest.apkName) }
        return AppUpdateInfo(
            packageName = packageName,
            availableVersionName = latest.versionName,
            availableVersionCode = latest.versionCode,
            sourceId = latest.repoId,
            sourceLabel = repos[latest.repoId]?.name ?: latest.repoId,
            confidence = confidence,
            signatureMatches = matches,
            downloadUrl = downloadUrl,
            apkSha256 = latest.apkSha256,
            sizeBytes = latest.sizeBytes,
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
                        downloadUrl = pick.downloadUrl,
                        apkSha256 = null,
                        sizeBytes = pick.sizeBytes,
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
