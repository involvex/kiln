package dev.involvex.kiln.feature.sources.discovery

import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.database.IndexDao
import dev.involvex.kiln.core.database.IndexedPackageEntity
import dev.involvex.kiln.core.database.ManagedAppDao
import dev.involvex.kiln.core.database.ManagedAppEntity
import dev.involvex.kiln.core.database.RepoDao
import dev.involvex.kiln.core.database.RepoEntity
import dev.involvex.kiln.core.model.AppUpdateInfo
import dev.involvex.kiln.core.model.InstallQueue
import dev.involvex.kiln.core.model.MatchConfidence
import dev.involvex.kiln.core.model.SourceKind
import dev.involvex.kiln.feature.sources.FdroidApkUrl
import dev.involvex.kiln.feature.sources.FdroidMediaUrl
import dev.involvex.kiln.feature.sources.fdroid.FDroidRepoSync
import dev.involvex.kiln.feature.sources.github.GitHubOwnerRepo
import dev.involvex.kiln.feature.sources.github.GitHubReleasesClient
import dev.involvex.kiln.feature.sources.github.GitHubVersionCode
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
        val repo = repoDao.getById(row.repoId) ?: return Result.failure("repo_missing")
        val release = indexDao.latestRelease(row.packageName, row.repoId)
            ?: return Result.failure("release_missing")
        val url = FdroidApkUrl.build(repo.address, release.apkName)
        val update = AppUpdateInfo(
            packageName = row.packageName,
            availableVersionName = release.versionName,
            availableVersionCode = release.versionCode,
            sourceId = repo.id,
            sourceLabel = repo.name,
            confidence = MatchConfidence.MEDIUM,
            signatureMatches = true,
            downloadUrl = url,
            apkSha256 = release.apkSha256,
            sizeBytes = release.sizeBytes,
        )
        return installQueue.enqueue(update)
    }

    suspend fun savedGithubSources(): List<DiscoveryGithubRow> {
        return managedAppDao.getAll()
            .mapNotNull { app ->
                val ownerRepo = app.githubOwnerRepo?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                DiscoveryGithubRow(
                    ownerRepo = ownerRepo,
                    packageName = app.packageName,
                    displayName = ownerRepo,
                    tagName = null,
                    assetName = null,
                    sizeBytes = null,
                    downloadUrl = null,
                    saved = true,
                )
            }
            .sortedBy { it.ownerRepo.lowercase() }
    }

    suspend fun lookupGithub(raw: String): Result<DiscoveryGithubRow> {
        val ownerRepo = GitHubOwnerRepo.parse(raw) ?: return Result.failure("github_query_invalid")
        val existing = managedAppDao.getAll()
            .firstOrNull { it.githubOwnerRepo.equals(ownerRepo, ignoreCase = true) }
        val packageName = existing?.packageName ?: GitHubOwnerRepo.provisionalPackageName(ownerRepo)
        val regex = runCatching {
            Regex(existing?.githubAssetRegex ?: DEFAULT_APK_REGEX)
        }.getOrElse { Regex(DEFAULT_APK_REGEX) }
        val includePre = existing?.includePreReleases == true
        return when (
            val result = gitHubReleasesClient.latestMatchingRelease(ownerRepo, regex, includePre)
        ) {
            is Result.Failure -> result

            is Result.Success -> {
                val pick = result.value
                if (existing == null) {
                    managedAppDao.upsert(
                        ManagedAppEntity(
                            packageName = packageName,
                            sourceId = "github:$ownerRepo",
                            confidence = MatchConfidence.MEDIUM.name,
                            pinned = false,
                            ignoreUpdates = false,
                            githubOwnerRepo = ownerRepo,
                            githubAssetRegex = DEFAULT_APK_REGEX,
                            includePreReleases = false,
                        ),
                    )
                }
                Result.success(
                    DiscoveryGithubRow(
                        ownerRepo = ownerRepo,
                        packageName = packageName,
                        displayName = ownerRepo,
                        tagName = pick.tagName,
                        assetName = pick.assetName,
                        sizeBytes = pick.sizeBytes,
                        downloadUrl = pick.downloadUrl,
                        saved = true,
                    ),
                )
            }
        }
    }

    suspend fun installGithub(row: DiscoveryGithubRow): Result<String> {
        val url = row.downloadUrl ?: return Result.failure("github_release_missing")
        val tag = row.tagName ?: return Result.failure("github_release_missing")
        val update = AppUpdateInfo(
            packageName = row.packageName,
            availableVersionName = tag,
            availableVersionCode = GitHubVersionCode.parse(tag),
            sourceId = "github:${row.ownerRepo}",
            sourceLabel = "GitHub ${row.ownerRepo}",
            confidence = MatchConfidence.MEDIUM,
            // First-time GitHub APK: signature checked at install time against device state.
            signatureMatches = false,
            downloadUrl = url,
            apkSha256 = null,
            sizeBytes = row.sizeBytes,
        )
        return installQueue.enqueue(update)
    }

    private companion object {
        const val DEFAULT_APK_REGEX = ".*\\.apk$"
    }
}

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
