package dev.involvex.kiln.feature.backup.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.common.IoDispatcher
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.BackupAppCandidate
import dev.involvex.kiln.core.model.BackupArchiveMetadata
import dev.involvex.kiln.core.model.BackupContentTier
import dev.involvex.kiln.core.model.InstallQueue
import dev.involvex.kiln.core.model.LocalInstallRequest
import dev.involvex.kiln.core.model.PrivilegeCapability
import dev.involvex.kiln.core.model.PrivilegedExecutor
import dev.involvex.kiln.feature.backup.archive.BackupArchiveReader
import dev.involvex.kiln.feature.backup.archive.BackupArchiveWriter
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packageLoader: BackupPackageLoader,
    private val installQueue: InstallQueue,
    private val privilegedExecutor: PrivilegedExecutor,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    fun activeTier(): BackupContentTier {
        val caps = privilegedExecutor.currentStatus().capabilities
        return when {
            PrivilegeCapability.FULL_BACKUP in caps -> BackupContentTier.ROOT
            PrivilegeCapability.ACCESS_ANDROID_DATA in caps -> BackupContentTier.SHIZUKU
            else -> BackupContentTier.NONE
        }
    }

    suspend fun listCandidates(): List<BackupAppCandidate> = packageLoader.listUserApps()

    suspend fun backupToUri(packageName: String, targetUri: Uri, contentResolver: ContentResolver): Result<BackupArchiveMetadata> =
        withContext(ioDispatcher) {
            val detail = packageLoader.loadDetail(packageName)
                ?: return@withContext Result.failure("package_not_found")
            if (detail.apkFiles.isEmpty()) {
                return@withContext Result.failure("apk_unreadable")
            }
            val tier = BackupContentTier.NONE
            val metadata = BackupArchiveMetadata(
                packageName = detail.candidate.packageName,
                label = detail.candidate.label,
                versionName = detail.candidate.versionName,
                versionCode = detail.candidate.versionCode,
                signerSha256 = detail.signerSha256,
                tier = tier,
                createdAtEpochMs = System.currentTimeMillis(),
                includesAppData = false,
                includesAndroidData = false,
                apkEntries = detail.apkFiles.map { it.name },
                hasSplits = detail.candidate.hasSplits,
            )
            try {
                val stream = contentResolver.openOutputStream(targetUri)
                    ?: return@withContext Result.failure("could_not_open_output")
                stream.use { BackupArchiveWriter.write(detail.apkFiles, metadata, it) }
                Result.success(metadata)
            } catch (e: IOException) {
                runCatching { contentResolver.delete(targetUri, null, null) }
                Result.failure(e.message ?: "backup_failed", e)
            } catch (e: SecurityException) {
                runCatching { contentResolver.delete(targetUri, null, null) }
                Result.failure(e.message ?: "backup_failed", e)
            }
        }

    suspend fun restoreFromUri(uri: Uri, contentResolver: ContentResolver): Result<String> = withContext(ioDispatcher) {
        val input = contentResolver.openInputStream(uri)
            ?: return@withContext Result.failure("could_not_open_input")
        val outDir = File(context.cacheDir, "backup_restore").apply {
            deleteRecursively()
            mkdirs()
        }
        when (val extracted = input.use { BackupArchiveReader.extract(it, outDir) }) {
            is Result.Failure -> extracted

            is Result.Success -> {
                val meta = extracted.value.metadata
                val base = extracted.value.baseApk
                installQueue.enqueueLocal(
                    LocalInstallRequest(
                        packageName = meta.packageName,
                        versionName = meta.versionName ?: meta.versionCode.toString(),
                        versionCode = meta.versionCode,
                        apkAbsolutePath = base.absolutePath,
                        expectedSha256 = null,
                    ),
                )
            }
        }
    }

    fun suggestedFileName(packageName: String, versionName: String?): String {
        val ver = versionName?.replace(Regex("[^A-Za-z0-9._-]"), "_") ?: "backup"
        return "${packageName}_$ver.kilnbackup.zip"
    }
}
