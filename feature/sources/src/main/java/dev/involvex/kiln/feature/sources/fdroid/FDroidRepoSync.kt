package dev.involvex.kiln.feature.sources.fdroid

import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.common.normalizeFingerprint
import dev.involvex.kiln.core.database.IndexDao
import dev.involvex.kiln.core.database.IndexedPackageEntity
import dev.involvex.kiln.core.database.IndexedReleaseEntity
import dev.involvex.kiln.core.database.RepoDao
import dev.involvex.kiln.core.database.RepoEntity
import dev.involvex.kiln.core.network.KilnHttpClient
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class FDroidRepoSync @Inject constructor(private val http: KilnHttpClient, private val repoDao: RepoDao, private val indexDao: IndexDao) {
    suspend fun sync(repo: RepoEntity): Result<Unit> {
        val base = repo.address.trimEnd('/')
        val parsedResult = http.getBytes("$base/entry.jar")
            .flatMap { entryBytes ->
                val verified = EntryJarVerifier.verify(entryBytes, repo.fingerprint)
                if (verified is Result.Failure) {
                    Timber.w("Rejecting repo %s: signature verify failed", repo.id)
                }
                verified
            }
            .flatMap { entryJson -> IndexV2Parser.parseEntryPointer(entryJson) }
            .flatMap { pointer -> loadAndParseIndex(base, pointer) }
        return when (parsedResult) {
            is Result.Failure -> parsedResult

            is Result.Success -> {
                persistIndex(repo, parsedResult.value)
                Result.success(Unit)
            }
        }
    }

    private suspend fun loadAndParseIndex(base: String, pointer: EntryIndexPointer): Result<List<ParsedIndexPackage>> =
        http.getBytes("$base/${pointer.name}").flatMap { indexBytes ->
            verifyIndexSha(indexBytes, pointer.sha256).flatMap {
                IndexV2Parser.parsePackages(indexBytes.toString(Charsets.UTF_8))
            }
        }

    private fun verifyIndexSha(indexBytes: ByteArray, expectedSha: String): Result<Unit> {
        val actualSha = sha256Hex(indexBytes)
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
    }

    private fun sha256Hex(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it) }
}
