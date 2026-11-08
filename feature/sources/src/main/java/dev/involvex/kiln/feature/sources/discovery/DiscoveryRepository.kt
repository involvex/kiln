package dev.involvex.kiln.feature.sources.discovery

import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.database.IndexDao
import dev.involvex.kiln.core.database.IndexedPackageEntity
import dev.involvex.kiln.core.database.RepoDao
import dev.involvex.kiln.core.database.RepoEntity
import dev.involvex.kiln.core.model.AppUpdateInfo
import dev.involvex.kiln.core.model.InstallQueue
import dev.involvex.kiln.core.model.MatchConfidence
import dev.involvex.kiln.core.model.SourceKind
import dev.involvex.kiln.feature.sources.FdroidApkUrl
import dev.involvex.kiln.feature.sources.fdroid.FDroidRepoSync
import javax.inject.Inject
import javax.inject.Singleton

data class DiscoveryAppRow(
    val packageName: String,
    val name: String,
    val summary: String?,
    val repoId: String,
    val repoName: String,
    val categories: List<String>,
    val versionName: String?,
    val versionCode: Long?,
)

@Singleton
class DiscoveryRepository @Inject constructor(
    private val indexDao: IndexDao,
    private val repoDao: RepoDao,
    private val sync: FDroidRepoSync,
    private val installQueue: InstallQueue,
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
            entity.toRow(repos[entity.repoId])
        }
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
            ?: return Result.failure("apk_url_invalid")
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

    private fun IndexedPackageEntity.toRow(repo: RepoEntity?) = DiscoveryAppRow(
        packageName = packageName,
        name = name,
        summary = summary,
        repoId = repoId,
        repoName = repo?.name ?: repoId,
        categories = categoriesCsv.split(',').map { it.trim() }.filter { it.isNotEmpty() },
        versionName = suggestedVersionName,
        versionCode = suggestedVersionCode,
    )
}
