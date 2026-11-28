package dev.involvex.kiln.feature.backup.data

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.common.BackupCrypto
import dev.involvex.kiln.core.common.IoDispatcher
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.ActivityLogEvents
import dev.involvex.kiln.core.model.ActivityLogRecorder
import dev.involvex.kiln.core.model.ActivityLogType
import dev.involvex.kiln.core.model.AndroidSharedRoot
import dev.involvex.kiln.core.model.BackupAppCandidate
import dev.involvex.kiln.core.model.BackupArchiveMetadata
import dev.involvex.kiln.core.model.BackupContentTier
import dev.involvex.kiln.core.model.BackupPreferences
import dev.involvex.kiln.core.model.BackupRotation
import dev.involvex.kiln.core.model.BackupTreeEntry
import dev.involvex.kiln.core.model.InstallQueue
import dev.involvex.kiln.core.model.LocalInstallRequest
import dev.involvex.kiln.core.model.PrivilegeCapability
import dev.involvex.kiln.core.model.PrivilegedExecutor
import dev.involvex.kiln.feature.backup.archive.BackupArchiveReader
import dev.involvex.kiln.feature.backup.archive.BackupExtractResult
import dev.involvex.kiln.feature.backup.archive.BackupMetadataCodec
import dev.involvex.kiln.feature.backup.archive.BackupRestoreApks
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.security.GeneralSecurityException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
@Suppress("TooManyFunctions") // SAF + WebDAV backup/restore + shared data restore paths.
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packageLoader: BackupPackageLoader,
    private val installQueue: InstallQueue,
    private val privilegedExecutor: PrivilegedExecutor,
    private val backupPreferences: BackupPreferences,
    private val treeStore: BackupTreeStore,
    private val archiveBuilder: BackupArchiveBuilder,
    private val webDavClient: WebDavClient,
    private val activityLog: ActivityLogRecorder,
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

    fun canIncludeAndroidData(): Boolean = archiveBuilder.canIncludeAndroidData()

    fun canIncludeAppData(): Boolean = archiveBuilder.canIncludeAppData()

    suspend fun listCandidates(): List<BackupAppCandidate> = packageLoader.listUserApps()

    suspend fun backupToUri(
        packageName: String,
        targetUri: Uri,
        contentResolver: ContentResolver,
        password: CharArray? = null,
        targetUserId: Int? = null,
        currentUserId: Int = 0,
    ): Result<BackupArchiveMetadata> = withContext(ioDispatcher) {
        when (val built = archiveBuilder.build(packageName, password, targetUserId)) {
            is Result.Failure -> built

            is Result.Success -> writeBytesToUri(
                built.value.first,
                targetUri,
                contentResolver,
                built.value.second,
                currentUserId,
            )
        }
    }

    suspend fun backupToTree(
        packageName: String,
        password: CharArray? = null,
        targetUserId: Int? = null,
        currentUserId: Int = 0,
    ): Result<BackupArchiveMetadata> = withContext(ioDispatcher) {
        val prefs = backupPreferences.current()
        val treeUri = prefs.treeUri ?: return@withContext Result.failure("tree_not_set")
        val detail = packageLoader.loadDetail(packageName)
            ?: return@withContext Result.failure("package_not_found")
        val fileName = suggestedFileName(packageName, detail.candidate.versionName)
        val doc = treeStore.createBackupFile(treeUri, packageName, fileName)
            ?: return@withContext Result.failure("tree_create_failed")
        when (val built = archiveBuilder.build(packageName, password, targetUserId)) {
            is Result.Failure -> built

            is Result.Success -> {
                val written = writeBytesToUri(
                    built.value.first,
                    doc.uri,
                    context.contentResolver,
                    built.value.second,
                    currentUserId,
                )
                if (written is Result.Success) {
                    treeStore.prunePackageArchives(treeUri, packageName, prefs.keepN)
                }
                written
            }
        }
    }

    suspend fun testWebDav(): Result<Unit> = webDavClient.testConnection()

    suspend fun backupToWebDav(
        packageName: String,
        password: CharArray? = null,
        targetUserId: Int? = null,
        currentUserId: Int = 0,
    ): Result<BackupArchiveMetadata> = withContext(ioDispatcher) {
        val prefs = backupPreferences.current()
        val detail = packageLoader.loadDetail(packageName)
            ?: return@withContext Result.failure("package_not_found")
        val fileName = suggestedFileName(packageName, detail.candidate.versionName)
        when (val built = archiveBuilder.build(packageName, password, targetUserId)) {
            is Result.Failure -> built

            is Result.Success -> {
                val (bytes, metadata) = built.value
                when (val put = webDavClient.putArchive(packageName, fileName, bytes)) {
                    is Result.Failure -> {
                        logBackupFail(packageName, put.message)
                        put
                    }

                    is Result.Success -> {
                        pruneWebDavArchives(packageName, prefs.keepN)
                        activityLog.record(
                            ActivityLogEvents.create(
                                type = ActivityLogType.BACKUP,
                                action = "backup_succeeded",
                                success = true,
                                packageName = metadata.packageName,
                                versionName = metadata.versionName,
                                versionCode = metadata.versionCode,
                                detailKey = buildUserDetailKey(
                                    metadata.targetUserId,
                                    currentUserId,
                                    failureDetail = "webdav",
                                ),
                            ),
                        )
                        Result.success(metadata)
                    }
                }
            }
        }
    }

    suspend fun listWebDavArchives(packageName: String): Result<List<BackupTreeEntry>> = webDavClient.listArchives(packageName)

    suspend fun restoreFromWebDav(
        href: String,
        password: CharArray? = null,
        targetUserId: Int? = null,
        currentUserId: Int = 0,
    ): Result<String> = withContext(ioDispatcher) {
        when (val downloaded = webDavClient.downloadArchive(href)) {
            is Result.Failure -> downloaded

            is Result.Success -> restoreFromBytes(
                bytes = downloaded.value,
                password = password,
                targetUserId = targetUserId,
                currentUserId = currentUserId,
            )
        }
    }

    suspend fun restoreFromUri(
        uri: Uri,
        contentResolver: ContentResolver,
        password: CharArray? = null,
        targetUserId: Int? = null,
        currentUserId: Int = 0,
    ): Result<String> = withContext(ioDispatcher) {
        when (val opened = openZipStream(uri, contentResolver, password)) {
            is Result.Failure -> opened

            is Result.Success -> {
                val outDir = File(context.cacheDir, "backup_restore").apply {
                    deleteRecursively()
                    mkdirs()
                }
                when (val extracted = opened.value.use { BackupArchiveReader.extract(it, outDir) }) {
                    is Result.Failure -> {
                        logRestoreFail(extracted.message)
                        extracted
                    }

                    is Result.Success -> enqueueRestore(
                        extracted = extracted.value,
                        targetUserId = targetUserId,
                        currentUserId = currentUserId,
                    )
                }
            }
        }
    }

    fun suggestedFileName(packageName: String, versionName: String?): String {
        val ver = versionName?.replace(Regex("[^A-Za-z0-9._-]"), "_") ?: "backup"
        val stamp = System.currentTimeMillis()
        return "${packageName}_${ver}_$stamp.kilnbackup.zip"
    }

    private suspend fun pruneWebDavArchives(packageName: String, keepN: Int) {
        when (val listed = webDavClient.listArchives(packageName)) {
            is Result.Failure -> Timber.w("WebDAV prune list failed: %s", listed.message)

            is Result.Success -> {
                BackupRotation.selectForDeletion(listed.value, keepN).forEach { entry ->
                    when (val deleted = webDavClient.deleteHref(entry.uriString)) {
                        is Result.Failure -> Timber.w("WebDAV prune delete failed: %s", deleted.message)
                        is Result.Success -> Unit
                    }
                }
            }
        }
    }

    private suspend fun restoreFromBytes(bytes: ByteArray, password: CharArray?, targetUserId: Int?, currentUserId: Int): Result<String> {
        when (val opened = openZipBytes(bytes, password)) {
            is Result.Failure -> return opened

            is Result.Success -> {
                val outDir = File(context.cacheDir, "backup_restore").apply {
                    deleteRecursively()
                    mkdirs()
                }
                return when (val extracted = opened.value.use { BackupArchiveReader.extract(it, outDir) }) {
                    is Result.Failure -> {
                        logRestoreFail(extracted.message)
                        extracted
                    }

                    is Result.Success -> enqueueRestore(
                        extracted = extracted.value,
                        targetUserId = targetUserId,
                        currentUserId = currentUserId,
                    )
                }
            }
        }
    }

    private suspend fun openZipBytes(bytes: ByteArray, password: CharArray?): Result<InputStream> {
        val peek = bytes.copyOf(minOf(bytes.size, PEEK_HEADER_BYTES))
        val needsPassword = BackupCrypto.looksEncrypted(peek)
        if (needsPassword && (password == null || password.isEmpty())) {
            logRestoreFail("password_required")
            return Result.failure("password_required")
        }
        val input = ByteArrayInputStream(bytes)
        return try {
            val stream = if (needsPassword) {
                BackupCrypto.decryptToStream(input, checkNotNull(password))
            } else {
                input
            }
            Result.success(stream)
        } catch (e: GeneralSecurityException) {
            logRestoreFail("decrypt_failed")
            Result.failure("decrypt_failed", e)
        } catch (e: IllegalArgumentException) {
            logRestoreFail("decrypt_failed")
            Result.failure("decrypt_failed", e)
        } catch (e: IOException) {
            logRestoreFail("decrypt_failed")
            Result.failure("decrypt_failed", e)
        }
    }

    private suspend fun openZipStream(uri: Uri, contentResolver: ContentResolver, password: CharArray?): Result<InputStream> {
        val peek = contentResolver.openInputStream(uri)?.use { stream ->
            val buf = ByteArray(PEEK_HEADER_BYTES)
            val n = stream.read(buf)
            if (n <= 0) ByteArray(0) else buf.copyOf(n)
        }
        if (peek == null) {
            logRestoreFail("could_not_open_input")
            return Result.failure("could_not_open_input")
        }
        val needsPassword = BackupCrypto.looksEncrypted(peek)
        if (needsPassword && (password == null || password.isEmpty())) {
            logRestoreFail("password_required")
            return Result.failure("password_required")
        }
        val input = contentResolver.openInputStream(uri)
            ?: run {
                logRestoreFail("could_not_open_input")
                return Result.failure("could_not_open_input")
            }
        return try {
            val stream = if (needsPassword) {
                BackupCrypto.decryptToStream(input, checkNotNull(password))
            } else {
                input
            }
            Result.success(stream)
        } catch (e: GeneralSecurityException) {
            input.close()
            logRestoreFail("decrypt_failed")
            Timber.w(e, "backup decrypt failed")
            Result.failure("decrypt_failed", e)
        } catch (e: IllegalArgumentException) {
            input.close()
            logRestoreFail("decrypt_failed")
            Timber.w(e, "backup decrypt failed")
            Result.failure("decrypt_failed", e)
        } catch (e: IOException) {
            input.close()
            logRestoreFail("decrypt_failed")
            Timber.w(e, "backup decrypt failed")
            Result.failure("decrypt_failed", e)
        }
    }

    private suspend fun enqueueRestore(extracted: BackupExtractResult, targetUserId: Int?, currentUserId: Int): Result<String> {
        val meta = extracted.metadata
        if (BackupRestoreApks.isIncompleteSplitArchive(meta.hasSplits, extracted.allApkFiles.size)) {
            logRestoreFail("splits_incomplete")
            return Result.failure("splits_incomplete")
        }
        val resolvedUserId = targetUserId ?: meta.targetUserId
        val apkPaths = BackupRestoreApks.orderedAbsolutePaths(extracted.baseApk, extracted.allApkFiles)
        val queued = installQueue.enqueueLocal(
            LocalInstallRequest(
                packageName = meta.packageName,
                versionName = meta.versionName ?: meta.versionCode.toString(),
                versionCode = meta.versionCode,
                apkAbsolutePaths = apkPaths,
                expectedSha256 = null,
                targetUserId = resolvedUserId,
            ),
        )
        return when (queued) {
            is Result.Failure -> {
                activityLog.record(
                    ActivityLogEvents.create(
                        type = ActivityLogType.RESTORE,
                        action = "restore_failed",
                        success = false,
                        packageName = meta.packageName,
                        detailKey = buildUserDetailKey(resolvedUserId, currentUserId, queued.message),
                    ),
                )
                queued
            }

            is Result.Success -> {
                if (meta.includesAndroidData && canIncludeAndroidData()) {
                    restoreSharedDataAfterInstall(
                        packageName = meta.packageName,
                        dataTar = extracted.androidDataTar,
                        obbTar = extracted.androidObbTar,
                    )
                }
                if (meta.includesAppData && canIncludeAppData()) {
                    restoreAppDataAfterInstall(
                        packageName = meta.packageName,
                        appDataTar = extracted.appDataTar,
                    )
                }
                activityLog.record(
                    ActivityLogEvents.create(
                        type = ActivityLogType.RESTORE,
                        action = "restore_queued",
                        success = true,
                        packageName = meta.packageName,
                        versionName = meta.versionName,
                        versionCode = meta.versionCode,
                        refId = queued.value,
                        detailKey = buildUserDetailKey(resolvedUserId, currentUserId, failureDetail = null),
                    ),
                )
                queued
            }
        }
    }

    private suspend fun writeBytesToUri(
        bytes: ByteArray,
        targetUri: Uri,
        contentResolver: ContentResolver,
        metadata: BackupArchiveMetadata,
        currentUserId: Int,
    ): Result<BackupArchiveMetadata> {
        return try {
            val stream = contentResolver.openOutputStream(targetUri)
                ?: return Result.failure("could_not_open_output").also {
                    logBackupFail(metadata.packageName, "could_not_open_output")
                }
            stream.use { it.write(bytes) }
            activityLog.record(
                ActivityLogEvents.create(
                    type = ActivityLogType.BACKUP,
                    action = "backup_succeeded",
                    success = true,
                    packageName = metadata.packageName,
                    versionName = metadata.versionName,
                    versionCode = metadata.versionCode,
                    detailKey = buildUserDetailKey(metadata.targetUserId, currentUserId, failureDetail = null),
                ),
            )
            Result.success(metadata)
        } catch (e: IOException) {
            runCatching { contentResolver.delete(targetUri, null, null) }
            logBackupFail(metadata.packageName, "backup_failed")
            Result.failure(e.message ?: "backup_failed", e)
        } catch (e: SecurityException) {
            runCatching { contentResolver.delete(targetUri, null, null) }
            logBackupFail(metadata.packageName, "backup_failed")
            Result.failure(e.message ?: "backup_failed", e)
        }
    }

    private fun buildUserDetailKey(targetUserId: Int?, currentUserId: Int, failureDetail: String?): String? = buildString {
        if (targetUserId != null && targetUserId != currentUserId) append("user:$targetUserId")
        if (failureDetail != null) {
            if (isNotEmpty()) append(';')
            append(failureDetail)
        }
    }.ifEmpty { null }

    private suspend fun restoreSharedDataAfterInstall(packageName: String, dataTar: File?, obbTar: File?) {
        val stage = archiveBuilder.stagingDir().apply { mkdirs() }
        try {
            waitUntilInstalled(packageName, timeoutMs = INSTALL_WAIT_TIMEOUT_MS)
            privilegedExecutor.forceStopPackage(packageName)
            dataTar?.takeIf { it.isFile }?.let { src ->
                val staged = File(stage, BackupMetadataCodec.ANDROID_DATA_ENTRY)
                src.copyTo(staged, overwrite = true)
                privilegedExecutor.restoreAndroidSharedPackageDir(
                    AndroidSharedRoot.DATA,
                    packageName,
                    staged.absolutePath,
                )
            }
            obbTar?.takeIf { it.isFile }?.let { src ->
                val staged = File(stage, BackupMetadataCodec.ANDROID_OBB_ENTRY)
                src.copyTo(staged, overwrite = true)
                privilegedExecutor.restoreAndroidSharedPackageDir(
                    AndroidSharedRoot.OBB,
                    packageName,
                    staged.absolutePath,
                )
            }
        } catch (e: IOException) {
            Timber.w(e, "shared data restore incomplete for %s", packageName)
        } catch (e: SecurityException) {
            Timber.w(e, "shared data restore incomplete for %s", packageName)
        } finally {
            stage.deleteRecursively()
        }
    }

    private suspend fun restoreAppDataAfterInstall(packageName: String, appDataTar: File?) {
        val src = appDataTar?.takeIf { it.isFile } ?: return
        val stage = archiveBuilder.stagingDir().apply { mkdirs() }
        try {
            waitUntilInstalled(packageName, timeoutMs = INSTALL_WAIT_TIMEOUT_MS)
            privilegedExecutor.forceStopPackage(packageName)
            val staged = File(stage, BackupMetadataCodec.APP_DATA_ENTRY)
            src.copyTo(staged, overwrite = true)
            when (val restored = privilegedExecutor.restoreAppDataDir(packageName, staged.absolutePath)) {
                is Result.Failure -> Timber.w("app data restore incomplete for %s: %s", packageName, restored.message)
                is Result.Success -> Unit
            }
        } catch (e: IOException) {
            Timber.w(e, "app data restore incomplete for %s", packageName)
        } catch (e: SecurityException) {
            Timber.w(e, "app data restore incomplete for %s", packageName)
        } finally {
            stage.deleteRecursively()
        }
    }

    private suspend fun waitUntilInstalled(packageName: String, timeoutMs: Long) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            try {
                context.packageManager.getPackageInfo(packageName, 0)
                return
            } catch (_: PackageManager.NameNotFoundException) {
                delay(INSTALL_POLL_MS)
            }
        }
    }

    private suspend fun logBackupFail(packageName: String, key: String) {
        activityLog.record(
            ActivityLogEvents.create(
                type = ActivityLogType.BACKUP,
                action = "backup_failed",
                success = false,
                packageName = packageName,
                detailKey = key,
            ),
        )
    }

    private suspend fun logRestoreFail(key: String) {
        activityLog.record(
            ActivityLogEvents.create(
                type = ActivityLogType.RESTORE,
                action = "restore_failed",
                success = false,
                detailKey = key,
            ),
        )
    }

    private companion object {
        const val PEEK_HEADER_BYTES = 16
        const val INSTALL_WAIT_TIMEOUT_MS = 60_000L
        const val INSTALL_POLL_MS = 1_000L
    }
}
