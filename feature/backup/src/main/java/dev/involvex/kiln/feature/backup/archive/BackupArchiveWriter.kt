package dev.involvex.kiln.feature.backup.archive

import dev.involvex.kiln.core.model.BackupArchiveMetadata
import java.io.File
import java.io.OutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Writes a Kiln backup zip: STORED APK entries + [BackupMetadataCodec.FILE_NAME].
 * Pure JVM — unit-tested with temp files.
 */
object BackupArchiveWriter {
    fun write(apkFiles: List<File>, metadata: BackupArchiveMetadata, output: OutputStream) {
        require(apkFiles.isNotEmpty()) { "at least one APK required" }
        val metaBytes = BackupMetadataCodec.encode(metadata)
        ZipOutputStream(output).use { zip ->
            zip.setMethod(ZipOutputStream.STORED)
            for (file in apkFiles) {
                require(file.isFile && file.canRead()) { "APK not readable: ${file.path}" }
                putStoredFile(zip, file.name, file)
            }
            putStoredBytes(zip, BackupMetadataCodec.FILE_NAME, metaBytes)
        }
    }

    private fun putStoredFile(zip: ZipOutputStream, entryName: String, file: File) {
        val crc = CRC32()
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                crc.update(buffer, 0, read)
            }
        }
        val entry = ZipEntry(entryName).apply {
            method = ZipEntry.STORED
            size = file.length()
            compressedSize = file.length()
            this.crc = crc.value
            time = file.lastModified()
        }
        zip.putNextEntry(entry)
        file.inputStream().use { input -> input.copyTo(zip) }
        zip.closeEntry()
    }

    private fun putStoredBytes(zip: ZipOutputStream, entryName: String, bytes: ByteArray) {
        val crc = CRC32().apply { update(bytes) }
        val entry = ZipEntry(entryName).apply {
            method = ZipEntry.STORED
            size = bytes.size.toLong()
            compressedSize = bytes.size.toLong()
            this.crc = crc.value
        }
        zip.putNextEntry(entry)
        zip.write(bytes)
        zip.closeEntry()
    }
}
