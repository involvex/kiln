package dev.involvex.kiln.feature.backup.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.common.BackupCrypto
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.AndroidSharedRoot
import dev.involvex.kiln.core.model.BackupArchiveMetadata
import dev.involvex.kiln.core.model.BackupContentTier
import dev.involvex.kiln.core.model.PrivilegeCapability
import dev.involvex.kiln.core.model.PrivilegedExecutor
import dev.involvex.kiln.feature.backup.archive.BackupArchiveWriter
import dev.involvex.kiln.feature.backup.archive.BackupMetadataCodec
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class BackupArchiveBuilder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packageLoader: BackupPackageLoader,
    private val privilegedExecutor: PrivilegedExecutor,
) {
    fun canIncludeAndroidData(): Boolean = PrivilegeCapability.ACCESS_ANDROID_DATA in privilegedExecutor.currentStatus().capabilities

    suspend fun build(
        packageName: String,
        password: CharArray?,
        targetUserId: Int? = null,
    ): Result<Pair<ByteArray, BackupArchiveMetadata>> {
        val detail = packageLoader.loadDetail(packageName)
            ?: return Result.failure("package_not_found")
        if (detail.apkFiles.isEmpty()) {
            return Result.failure("apk_unreadable")
        }
        val stage = stagingDir().apply {
            deleteRecursively()
            mkdirs()
        }
        return try {
            val includeData = canIncludeAndroidData()
            val shared = if (includeData) {
                archiveSharedDirs(packageName, stage)
            } else {
                SharedArchiveParts(null, null, included = false)
            }
            val encrypted = password != null && password.isNotEmpty()
            val metadata = metadataFor(detail, shared, encrypted, includeData, targetUserId)
            val zipBytes = writeZip(detail.apkFiles, metadata, shared)
            val payload = maybeEncrypt(zipBytes, password, encrypted)
            Result.success(payload to metadata)
        } catch (e: IOException) {
            Timber.e(e, "build archive failed")
            Result.failure(e.message ?: "backup_failed", e)
        } catch (e: GeneralSecurityException) {
            Timber.e(e, "build archive crypto failed")
            Result.failure(e.message ?: "backup_failed", e)
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "build archive invalid")
            Result.failure(e.message ?: "backup_failed", e)
        } finally {
            stage.deleteRecursively()
        }
    }

    fun stagingDir(): File {
        val base = context.externalCacheDir ?: context.cacheDir
        return File(base, "backup_stage")
    }

    private fun metadataFor(
        detail: BackupPackageDetail,
        shared: SharedArchiveParts,
        encrypted: Boolean,
        includeData: Boolean,
        targetUserId: Int?,
    ): BackupArchiveMetadata {
        val formatVersion = if (encrypted || shared.included) FORMAT_V2 else FORMAT_V1
        val contentTier = if (includeData) BackupContentTier.SHIZUKU else BackupContentTier.NONE
        return BackupArchiveMetadata(
            formatVersion = formatVersion,
            packageName = detail.candidate.packageName,
            label = detail.candidate.label,
            versionName = detail.candidate.versionName,
            versionCode = detail.candidate.versionCode,
            signerSha256 = detail.signerSha256,
            tier = contentTier,
            createdAtEpochMs = System.currentTimeMillis(),
            includesAppData = false,
            includesAndroidData = shared.included,
            apkEntries = detail.apkFiles.map { it.name },
            hasSplits = detail.candidate.hasSplits,
            encrypted = encrypted,
            androidDataEntry = shared.dataEntryName,
            androidObbEntry = shared.obbEntryName,
            targetUserId = targetUserId,
        )
    }

    private fun writeZip(apkFiles: List<File>, metadata: BackupArchiveMetadata, shared: SharedArchiveParts): ByteArray =
        ByteArrayOutputStream().use { out ->
            BackupArchiveWriter.write(
                apkFiles = apkFiles,
                metadata = metadata.copy(encrypted = false),
                output = out,
                androidDataTar = shared.dataTar,
                androidObbTar = shared.obbTar,
            )
            out.toByteArray()
        }

    private fun maybeEncrypt(zipBytes: ByteArray, password: CharArray?, encrypted: Boolean): ByteArray {
        if (!encrypted || password == null) return zipBytes
        return BackupCrypto.encrypt(zipBytes, password)
    }

    private suspend fun archiveSharedDirs(packageName: String, stage: File): SharedArchiveParts {
        val dataResult = archiveOneRoot(AndroidSharedRoot.DATA, packageName, File(stage, BackupMetadataCodec.ANDROID_DATA_ENTRY))
        val obbResult = archiveOneRoot(AndroidSharedRoot.OBB, packageName, File(stage, BackupMetadataCodec.ANDROID_OBB_ENTRY))
        return SharedArchiveParts(
            dataTar = dataResult,
            obbTar = obbResult,
            included = dataResult != null || obbResult != null,
            dataEntryName = dataResult?.let { BackupMetadataCodec.ANDROID_DATA_ENTRY },
            obbEntryName = obbResult?.let { BackupMetadataCodec.ANDROID_OBB_ENTRY },
        )
    }

    private suspend fun archiveOneRoot(root: AndroidSharedRoot, packageName: String, dest: File): File? = when (
        val result = privilegedExecutor.archiveAndroidSharedPackageDir(root, packageName, dest.absolutePath)
    ) {
        is Result.Failure -> {
            Timber.w("android shared archive %s: %s", root, result.message)
            null
        }

        is Result.Success -> if (result.value && dest.isFile && dest.length() > 0) dest else null
    }

    private data class SharedArchiveParts(
        val dataTar: File?,
        val obbTar: File?,
        val included: Boolean,
        val dataEntryName: String? = null,
        val obbEntryName: String? = null,
    )

    private companion object {
        const val FORMAT_V1 = 1
        const val FORMAT_V2 = 2
    }
}
