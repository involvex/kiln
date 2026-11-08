package dev.involvex.kiln.feature.apps.export

import android.content.ContentResolver
import android.net.Uri
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.ApkExportFormat
import dev.involvex.kiln.core.model.ApkPathEntry
import dev.involvex.kiln.core.model.InstalledAppDetail
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApkExporter @Inject constructor() {
    fun exportToUri(detail: InstalledAppDetail, format: ApkExportFormat, targetUri: Uri, contentResolver: ContentResolver): Result<Unit> {
        return try {
            val stream = contentResolver.openOutputStream(targetUri)
                ?: return Result.failure("Could not open output stream")
            stream.use { output -> write(detail, format, output) }
            Result.success(Unit)
        } catch (error: IOException) {
            runCatching { contentResolver.delete(targetUri, null, null) }
            Result.failure(error.message ?: "export_failed", error)
        } catch (error: SecurityException) {
            runCatching { contentResolver.delete(targetUri, null, null) }
            Result.failure(error.message ?: "export_failed", error)
        }
    }

    fun exportToFile(detail: InstalledAppDetail, format: ApkExportFormat, target: File): Result<Unit> = try {
        target.outputStream().use { output -> write(detail, format, output) }
        Result.success(Unit)
    } catch (error: IOException) {
        if (target.exists()) {
            target.delete()
        }
        Result.failure(error.message ?: "export_failed", error)
    } catch (error: SecurityException) {
        if (target.exists()) {
            target.delete()
        }
        Result.failure(error.message ?: "export_failed", error)
    }

    fun write(detail: InstalledAppDetail, format: ApkExportFormat, output: java.io.OutputStream) {
        val apkFiles = resolveApkFiles(detail.apkPaths)
        when (format) {
            ApkExportFormat.APK -> {
                val base = apkFiles.firstOrNull()
                    ?: throw IOException("No base APK for ${detail.packageName}")
                ApkBundleWriter.copyApk(base, output)
            }

            ApkExportFormat.APKS -> {
                ApkBundleWriter.writeStoredZip(apkFiles, output)
            }

            ApkExportFormat.XAPK -> {
                val manifest = XapkManifestBuilder.buildJson(detail).toByteArray(Charsets.UTF_8)
                ApkBundleWriter.writeStoredZip(
                    apkFiles = apkFiles,
                    output = output,
                    extraEntries = mapOf("manifest.json" to manifest),
                )
            }
        }
    }

    private fun resolveApkFiles(paths: List<ApkPathEntry>): List<File> = paths.map { entry ->
        val file = File(entry.path)
        if (!file.isFile || !file.canRead()) {
            throw IOException("APK not readable: ${entry.path}")
        }
        file
    }
}
