package dev.involvex.kiln.feature.apps.apktools

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.common.IoDispatcher
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.ApkArchiveKind
import dev.involvex.kiln.core.model.ApkInspectReport
import dev.involvex.kiln.core.model.ApkSignatureCompareReport
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class ApkToolsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun inspectUri(uri: Uri): Result<ApkInspectReport> = withContext(ioDispatcher) {
        val displayName = queryDisplayName(uri) ?: "picked"
        val cache = copyUriToCache(uri, displayName)
            ?: return@withContext Result.failure("copy_failed")
        try {
            Result.success(ApkArchiveReader.inspect(context.packageManager, cache, displayName))
        } finally {
            cache.delete()
        }
    }

    suspend fun compareUris(left: Uri, right: Uri): Result<ApkSignatureCompareReport> = withContext(ioDispatcher) {
        val leftName = queryDisplayName(left) ?: "left"
        val rightName = queryDisplayName(right) ?: "right"
        val leftFile = copyUriToCache(left, leftName) ?: return@withContext Result.failure("copy_failed")
        val rightFile = copyUriToCache(right, rightName) ?: return@withContext Result.failure("copy_failed")
        try {
            Result.success(
                ApkArchiveReader.compareArchives(
                    packageManager = context.packageManager,
                    left = leftFile,
                    leftLabel = leftName,
                    right = rightFile,
                    rightLabel = rightName,
                ),
            )
        } finally {
            leftFile.delete()
            rightFile.delete()
        }
    }

    suspend fun compareUriToInstalled(uri: Uri, packageName: String): Result<ApkSignatureCompareReport> = withContext(ioDispatcher) {
        val name = queryDisplayName(uri) ?: "apk"
        val file = copyUriToCache(uri, name) ?: return@withContext Result.failure("copy_failed")
        try {
            Result.success(
                ApkArchiveReader.compareArchiveToInstalled(
                    packageManager = context.packageManager,
                    apk = file,
                    apkLabel = name,
                    packageName = packageName.trim(),
                ),
            )
        } finally {
            file.delete()
        }
    }

    suspend fun resign(
        apkUri: Uri,
        keystoreUri: Uri,
        outputUri: Uri,
        alias: String,
        storePassword: CharArray,
        keyPassword: CharArray,
    ): Result<Unit> = withContext(ioDispatcher) {
        val credentials = ApkResign.Credentials(
            storePassword = storePassword,
            keyAlias = alias,
            keyPassword = keyPassword,
        )
        val apkName = queryDisplayName(apkUri) ?: "app.apk"
        val keyName = queryDisplayName(keystoreUri) ?: "keystore.p12"
        val apkFile = copyUriToCache(apkUri, apkName) ?: return@withContext Result.failure("copy_failed")
        val keyFile = copyUriToCache(keystoreUri, keyName) ?: run {
            apkFile.delete()
            return@withContext Result.failure("copy_failed")
        }
        val outFile = File(context.cacheDir, "apk_tools/out-${System.currentTimeMillis()}.apk")
        outFile.parentFile?.mkdirs()
        try {
            val signed = ApkResign.sign(apkFile, outFile, keyFile, credentials)
            if (signed.isFailure) {
                return@withContext Result.failure(signed.exceptionOrNull()?.message ?: "resign_failed")
            }
            context.contentResolver.openOutputStream(outputUri)?.use { output ->
                outFile.inputStream().use { input -> input.copyTo(output) }
            } ?: return@withContext Result.failure("output_write_failed")
            Result.success(Unit)
        } finally {
            ApkResign.clearCredentials(credentials)
            apkFile.delete()
            keyFile.delete()
            outFile.delete()
        }
    }

    suspend fun exportSplits(bundleUri: Uri, treeUri: Uri): Result<Int> = withContext(ioDispatcher) {
        val name = queryDisplayName(bundleUri) ?: "bundle.apks"
        val kind = ApkArchiveReader.kindOf(name)
        if (kind != ApkArchiveKind.APKS && kind != ApkArchiveKind.XAPK) {
            return@withContext Result.failure("unsupported_archive")
        }
        val bundle = copyUriToCache(bundleUri, name) ?: return@withContext Result.failure("copy_failed")
        val tempDir = File(context.cacheDir, "apk_tools/export-${System.currentTimeMillis()}").apply { mkdirs() }
        try {
            val extracted = ApkSplitExporter.extractApkMembers(bundle, tempDir)
            if (extracted.isFailure) {
                return@withContext Result.failure(extracted.exceptionOrNull()?.message ?: "bundle_extract_failed")
            }
            writeExtractedApksToTree(tempDir, treeUri)
        } finally {
            bundle.delete()
            tempDir.deleteRecursively()
        }
    }

    private fun writeExtractedApksToTree(tempDir: File, treeUri: Uri): Result<Int> {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        }
        val tree = DocumentFile.fromTreeUri(context, treeUri)
            ?: return Result.failure("dest_unwritable")
        val files = tempDir.listFiles().orEmpty()
        if (files.isEmpty()) return Result.failure("bundle_no_apk")
        val failed = files.firstOrNull { file ->
            !copyFileIntoTree(tree, file)
        }
        return if (failed != null) {
            Result.failure("dest_unwritable")
        } else {
            Result.success(files.size)
        }
    }

    private fun copyFileIntoTree(tree: DocumentFile, file: File): Boolean {
        val target = tree.createFile("application/vnd.android.package-archive", file.name) ?: return false
        return context.contentResolver.openOutputStream(target.uri)?.use { output ->
            file.inputStream().use { input -> input.copyTo(output) }
            true
        } ?: false
    }

    fun displayName(uri: Uri): String? = queryDisplayName(uri)

    private fun queryDisplayName(uri: Uri): String? {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) return cursor.getString(index)
            }
        }
        return uri.lastPathSegment
    }

    private fun copyUriToCache(uri: Uri, displayName: String): File? = runCatching {
        val dir = File(context.cacheDir, "apk_tools").apply { mkdirs() }
        val safe = displayName.replace(Regex("[^A-Za-z0-9._-]"), "_").take(80).ifBlank { "picked" }
        val out = File(dir, "in-${System.currentTimeMillis()}-$safe")
        context.contentResolver.openInputStream(uri)?.use { input ->
            out.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        out
    }.getOrNull()
}
