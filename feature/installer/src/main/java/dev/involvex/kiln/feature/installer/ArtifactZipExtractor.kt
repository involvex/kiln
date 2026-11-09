package dev.involvex.kiln.feature.installer

import dev.involvex.kiln.core.common.Result
import java.io.File
import java.io.IOException
import java.util.zip.ZipFile

object ArtifactZipExtractor {
    /**
     * Extracts the first zip entry whose name matches [apkRegex] into [targetApk].
     * Entry paths are sanitized (no `..` / absolute paths).
     */
    fun extractMatchingApk(zipFile: File, targetApk: File, apkRegex: Regex): Result<File> {
        if (!zipFile.isFile) return Result.failure("zip_missing")
        return try {
            ZipFile(zipFile).use { zip ->
                val entry = zip.entries().asSequence()
                    .filter { !it.isDirectory }
                    .firstOrNull { zipEntry ->
                        val name = zipEntry.name.substringAfterLast('/').substringAfterLast('\\')
                        apkRegex.containsMatchIn(name)
                    }
                    ?: return Result.failure("apk_not_in_artifact")
                val safeName = entry.name.replace('\\', '/')
                if (safeName.contains("..") || safeName.startsWith("/")) {
                    return Result.failure("unsafe_zip_entry")
                }
                targetApk.parentFile?.mkdirs()
                zip.getInputStream(entry).use { input ->
                    targetApk.outputStream().use { output -> input.copyTo(output) }
                }
                Result.success(targetApk)
            }
        } catch (error: IOException) {
            Result.failure(error.message ?: "zip_extract_failed", error)
        }
    }

    fun looksLikeArtifactZip(downloadUrl: String, requiresGitHubAuth: Boolean): Boolean {
        if (requiresGitHubAuth) return true
        val lower = downloadUrl.lowercase()
        return lower.contains("/actions/") && lower.endsWith("/zip")
    }
}
