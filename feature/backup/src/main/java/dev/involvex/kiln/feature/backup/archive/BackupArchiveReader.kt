package dev.involvex.kiln.feature.backup.archive

import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.BackupArchiveMetadata
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream
import kotlinx.serialization.SerializationException

data class BackupExtractResult(
    val metadata: BackupArchiveMetadata,
    val baseApk: File,
    val allApkFiles: List<File>,
    val androidDataTar: File?,
    val androidObbTar: File?,
)

object BackupArchiveReader {
    fun extract(input: InputStream, outputDir: File): Result<BackupExtractResult> {
        if (!outputDir.exists()) outputDir.mkdirs()
        return try {
            val parsed = readEntries(input, outputDir)
            val meta = parsed.metadata ?: return Result.failure("metadata_missing")
            if (parsed.apkFiles.isEmpty()) return Result.failure("apk_missing")
            val base = pickBaseApk(parsed.apkFiles) ?: return Result.failure("apk_missing")
            Result.success(
                BackupExtractResult(
                    metadata = meta,
                    baseApk = base,
                    allApkFiles = parsed.apkFiles,
                    androidDataTar = parsed.androidDataTar,
                    androidObbTar = parsed.androidObbTar,
                ),
            )
        } catch (e: IOException) {
            Result.failure(e.message ?: "extract_failed", e)
        } catch (e: SerializationException) {
            Result.failure(e.message ?: "extract_failed", e)
        } catch (e: IllegalArgumentException) {
            Result.failure(e.message ?: "extract_failed", e)
        }
    }

    private data class Parsed(
        val metadata: BackupArchiveMetadata?,
        val apkFiles: List<File>,
        val androidDataTar: File?,
        val androidObbTar: File?,
    )

    private fun readEntries(input: InputStream, outputDir: File): Parsed {
        var metadata: BackupArchiveMetadata? = null
        val apkFiles = ArrayList<File>()
        var androidDataTar: File? = null
        var androidObbTar: File? = null
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name.substringAfterLast('/').ifBlank { entry.name }
                when {
                    name == BackupMetadataCodec.FILE_NAME -> {
                        metadata = BackupMetadataCodec.decode(zip.readBytes())
                    }

                    name.equals(BackupMetadataCodec.ANDROID_DATA_ENTRY, ignoreCase = true) -> {
                        val out = File(outputDir, BackupMetadataCodec.ANDROID_DATA_ENTRY)
                        out.outputStream().use { zip.copyTo(it) }
                        androidDataTar = out
                    }

                    name.equals(BackupMetadataCodec.ANDROID_OBB_ENTRY, ignoreCase = true) -> {
                        val out = File(outputDir, BackupMetadataCodec.ANDROID_OBB_ENTRY)
                        out.outputStream().use { zip.copyTo(it) }
                        androidObbTar = out
                    }

                    name.endsWith(".apk", ignoreCase = true) -> {
                        val out = File(outputDir, name)
                        out.outputStream().use { zip.copyTo(it) }
                        apkFiles.add(out)
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return Parsed(metadata, apkFiles, androidDataTar, androidObbTar)
    }

    private fun pickBaseApk(apkFiles: List<File>): File? = apkFiles.firstOrNull { it.name.equals("base.apk", ignoreCase = true) }
        ?: apkFiles.minByOrNull { it.name.length }
}
