package dev.involvex.kiln.feature.apps.apktools

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.common.IoDispatcher
import dev.involvex.kiln.core.common.Result
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
