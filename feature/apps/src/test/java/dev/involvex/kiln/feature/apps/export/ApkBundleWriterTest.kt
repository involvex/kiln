package dev.involvex.kiln.feature.apps.export

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ApkBundleWriterTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `copies single apk bytes`() {
        val source = File(tempDir, "base.apk").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        val out = ByteArrayOutputStream()
        ApkBundleWriter.copyApk(source, out)
        assertEquals(listOf<Byte>(1, 2, 3, 4), out.toByteArray().toList())
    }

    @Test
    fun `writes stored zip with original names and extra manifest`() {
        val base = File(tempDir, "base.apk").apply { writeText("BASE") }
        val split = File(tempDir, "split_config.apk").apply { writeText("SPLIT") }
        val out = ByteArrayOutputStream()
        ApkBundleWriter.writeStoredZip(
            apkFiles = listOf(base, split),
            output = out,
            extraEntries = mapOf("manifest.json" to """{"xapk_version":2}""".toByteArray()),
        )
        val names = mutableListOf<String>()
        val methods = mutableListOf<Int>()
        ZipInputStream(out.toByteArray().inputStream()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                names += entry.name
                methods += entry.method
                zip.closeEntry()
            }
        }
        assertEquals(listOf("base.apk", "split_config.apk", "manifest.json"), names)
        assertTrue(methods.all { it == java.util.zip.ZipEntry.STORED })
    }
}
