package dev.involvex.kiln.feature.sources

import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.common.fingerprintsEqual
import dev.involvex.kiln.core.database.IndexDao
import dev.involvex.kiln.core.database.ManagedAppDao
import dev.involvex.kiln.core.database.ManagedAppEntity
import dev.involvex.kiln.core.database.RepoDao
import dev.involvex.kiln.core.model.AppUpdateInfo
import dev.involvex.kiln.core.model.AppUpdateRepository
import dev.involvex.kiln.core.model.GitHubReleaseChannel
import dev.involvex.kiln.core.model.InstalledAppMatchInput
import dev.involvex.kiln.core.model.InstalledAppsReader
import dev.involvex.kiln.core.model.ManagedSourceLink
import dev.involvex.kiln.core.model.MatchConfidence
import dev.involvex.kiln.core.model.ReleaseForge
import dev.involvex.kiln.core.model.RepoConfig
import dev.involvex.kiln.core.model.SourceKind
import dev.involvex.kiln.core.model.UpdateCheckProgress
import dev.involvex.kiln.core.model.UpdateCheckResult
import dev.involvex.kiln.core.model.WatchlistEntry
import dev.involvex.kiln.core.model.WatchlistRepository
import dev.involvex.kiln.core.model.displayLabel
import dev.involvex.kiln.core.model.parseGitHubReleaseChannel
import dev.involvex.kiln.core.model.parseReleaseForge
import dev.involvex.kiln.core.model.sourceId
import dev.involvex.kiln.feature.sources.codeberg.CodebergReleasesClient
import dev.involvex.kiln.feature.sources.fdroid.FDroidRepoSync
import dev.involvex.kiln.feature.sources.github.GitHubActionsClient
import dev.involvex.kiln.feature.sources.github.GitHubReleasePick
import dev.involvex.kiln.feature.sources.github.GitHubReleasesClient
import dev.involvex.kiln.feature.sources.github.GitHubTokenStore
import dev.involvex.kiln.feature.sources.github.GitHubVersionCode
import dev.involvex.kiln.feature.sources.gitlab.GitLabReleasesClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import timber.log.Timber

@Singleton
@Suppress("TooManyFunctions") // Update badges, GitHub/F-Droid matching, and watchlist share one repo.
class DefaultAppUpdateRepository @Inject constructor(
    private val managedAppDao: ManagedAppDao,
    private val indexDao: IndexDao,
    private val repoDao: RepoDao,
    private val installedAppsReader: InstalledAppsReader,
    private val gitHubReleasesClient: GitHubReleasesClient,
    private val gitLabReleasesClient: GitLabReleasesClient,
    private val codebergReleasesClient: CodebergReleasesClient,
    private val gitHubActionsClient: GitHubActionsClient,
    private val gitHubTokenStore: GitHubTokenStore,
    private val fdroidRepoSync: FDroidRepoSync,
) : AppUpdateRepository,
    WatchlistRepository {
    private val updatesState = MutableStateFlow<Map<String, AppUpdateInfo>>(emptyMap())

    override fun updates(): Flow<Map<String, AppUpdateInfo>> = updatesState.asStateFlow()

    override suspend fun refreshUpdates() {
        recomputeUpdates(includeGitHub = false)
    }

    private suspend fun recomputeUpdates(includeGitHub: Boolean) {
        val installed = installedAppsReader.userApps().associateBy { it.packageName }
        val repos = repoDao.getAll().associateBy { it.id }
        val out = linkedMapOf<String, AppUpdateInfo>()
        for (app in managedAppDao.getAll().filter { !it.ignoreUpdates }) {
            coroutineContext.ensureActive()
            val local = installed[app.packageName]
            if (local != null) {
                if (app.watchOnly) {
                    managedAppDao.upsert(app.copy(watchOnly = false))
                }
                putBestUpdate(out, app, local, repos, watchOnly = false, includeGitHub = includeGitHub)
            } else if (app.watchOnly) {
                val synthetic = InstalledAppMatchInput(
                    packageName = app.packageName,
                    label = app.packageName,
                    versionName = null,
                    versionCode = 0L,
                    signerSha256 = emptyList(),
                )
                putBestUpdate(out, app, synthetic, repos, watchOnly = true, includeGitHub = includeGitHub)
            }
        }
        updatesState.value = out
    }

    private suspend fun putBestUpdate(
        out: MutableMap<String, AppUpdateInfo>,
        app: ManagedAppEntity,
        local: InstalledAppMatchInput,
        repos: Map<String, dev.involvex.kiln.core.database.RepoEntity>,
        watchOnly: Boolean,
        includeGitHub: Boolean,
    ) {
        val fromIndex = findFdroidUpdate(
            app.packageName,
            app.sourceId,
            local.versionCode,
            local.versionName,
            local.signerSha256,
            repos,
            watchOnly = watchOnly,
        )
        val ownerRepo = app.githubOwnerRepo
        val fromGitHub = if (includeGitHub && ownerRepo != null) {
            findGitHubUpdate(
                packageName = app.packageName,
                ownerRepo = ownerRepo,
                assetRegex = app.githubAssetRegex,
                channel = parseGitHubReleaseChannel(app.githubChannel),
                forge = parseReleaseForge(app.releaseForge),
                local = local,
                watchOnly = watchOnly,
            )
        } else {
            null
        }
        val best = listOfNotNull(fromIndex, fromGitHub)
            .filter { isInstallableUpdateCandidate(it) }
            .maxWithOrNull(
                compareBy<AppUpdateInfo> { it.signatureMatches }
                    .thenBy { it.availableVersionCode },
            )
        if (best != null) {
            out[app.packageName] = best
        }
    }

    override suspend fun checkForUpdates(onProgress: suspend (UpdateCheckProgress) -> Unit): Result<UpdateCheckResult> {
        val repos = repoDao.getAll().filter { it.enabled }
        val (ok, fail) = EnabledRepoSync.syncAll(
            repos = repos,
            onProgress = onProgress,
            syncOne = { repo -> fdroidRepoSync.sync(repo) },
        )
        coroutineContext.ensureActive()
        onProgress(
            UpdateCheckProgress(
                current = repos.size.coerceAtLeast(1),
                total = repos.size.coerceAtLeast(1),
                label = "github",
            ),
        )
        recomputeUpdates(includeGitHub = true)
        return Result.success(
            UpdateCheckResult(
                reposSynced = ok,
                reposFailed = fail,
                updatesFound = updatesState.value.size,
            ),
        )
    }

    override fun observeIgnoreUpdates(packageName: String): Flow<Boolean?> = managedAppDao.observe(packageName).map { it?.ignoreUpdates }

    override fun observeUpdateCooldownDays(packageName: String): Flow<Int?> =
        managedAppDao.observe(packageName).map { it?.updateCooldownDays }

    override fun observeIgnoredUpdatePackages(): Flow<List<String>> = managedAppDao.observeIgnoredPackageNames()

    override suspend fun cooldownOverrides(): Map<String, Int?> =
        managedAppDao.getAll().associate { it.packageName to it.updateCooldownDays }

    override fun observeManagedLink(packageName: String): Flow<ManagedSourceLink?> = managedAppDao.observe(packageName).map { entity ->
        entity?.let {
            ManagedSourceLink(
                sourceId = it.sourceId,
                confidence = runCatching { MatchConfidence.valueOf(it.confidence) }
                    .getOrDefault(MatchConfidence.NONE),
                pinned = it.pinned,
                githubOwnerRepo = it.githubOwnerRepo,
                githubChannel = parseGitHubReleaseChannel(it.githubChannel),
                releaseForge = parseReleaseForge(it.releaseForge),
            )
        }
    }

    override fun observeEnabledRepos(): Flow<List<RepoConfig>> = repoDao.observeAll().map { repos ->
        repos.filter { it.enabled }
            .sortedBy { it.priority }
            .map {
                RepoConfig(
                    id = it.id,
                    name = it.name,
                    address = it.address,
                    fingerprint = it.fingerprint,
                    kind = runCatching { SourceKind.valueOf(it.kind) }.getOrDefault(SourceKind.FDROID),
                    enabled = it.enabled,
                    lastSyncedAtEpochMs = it.lastSyncedAtEpochMs,
                )
            }
    }

    override fun observeSavedGithubOwnerRepos(): Flow<List<String>> = managedAppDao.observeAll().map { apps ->
        apps.mapNotNull { it.githubOwnerRepo?.takeIf { repo -> repo.contains('/') } }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
    }

    override fun observeIsWatching(packageName: String): Flow<Boolean> = managedAppDao.observe(packageName).map { it?.watchOnly == true }

    override fun observeWatchlist(): Flow<List<WatchlistEntry>> = channelFlow {
        combine(managedAppDao.observeAll(), updatesState) { apps, updates -> apps to updates }
            .collect { (apps, updates) ->
                send(buildWatchlistEntries(apps, updates))
            }
    }

    private suspend fun buildWatchlistEntries(apps: List<ManagedAppEntity>, updates: Map<String, AppUpdateInfo>): List<WatchlistEntry> =
        apps.filter { it.watchOnly }
            .sortedBy { it.packageName.lowercase() }
            .map { app ->
                val update = updates[app.packageName]
                val displayName = indexDao.getPackage(app.packageName, app.sourceId)?.name
                    ?: app.githubOwnerRepo
                    ?: app.packageName
                WatchlistEntry(
                    packageName = app.packageName,
                    displayName = displayName,
                    sourceId = app.sourceId,
                    githubOwnerRepo = app.githubOwnerRepo,
                    availableVersionName = update?.availableVersionName,
                    hasUpdate = update != null,
                    releaseNotes = update?.releaseNotes,
                )
            }

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
                githubOwnerRepo = ownerRepoFromSourceId(update?.sourceId),
                githubAssetRegex = ".*\\.apk$",
                githubChannel = GitHubReleaseChannel.STABLE.name,
                releaseForge = forgeFromSourceId(update?.sourceId).name,
                watchOnly = false,
                updateCooldownDays = null,
            )
        managedAppDao.upsert(entity)
        refreshUpdates()
        return Result.success(Unit)
    }

    override suspend fun setUpdateCooldownDays(packageName: String, days: Int?): Result<Unit> {
        val sanitized = days?.coerceIn(0, MAX_PER_APP_COOLDOWN_DAYS)
        val existing = managedAppDao.get(packageName)
        val update = updatesState.value[packageName]
        val entity = existing?.copy(updateCooldownDays = sanitized)
            ?: ManagedAppEntity(
                packageName = packageName,
                sourceId = update?.sourceId ?: "manual",
                confidence = (update?.confidence ?: MatchConfidence.NONE).name,
                pinned = false,
                ignoreUpdates = false,
                githubOwnerRepo = ownerRepoFromSourceId(update?.sourceId),
                githubAssetRegex = ".*\\.apk$",
                githubChannel = GitHubReleaseChannel.STABLE.name,
                releaseForge = forgeFromSourceId(update?.sourceId).name,
                watchOnly = false,
                updateCooldownDays = sanitized,
            )
        managedAppDao.upsert(entity)
        return Result.success(Unit)
    }

    override suspend fun linkManagedSource(
        packageName: String,
        sourceId: String,
        confidence: MatchConfidence,
        githubOwnerRepo: String?,
        githubAssetRegex: String?,
        pinned: Boolean,
        githubChannel: GitHubReleaseChannel?,
        releaseForge: ReleaseForge,
    ): Result<Unit> {
        val existing = managedAppDao.get(packageName)
        val channel = githubChannel ?: parseGitHubReleaseChannel(existing?.githubChannel)
        val forge = if (githubOwnerRepo.isNullOrBlank()) {
            ReleaseForge.GITHUB
        } else {
            releaseForge
        }
        if (channel == GitHubReleaseChannel.NIGHTLY && forge != ReleaseForge.GITHUB) {
            return Result.failure("forge_nightly_unsupported")
        }
        managedAppDao.upsert(
            ManagedAppEntity(
                packageName = packageName,
                sourceId = sourceId.ifBlank { "manual" },
                confidence = confidence.name,
                pinned = pinned,
                ignoreUpdates = existing?.ignoreUpdates ?: false,
                githubOwnerRepo = githubOwnerRepo,
                githubAssetRegex = githubAssetRegex ?: existing?.githubAssetRegex ?: ".*\\.apk$",
                githubChannel = channel.name,
                releaseForge = forge.name,
                watchOnly = false,
            ),
        )
        // GitHub badges need a live probe; local index refresh alone leaves them empty.
        if (!githubOwnerRepo.isNullOrBlank()) {
            recomputeUpdates(includeGitHub = true)
        } else {
            refreshUpdates()
        }
        return Result.success(Unit)
    }

    override suspend fun unlinkManagedPackage(packageName: String): Result<Unit> {
        managedAppDao.delete(packageName)
        refreshUpdates()
        return Result.success(Unit)
    }

    override suspend fun setGitHubChannel(packageName: String, channel: GitHubReleaseChannel): Result<Unit> {
        val existing = managedAppDao.get(packageName)
            ?: return Result.failure("not_managed")
        if (existing.githubOwnerRepo.isNullOrBlank()) {
            return Result.failure("not_github_source")
        }
        val forge = parseReleaseForge(existing.releaseForge)
        if (channel == GitHubReleaseChannel.NIGHTLY && forge != ReleaseForge.GITHUB) {
            return Result.failure("forge_nightly_unsupported")
        }
        managedAppDao.upsert(existing.copy(githubChannel = channel.name))
        refreshUpdates()
        return Result.success(Unit)
    }

    private fun ownerRepoFromSourceId(sourceId: String?): String? {
        val id = sourceId ?: return null
        return when {
            id.startsWith("github:") -> id.removePrefix("github:")
            id.startsWith("gitlab:") -> id.removePrefix("gitlab:")
            id.startsWith("codeberg:") -> id.removePrefix("codeberg:")
            else -> null
        }?.takeIf { it.contains('/') }
    }

    private fun forgeFromSourceId(sourceId: String?): ReleaseForge = when {
        sourceId?.startsWith("gitlab:") == true -> ReleaseForge.GITLAB
        sourceId?.startsWith("codeberg:") == true -> ReleaseForge.CODEBERG
        else -> ReleaseForge.GITHUB
    }

    override suspend fun watchCatalogPackage(packageName: String, repoId: String): Result<Unit> {
        val existing = managedAppDao.get(packageName)
        if (existing != null && !existing.watchOnly) {
            return Result.failure("already_managed")
        }
        managedAppDao.upsert(
            ManagedAppEntity(
                packageName = packageName,
                sourceId = repoId,
                confidence = MatchConfidence.MEDIUM.name,
                pinned = true,
                ignoreUpdates = false,
                githubOwnerRepo = null,
                githubAssetRegex = null,
                githubChannel = GitHubReleaseChannel.STABLE.name,
                watchOnly = true,
            ),
        )
        refreshUpdates()
        return Result.success(Unit)
    }

    override suspend fun unwatchPackage(packageName: String): Result<Unit> {
        val existing = managedAppDao.get(packageName) ?: return Result.success(Unit)
        if (!existing.watchOnly) {
            return Result.failure("not_watching")
        }
        managedAppDao.delete(packageName)
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
        watchOnly: Boolean,
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
        val (matches, confidence) = fdroidMatch(watchOnly, indexSigners, installedSigners)
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
            releaseNotes = ReleaseNotesText.normalize(latest.whatsNew),
            watchOnly = watchOnly,
        )
    }

    private fun fdroidMatch(
        watchOnly: Boolean,
        indexSigners: List<String>,
        installedSigners: List<String>,
    ): Pair<Boolean, MatchConfidence> {
        if (watchOnly) return true to MatchConfidence.MEDIUM
        val matches = indexSigners.isEmpty() || installedSigners.any { inst ->
            indexSigners.any { fingerprintsEqual(inst, it) }
        }
        val confidence = when {
            matches && indexSigners.isNotEmpty() -> MatchConfidence.HIGH
            indexSigners.isEmpty() -> MatchConfidence.MEDIUM
            else -> MatchConfidence.WARNING
        }
        return matches to confidence
    }

    private suspend fun findGitHubUpdate(
        packageName: String,
        ownerRepo: String,
        assetRegex: String?,
        channel: GitHubReleaseChannel,
        forge: ReleaseForge,
        local: InstalledAppMatchInput,
        watchOnly: Boolean,
    ): AppUpdateInfo? {
        val regex = runCatching { Regex(assetRegex ?: ".*\\.apk$") }.getOrElse { Regex(".*\\.apk$") }
        return when (channel) {
            GitHubReleaseChannel.NIGHTLY -> {
                if (forge != ReleaseForge.GITHUB) {
                    Timber.d("Nightly skipped for non-GitHub forge %s (%s)", forge.name, packageName)
                    null
                } else {
                    findGitHubNightlyUpdate(packageName, ownerRepo, regex, local, watchOnly)
                }
            }

            GitHubReleaseChannel.STABLE,
            GitHubReleaseChannel.PRE_RELEASE,
            -> findGitHubReleaseUpdate(packageName, ownerRepo, regex, channel, forge, local, watchOnly)
        }
    }

    private suspend fun findGitHubReleaseUpdate(
        packageName: String,
        ownerRepo: String,
        regex: Regex,
        channel: GitHubReleaseChannel,
        forge: ReleaseForge,
        local: InstalledAppMatchInput,
        watchOnly: Boolean,
    ): AppUpdateInfo? = when (
        val result = latestMatchingRelease(forge, ownerRepo, regex, channel)
    ) {
        is Result.Failure -> {
            Timber.d("%s update check failed for %s", forge.displayLabel(), packageName)
            null
        }

        is Result.Success -> {
            val pick = result.value
            val remoteCode = GitHubVersionCode.parse(pick.tagName)
            if (!isGitHubRemoteNewer(local.versionCode, local.versionName, remoteCode, pick.tagName)) {
                null
            } else {
                AppUpdateInfo(
                    packageName = packageName,
                    availableVersionName = pick.tagName,
                    availableVersionCode = remoteCode,
                    sourceId = forge.sourceId(ownerRepo),
                    sourceLabel = "${forge.displayLabel()} $ownerRepo",
                    confidence = MatchConfidence.MEDIUM,
                    signatureMatches = false,
                    downloadUrl = pick.downloadUrl,
                    apkSha256 = null,
                    sizeBytes = pick.sizeBytes,
                    releaseNotes = pick.releaseNotes,
                    watchOnly = watchOnly,
                )
            }
        }
    }

    private suspend fun latestMatchingRelease(
        forge: ReleaseForge,
        ownerRepo: String,
        regex: Regex,
        channel: GitHubReleaseChannel,
    ): Result<GitHubReleasePick> = when (forge) {
        ReleaseForge.GITHUB -> gitHubReleasesClient.latestMatchingRelease(ownerRepo, regex, channel)
        ReleaseForge.GITLAB -> gitLabReleasesClient.latestMatchingRelease(ownerRepo, regex, channel)
        ReleaseForge.CODEBERG -> codebergReleasesClient.latestMatchingRelease(ownerRepo, regex, channel)
    }

    private suspend fun findGitHubNightlyUpdate(
        packageName: String,
        ownerRepo: String,
        apkRegex: Regex,
        local: InstalledAppMatchInput,
        watchOnly: Boolean,
    ): AppUpdateInfo? {
        if (gitHubTokenStore.getToken().isNullOrBlank()) {
            Timber.d("GitHub nightly skipped for %s: PAT required", packageName)
            return null
        }
        return when (val result = gitHubActionsClient.latestMatchingArtifact(ownerRepo)) {
            is Result.Failure -> {
                Timber.d("GitHub Actions check failed for %s: %s", packageName, result.message)
                null
            }

            is Result.Success -> {
                val pick = result.value
                val remoteCode = if (pick.runNumber > 0) pick.runNumber else GitHubVersionCode.parse(pick.versionLabel)
                if (!isGitHubRemoteNewer(local.versionCode, local.versionName, remoteCode, pick.versionLabel)) {
                    null
                } else {
                    AppUpdateInfo(
                        packageName = packageName,
                        availableVersionName = pick.versionLabel,
                        availableVersionCode = remoteCode,
                        sourceId = "github:$ownerRepo",
                        sourceLabel = "GitHub $ownerRepo (nightly)",
                        confidence = MatchConfidence.MEDIUM,
                        signatureMatches = false,
                        downloadUrl = pick.downloadUrl,
                        apkSha256 = null,
                        sizeBytes = pick.sizeBytes,
                        requiresGitHubAuth = true,
                        apkAssetRegex = apkRegex.pattern,
                        watchOnly = watchOnly,
                    )
                }
            }
        }
    }

    companion object {
        private const val MAX_PER_APP_COOLDOWN_DAYS = 30

        /**
         * Matching certs, known WARNUNG mismatches, and unknown-signer rows with a download URL
         * (GitHub MEDIUM) are kept. Empty MEDIUM without a URL is dropped.
         */
        fun isInstallableUpdateCandidate(info: AppUpdateInfo): Boolean {
            if (info.signatureMatches) return true
            if (info.confidence == MatchConfidence.WARNING) return true
            return info.confidence == MatchConfidence.MEDIUM && !info.downloadUrl.isNullOrBlank()
        }
    }
}
