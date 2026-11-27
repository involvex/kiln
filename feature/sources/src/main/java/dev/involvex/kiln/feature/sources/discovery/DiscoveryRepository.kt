package dev.involvex.kiln.feature.sources.discovery

import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.database.IndexDao
import dev.involvex.kiln.core.database.IndexedPackageEntity
import dev.involvex.kiln.core.database.ManagedAppDao
import dev.involvex.kiln.core.database.ManagedAppEntity
import dev.involvex.kiln.core.database.RepoDao
import dev.involvex.kiln.core.database.RepoEntity
import dev.involvex.kiln.core.model.AppUpdateInfo
import dev.involvex.kiln.core.model.GitHubReleaseChannel
import dev.involvex.kiln.core.model.InstallQueue
import dev.involvex.kiln.core.model.MatchConfidence
import dev.involvex.kiln.core.model.ReleaseForge
import dev.involvex.kiln.core.model.SourceKind
import dev.involvex.kiln.core.model.displayLabel
import dev.involvex.kiln.core.model.parseGitHubReleaseChannel
import dev.involvex.kiln.core.model.parseReleaseForge
import dev.involvex.kiln.core.model.sourceId
import dev.involvex.kiln.feature.sources.FdroidApkUrl
import dev.involvex.kiln.feature.sources.FdroidMediaUrl
import dev.involvex.kiln.feature.sources.ReleaseNotesText
import dev.involvex.kiln.feature.sources.codeberg.CodebergReleasesClient
import dev.involvex.kiln.feature.sources.fdroid.FDroidRepoSync
import dev.involvex.kiln.feature.sources.forge.ForgeRef
import dev.involvex.kiln.feature.sources.forge.ForgeUrlParse
import dev.involvex.kiln.feature.sources.github.GitHubReleaseDto
import dev.involvex.kiln.feature.sources.github.GitHubReleaseMatch
import dev.involvex.kiln.feature.sources.github.GitHubReleasesClient
import dev.involvex.kiln.feature.sources.github.GitHubVersionCode
import dev.involvex.kiln.feature.sources.gitlab.GitLabReleasesClient
import javax.inject.Inject
import javax.inject.Singleton

data class DiscoveryAppRow(
    val packageName: String,
    val name: String,
    val summary: String?,
    val repoId: String,
    val repoName: String,
    val repoAddress: String = "",
    val categories: List<String>,
    val antiFeatures: List<String> = emptyList(),
    val versionName: String?,
    val versionCode: Long?,
    val iconUrl: String? = null,
    val sourceCodeUrl: String? = null,
    val phoneScreenshotUrls: List<String> = emptyList(),
)

@Singleton
class DiscoveryRepository @Inject constructor(
    private val indexDao: IndexDao,
    private val repoDao: RepoDao,
    private val managedAppDao: ManagedAppDao,
    private val sync: FDroidRepoSync,
    private val installQueue: InstallQueue,
    private val gitHubReleasesClient: GitHubReleasesClient,
    private val gitLabReleasesClient: GitLabReleasesClient,
    private val codebergReleasesClient: CodebergReleasesClient,
) {
    suspend fun packageCount(): Int = indexDao.packageCount()

    suspend fun categories(): List<String> {
        val tokens = linkedSetOf<String>()
        for (csv in indexDao.categoryCsvRows()) {
            csv.split(',').map { it.trim() }.filter { it.isNotEmpty() }.forEach { tokens += it }
        }
        return tokens.sorted()
    }

    suspend fun search(query: String, category: String, limit: Int = 200): List<DiscoveryAppRow> {
        val repos = repoDao.getAll().associateBy { it.id }
        return indexDao.searchPackages(query.trim(), category.trim(), limit).map { entity ->
            entity.toDiscoveryRow(repos[entity.repoId])
        }
    }

    suspend fun catalogRow(packageName: String, repoId: String): DiscoveryAppRow? {
        val entity = indexDao.getPackage(packageName, repoId) ?: return null
        return entity.toDiscoveryRow(repoDao.getById(repoId))
    }

    suspend fun refreshCatalog(): Result<Int> {
        val fdroidRepos = repoDao.getAll().filter { it.enabled && it.kind == SourceKind.FDROID.name }
        if (fdroidRepos.isEmpty()) return Result.failure("no_fdroid_repos")
        var total = 0
        var lastError: String? = null
        for (repo in fdroidRepos) {
            when (val result = sync.syncCatalog(repo)) {
                is Result.Success -> total += result.value
                is Result.Failure -> lastError = "${repo.id}:${result.message}"
            }
        }
        return if (total > 0 || lastError == null) {
            Result.success(total)
        } else {
            Result.failure(lastError)
        }
    }

    suspend fun install(row: DiscoveryAppRow): Result<String> {
        val options = when (val loaded = loadFdroidInstallOptions(row.packageName, row.repoId)) {
            is Result.Failure -> return loaded
            is Result.Success -> loaded.value
        }
        val deviceAbis = android.os.Build.SUPPORTED_ABIS.toList()
        val group = options.versions.firstOrNull() ?: return Result.failure("release_missing")
        val variant = group.variantForAbi(InstallVariantSelection.ABI_AUTO, deviceAbis)
            ?: return Result.failure("release_missing")
        return installFdroidVariant(
            packageName = row.packageName,
            repoId = row.repoId,
            versionCode = group.versionCode,
            versionName = group.versionName,
            apkName = variant.apkName,
            apkSha256 = variant.apkSha256,
            sizeBytes = variant.sizeBytes,
            releaseNotes = group.whatsNew,
        )
    }

    suspend fun loadFdroidInstallOptions(packageName: String, repoId: String): Result<FdroidInstallOptions> {
        val repo = repoDao.getById(repoId) ?: return Result.failure("repo_missing")
        val releases = indexDao.releasesFor(packageName, repoId)
        if (releases.isEmpty()) return Result.failure("release_missing")
        val byCode = linkedMapOf<Long, VersionGroup>()
        for (release in releases) {
            InstallVariantSelection.groupByVersionCode(
                versionCode = release.versionCode,
                versionName = release.versionName,
                nativeCodes = InstallVariantSelection.parseNativeCodesCsv(release.nativeCodesCsv),
                existing = byCode,
                apkName = release.apkName,
                apkSha256 = release.apkSha256,
                sizeBytes = release.sizeBytes,
                whatsNew = ReleaseNotesText.normalize(release.whatsNew),
            )
        }
        val versions = byCode.values.sortedByDescending { it.versionCode }
        return Result.success(
            FdroidInstallOptions(
                packageName = packageName,
                repoId = repo.id,
                repoName = repo.name,
                repoAddress = repo.address,
                versions = versions,
            ),
        )
    }

    suspend fun installFdroidVariant(
        packageName: String,
        repoId: String,
        versionCode: Long,
        versionName: String,
        apkName: String,
        apkSha256: String?,
        sizeBytes: Long?,
        releaseNotes: String?,
    ): Result<String> {
        val repo = repoDao.getById(repoId) ?: return Result.failure("repo_missing")
        val url = FdroidApkUrl.build(repo.address, apkName)
        val update = AppUpdateInfo(
            packageName = packageName,
            availableVersionName = versionName,
            availableVersionCode = versionCode,
            sourceId = repo.id,
            sourceLabel = repo.name,
            confidence = MatchConfidence.MEDIUM,
            signatureMatches = true,
            downloadUrl = url,
            apkSha256 = apkSha256,
            sizeBytes = sizeBytes,
            releaseNotes = releaseNotes,
        )
        clearWatchOnlyIfPresent(packageName)
        return installQueue.enqueue(update)
    }

    suspend fun savedGithubSources(): List<DiscoveryGithubRow> {
        return managedAppDao.getAll()
            .mapNotNull { app ->
                val ownerRepo = app.githubOwnerRepo?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val forge = parseReleaseForge(app.releaseForge)
                DiscoveryGithubRow(
                    ownerRepo = ownerRepo,
                    packageName = app.packageName,
                    displayName = "${forge.displayLabel()} $ownerRepo",
                    tagName = null,
                    assetName = null,
                    sizeBytes = null,
                    downloadUrl = null,
                    saved = true,
                    forge = forge,
                )
            }
            .sortedBy { it.displayName.lowercase() }
    }

    suspend fun lookupGithub(raw: String): Result<DiscoveryGithubRow> {
        val ref = ForgeUrlParse.parse(raw) ?: return Result.failure("github_query_invalid")
        val existing = findManaged(ref)
        val packageName = existing?.packageName ?: ref.provisionalPackageName()
        val regex = runCatching {
            Regex(existing?.githubAssetRegex ?: DEFAULT_APK_REGEX)
        }.getOrElse { Regex(DEFAULT_APK_REGEX) }
        val channel = parseGitHubReleaseChannel(existing?.githubChannel)
        if (channel == GitHubReleaseChannel.NIGHTLY) {
            return if (ref.forge == ReleaseForge.GITHUB) {
                Result.failure("github_nightly_use_library")
            } else {
                Result.failure("forge_nightly_unsupported")
            }
        }
        // New Explore rows: fall back to PRE_RELEASE when the repo only ships prereleases.
        // Existing STABLE stays strict so App Detail channel chips remain authoritative.
        val fallbackToPreRelease = existing == null && channel == GitHubReleaseChannel.STABLE
        return when (
            val result = matchLatestRelease(
                forge = ref.forge,
                ownerRepo = ref.ownerRepo,
                assetRegex = regex,
                channel = channel,
                fallbackToPreRelease = fallbackToPreRelease,
            )
        ) {
            is Result.Failure -> result

            is Result.Success -> {
                val match = result.value
                val pick = match.pick
                if (existing == null) {
                    managedAppDao.upsert(
                        ManagedAppEntity(
                            packageName = packageName,
                            sourceId = ref.sourceId,
                            confidence = MatchConfidence.MEDIUM.name,
                            pinned = true,
                            ignoreUpdates = false,
                            githubOwnerRepo = ref.ownerRepo,
                            githubAssetRegex = DEFAULT_APK_REGEX,
                            githubChannel = match.channel.name,
                            releaseForge = ref.forge.name,
                            watchOnly = true,
                        ),
                    )
                }
                Result.success(
                    DiscoveryGithubRow(
                        ownerRepo = ref.ownerRepo,
                        packageName = packageName,
                        displayName = ref.displayLabel,
                        tagName = pick.tagName,
                        assetName = pick.assetName,
                        sizeBytes = pick.sizeBytes,
                        downloadUrl = pick.downloadUrl,
                        saved = true,
                        releaseNotes = pick.releaseNotes,
                        forge = ref.forge,
                    ),
                )
            }
        }
    }

    suspend fun installGithub(row: DiscoveryGithubRow): Result<String> {
        val url = row.downloadUrl ?: return Result.failure("github_release_missing")
        val tag = row.tagName ?: return Result.failure("github_release_missing")
        return installGithubVariant(
            packageName = row.packageName,
            ownerRepo = row.ownerRepo,
            tagName = tag,
            downloadUrl = url,
            sizeBytes = row.sizeBytes,
            releaseNotes = row.releaseNotes,
            forge = row.forge,
        )
    }

    suspend fun loadGithubInstallOptions(ownerRepoRaw: String): Result<GithubInstallOptions> {
        val ref = ForgeUrlParse.parse(ownerRepoRaw) ?: return Result.failure("github_query_invalid")
        val existing = findManaged(ref)
        val packageName = existing?.packageName ?: ref.provisionalPackageName()
        val regex = runCatching {
            Regex(existing?.githubAssetRegex ?: DEFAULT_APK_REGEX)
        }.getOrElse { Regex(DEFAULT_APK_REGEX) }
        nightlyBlocked(parseGitHubReleaseChannel(existing?.githubChannel), ref.forge)?.let {
            return Result.failure(it)
        }
        return when (val listed = listReleases(ref.forge, ref.ownerRepo)) {
            is Result.Failure -> listed
            is Result.Success -> buildGithubInstallOptions(ref, existing, packageName, regex, listed.value)
        }
    }

    private fun nightlyBlocked(channel: GitHubReleaseChannel, forge: ReleaseForge): String? {
        if (channel != GitHubReleaseChannel.NIGHTLY) return null
        return if (forge == ReleaseForge.GITHUB) {
            "github_nightly_use_library"
        } else {
            "forge_nightly_unsupported"
        }
    }

    private suspend fun buildGithubInstallOptions(
        ref: ForgeRef,
        existing: ManagedAppEntity?,
        packageName: String,
        regex: Regex,
        releases: List<GitHubReleaseDto>,
    ): Result<GithubInstallOptions> {
        val apkReleases = releases.mapNotNull { release ->
            val apkAssets = release.assets
                .filter { regex.containsMatchIn(it.name) }
                .map {
                    GithubApkAsset(
                        name = it.name,
                        downloadUrl = it.browserDownloadUrl,
                        sizeBytes = it.size,
                    )
                }
            if (apkAssets.isEmpty()) return@mapNotNull null
            GithubReleaseVariantRow(
                tagName = release.tagName,
                releaseName = release.name,
                prerelease = release.prerelease,
                nightly = InstallVariantSelection.isNightlyLabel(release.tagName, release.name),
                latestStable = false,
                assets = apkAssets,
                releaseNotes = ReleaseNotesText.normalize(release.body),
            )
        }
        val firstStableTag = apkReleases.firstOrNull { !it.prerelease && !it.nightly }?.tagName
        val rows = apkReleases.map { row -> row.copy(latestStable = row.tagName == firstStableTag) }
        if (rows.isEmpty()) {
            return Result.failure("no_matching_github_asset")
        }
        if (existing == null) {
            val initialChannel =
                if (rows.any { !it.prerelease }) {
                    GitHubReleaseChannel.STABLE
                } else {
                    GitHubReleaseChannel.PRE_RELEASE
                }
            managedAppDao.upsert(
                ManagedAppEntity(
                    packageName = packageName,
                    sourceId = ref.sourceId,
                    confidence = MatchConfidence.MEDIUM.name,
                    pinned = true,
                    ignoreUpdates = false,
                    githubOwnerRepo = ref.ownerRepo,
                    githubAssetRegex = DEFAULT_APK_REGEX,
                    githubChannel = initialChannel.name,
                    releaseForge = ref.forge.name,
                    watchOnly = true,
                ),
            )
        }
        return Result.success(
            GithubInstallOptions(
                ownerRepo = ref.ownerRepo,
                packageName = packageName,
                releases = rows,
                forge = ref.forge,
            ),
        )
    }

    suspend fun installGithubVariant(
        packageName: String,
        ownerRepo: String,
        tagName: String,
        downloadUrl: String,
        sizeBytes: Long?,
        releaseNotes: String?,
        forge: ReleaseForge = ReleaseForge.GITHUB,
    ): Result<String> {
        val update = AppUpdateInfo(
            packageName = packageName,
            availableVersionName = tagName,
            availableVersionCode = GitHubVersionCode.parse(tagName),
            sourceId = forge.sourceId(ownerRepo),
            sourceLabel = "${forge.displayLabel()} $ownerRepo",
            confidence = MatchConfidence.MEDIUM,
            signatureMatches = false,
            downloadUrl = downloadUrl,
            apkSha256 = null,
            sizeBytes = sizeBytes,
            releaseNotes = releaseNotes,
        )
        clearWatchOnlyIfPresent(packageName)
        return installQueue.enqueue(update)
    }

    private suspend fun clearWatchOnlyIfPresent(packageName: String) {
        val existing = managedAppDao.get(packageName) ?: return
        if (existing.watchOnly) {
            managedAppDao.upsert(existing.copy(watchOnly = false))
        }
    }

    private suspend fun findManaged(ref: ForgeRef): ManagedAppEntity? = managedAppDao.getAll().firstOrNull { app ->
        app.githubOwnerRepo.equals(ref.ownerRepo, ignoreCase = true) &&
            parseReleaseForge(app.releaseForge) == ref.forge
    }

    private suspend fun matchLatestRelease(
        forge: ReleaseForge,
        ownerRepo: String,
        assetRegex: Regex,
        channel: GitHubReleaseChannel,
        fallbackToPreRelease: Boolean,
    ): Result<GitHubReleaseMatch> = when (forge) {
        ReleaseForge.GITHUB -> gitHubReleasesClient.matchLatestRelease(
            ownerRepo,
            assetRegex,
            channel,
            fallbackToPreRelease,
        )

        ReleaseForge.GITLAB -> gitLabReleasesClient.matchLatestRelease(
            ownerRepo,
            assetRegex,
            channel,
            fallbackToPreRelease,
        )

        ReleaseForge.CODEBERG -> codebergReleasesClient.matchLatestRelease(
            ownerRepo,
            assetRegex,
            channel,
            fallbackToPreRelease,
        )
    }

    private suspend fun listReleases(forge: ReleaseForge, ownerRepo: String): Result<List<GitHubReleaseDto>> = when (forge) {
        ReleaseForge.GITHUB -> gitHubReleasesClient.listReleases(ownerRepo)
        ReleaseForge.GITLAB -> gitLabReleasesClient.listReleases(ownerRepo)
        ReleaseForge.CODEBERG -> codebergReleasesClient.listReleases(ownerRepo)
    }

    private companion object {
        const val DEFAULT_APK_REGEX = ".*\\.apk$"
    }
}

data class FdroidInstallOptions(
    val packageName: String,
    val repoId: String,
    val repoName: String,
    val repoAddress: String,
    val versions: List<VersionGroup>,
)

data class GithubInstallOptions(
    val ownerRepo: String,
    val packageName: String,
    val releases: List<GithubReleaseVariantRow>,
    val forge: ReleaseForge = ReleaseForge.GITHUB,
)

internal fun IndexedPackageEntity.toDiscoveryRow(repo: RepoEntity?): DiscoveryAppRow {
    val address = repo?.address.orEmpty()
    return DiscoveryAppRow(
        packageName = packageName,
        name = name,
        summary = summary,
        repoId = repoId,
        repoName = repo?.name ?: repoId,
        repoAddress = address,
        categories = categoriesCsv.split(',').map { it.trim() }.filter { it.isNotEmpty() },
        antiFeatures = antiFeaturesCsv.split(',').map { it.trim() }.filter { it.isNotEmpty() },
        versionName = suggestedVersionName,
        versionCode = suggestedVersionCode,
        iconUrl = FdroidMediaUrl.join(address, iconPath),
        sourceCodeUrl = sourceCodeUrl,
        phoneScreenshotUrls = phoneScreenshotsCsv
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { FdroidMediaUrl.join(address, it) },
    )
}
