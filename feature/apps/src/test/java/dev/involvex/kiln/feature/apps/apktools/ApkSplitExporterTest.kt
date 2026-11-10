package dev.involvex.kiln.feature.apps.apktools

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ApkSplitExporterTest {
    @Test
    fun apkMemberNames_filtersApkEntriesOnly(@TempDir dir: File) {
        val zip = File(dir, "sample.apks")
        ZipOutputStream(zip.outputStream()).use { out ->
            out.putNextEntry(ZipEntry("manifest.json"))
            out.write(byteArrayOf(1))
            out.closeEntry()
            out.putNextEntry(ZipEntry("base.apk"))
            out.write(byteArrayOf(2))
            out.closeEntry()
            out.putNextEntry(ZipEntry("config.arm64_v8a.apk"))
            out.write(byteArrayOf(3))
            out.closeEntry()
        }
        assertEquals(
            listOf("base.apk", "config.arm64_v8a.apk"),
            ApkSplitExporter.apkMemberNames(zip),
        )
    }

    @Test
    fun extractApkMembers_writesFiles(@TempDir dir: File) {
        val zip = File(dir, "sample.xapk")
        ZipOutputStream(zip.outputStream()).use { out ->
            out.putNextEntry(ZipEntry("base.apk"))
            out.write(byteArrayOf(9, 9))
            out.closeEntry()
            out.putNextEntry(ZipEntry("config.apk"))
            out.write(byteArrayOf(8))
            out.closeEntry()
        }
        val outDir = File(dir, "out").apply { mkdirs() }
        val result = ApkSplitExporter.extractApkMembers(zip, outDir)
        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrNull())
        assertTrue(File(outDir, "base.apk").isFile)
        assertTrue(File(outDir, "config.apk").isFile)
    }

    @Test
    fun extractApkMembers_failsWithoutApk(@TempDir dir: File) {
        val zip = File(dir, "empty.apks")
        ZipOutputStream(zip.outputStream()).use { out ->
            out.putNextEntry(ZipEntry("readme.txt"))
            out.write(byteArrayOf(1))
            out.closeEntry()
        }
        val outDir = File(dir, "out").apply { mkdirs() }
        val result = ApkSplitExporter.extractApkMembers(zip, outDir)
        assertTrue(result.isFailure)
        assertEquals("bundle_no_apk", result.exceptionOrNull()?.message)
    }
}
