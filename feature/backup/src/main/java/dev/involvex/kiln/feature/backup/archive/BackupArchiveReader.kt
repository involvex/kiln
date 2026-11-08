package dev.involvex.kiln.feature.backup.archive

import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.BackupArchiveMetadata
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream
import kotlinx.serialization.SerializationException

data class BackupExtractResult(val metadata: BackupArchiveMetadata, val baseApk: File, val allApkFiles: List<File>)

object BackupArchiveReader {
    fun extract(input: InputStream, outputDir: File): Result<BackupExtractResult> {
        if (!outputDir.exists()) outputDir.mkdirs()
        return try {
            val (metadata, apkFiles) = readEntries(input, outputDir)
            val meta = metadata ?: return Result.failure("metadata_missing")
            if (apkFiles.isEmpty()) return Result.failure("apk_missing")
            val base = pickBaseApk(apkFiles) ?: return Result.failure("apk_missing")
            Result.success(BackupExtractResult(meta, base, apkFiles))
        } catch (e: IOException) {
            Result.failure(e.message ?: "extract_failed", e)
        } catch (e: SerializationException) {
            Result.failure(e.message ?: "extract_failed", e)
        } catch (e: IllegalArgumentException) {
            Result.failure(e.message ?: "extract_failed", e)
        }
    }

    private fun readEntries(input: InputStream, outputDir: File): Pair<BackupArchiveMetadata?, List<File>> {
        var metadata: BackupArchiveMetadata? = null
        val apkFiles = ArrayList<File>()
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name.substringAfterLast('/').ifBlank { entry.name }
                when {
                    name == BackupMetadataCodec.FILE_NAME -> {
                        metadata = BackupMetadataCodec.decode(zip.readBytes())
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
        return metadata to apkFiles
    }

    private fun pickBaseApk(apkFiles: List<File>): File? = apkFiles.firstOrNull { it.name.equals("base.apk", ignoreCase = true) }
        ?: apkFiles.minByOrNull { it.name.length }
}
