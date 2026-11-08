package dev.involvex.kiln.feature.apps.export

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Writes single APKs or STORED zip bundles (.apks / .xapk). Pure JVM — unit-tested with temp files.
 */
object ApkBundleWriter {
    fun copyApk(source: File, output: OutputStream) {
        require(source.isFile && source.canRead()) { "APK source is not readable: ${source.path}" }
        source.inputStream().use { input ->
            input.copyTo(output)
        }
        output.flush()
    }

    /**
     * ZIP of APK files using [ZipEntry.STORED] (no compression). Entry names are the file names.
     * Optional [extraEntries] (e.g. `manifest.json` for XAPK) are also stored uncompressed.
     */
    fun writeStoredZip(apkFiles: List<File>, output: OutputStream, extraEntries: Map<String, ByteArray> = emptyMap()) {
        require(apkFiles.isNotEmpty()) { "at least one APK is required" }
        ZipOutputStream(output).use { zip ->
            // Disable default DEFLATED for entries we mark STORED.
            zip.setMethod(ZipOutputStream.STORED)
            for (file in apkFiles) {
                require(file.isFile && file.canRead()) { "APK not readable: ${file.path}" }
                putStoredFile(zip, file.name, file)
            }
            for ((name, bytes) in extraEntries) {
                putStoredBytes(zip, name, bytes)
            }
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

    fun copyStream(input: InputStream, output: OutputStream) {
        input.copyTo(output)
        output.flush()
    }
}
