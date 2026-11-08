package dev.involvex.kiln.feature.sources.fdroid

import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.common.normalizeFingerprint
import dev.involvex.kiln.core.database.IndexDao
import dev.involvex.kiln.core.database.IndexedPackageEntity
import dev.involvex.kiln.core.database.IndexedReleaseEntity
import dev.involvex.kiln.core.database.ManagedAppDao
import dev.involvex.kiln.core.database.RepoDao
import dev.involvex.kiln.core.database.RepoEntity
import dev.involvex.kiln.core.model.InstalledAppsReader
import dev.involvex.kiln.core.network.KilnHttpClient
import java.nio.file.Files
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class FDroidRepoSync @Inject constructor(
    private val http: KilnHttpClient,
    private val repoDao: RepoDao,
    private val indexDao: IndexDao,
    private val managedAppDao: ManagedAppDao,
    private val installedAppsReader: InstalledAppsReader,
) {
    suspend fun sync(repo: RepoEntity): Result<Unit> {
        val base = repo.address.trimEnd('/')
        val wanted = wantedPackageNames()
        Timber.i("Syncing repo %s for %d installed/managed packages", repo.id, wanted.size)
        val parsedResult = http.getBytes("$base/entry.jar")
            .flatMap { entryBytes ->
                val verified = EntryJarVerifier.verify(entryBytes, repo.fingerprint)
                if (verified is Result.Failure) {
                    Timber.w("Rejecting repo %s: signature verify failed (%s)", repo.id, verified.message)
                }
                verified
            }
            .flatMap { entryJson -> IndexV2Parser.parseEntryPointer(entryJson) }
            .flatMap { pointer -> loadAndParseIndex(base, pointer, wanted) }
        return when (parsedResult) {
            is Result.Failure -> parsedResult

            is Result.Success -> {
                persistIndex(repo, parsedResult.value)
                Result.success(Unit)
            }
        }
    }

    private suspend fun wantedPackageNames(): Set<String> {
        val installed = installedAppsReader.userApps().mapTo(HashSet()) { it.packageName }
        val managed = managedAppDao.getAll().mapTo(HashSet()) { it.packageName }
        return installed.apply { addAll(managed) }
    }

    private suspend fun loadAndParseIndex(
        base: String,
        pointer: EntryIndexPointer,
        wanted: Set<String>,
    ): Result<List<ParsedIndexPackage>> {
        // entry.json uses names like "/index-v2.json"; join without duplicating slashes.
        val indexPath = pointer.name.trimStart('/')
        val tmp = Files.createTempFile("kiln-index-", ".json").toFile()
        return try {
            http.downloadToFile("$base/$indexPath", tmp).flatMap { file ->
                verifyIndexShaFile(file, pointer.sha256).flatMap {
                    // Stream + filter: never readText() the full ~70MB index into heap.
                    file.inputStream().use { input ->
                        IndexV2StreamParser.parseWantedPackages(input, wanted)
                    }
                }
            }
        } catch (error: OutOfMemoryError) {
            Result.failure("index_oom", error)
        } finally {
            tmp.delete()
        }
    }

    private fun verifyIndexShaFile(file: java.io.File, expectedSha: String): Result<Unit> {
        val actualSha = sha256FileHex(file)
        val expectedNormalized = normalizeFingerprint(expectedSha)
        val expectedRaw = expectedSha.replace(":", "")
        val ok = actualSha.equals(expectedNormalized, ignoreCase = true) ||
            actualSha.equals(expectedRaw, ignoreCase = true)
        return if (ok) {
            Result.success(Unit)
        } else {
            Result.failure("index_sha256_mismatch")
        }
    }

    private suspend fun persistIndex(repo: RepoEntity, parsed: List<ParsedIndexPackage>) {
        val packages = parsed.map { pkg ->
            IndexedPackageEntity(
                packageName = pkg.packageName,
                repoId = repo.id,
                name = pkg.name,
                summary = pkg.summary,
                sourceCodeUrl = pkg.sourceCodeUrl,
                signerSha256Csv = pkg.signerSha256.joinToString(","),
                suggestedVersionCode = pkg.suggestedVersionCode,
                suggestedVersionName = pkg.suggestedVersionName,
            )
        }
        val releases = parsed.flatMap { pkg ->
            pkg.releases.map { rel ->
                IndexedReleaseEntity(
                    packageName = pkg.packageName,
                    repoId = repo.id,
                    versionName = rel.versionName,
                    versionCode = rel.versionCode,
                    apkName = rel.apkName,
                    apkSha256 = rel.apkSha256,
                    sizeBytes = rel.sizeBytes,
                    signerSha256Csv = rel.signerSha256.joinToString(","),
                )
            }
        }
        indexDao.replaceRepoIndex(repo.id, packages, releases)
        repoDao.updateLastSynced(repo.id, System.currentTimeMillis())
        Timber.i("Synced repo %s: %d packages", repo.id, packages.size)
    }

    private fun sha256FileHex(file: java.io.File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val DEFAULT_BUFFER = 64 * 1024
    }
}
