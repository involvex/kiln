package dev.involvex.kiln.feature.apps.apktools

import dev.involvex.kiln.core.model.ApkArchiveKind
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ApkArchiveReaderTest {
    @Test
    fun kindOf_detectsExtensions() {
        assertEquals(ApkArchiveKind.APK, ApkArchiveReader.kindOf("app.apk"))
        assertEquals(ApkArchiveKind.APKS, ApkArchiveReader.kindOf("app.APKS"))
        assertEquals(ApkArchiveKind.XAPK, ApkArchiveReader.kindOf("bundle.xapk"))
        assertEquals(ApkArchiveKind.UNKNOWN, ApkArchiveReader.kindOf("notes.txt"))
    }

    @Test
    fun pickBaseApkMember_prefersBaseApk() {
        val members = listOf("config.xxhdpi.apk", "base.apk", "config.arm64_v8a.apk")
        assertEquals("base.apk", ApkArchiveReader.pickBaseApkMember(members))
    }

    @Test
    fun pickBaseApkMember_fallsBackToShortest() {
        val members = listOf("config.xxhdpi.apk", "a.apk", "config.arm64_v8a.apk")
        assertEquals("a.apk", ApkArchiveReader.pickBaseApkMember(members))
    }

    @Test
    fun pickBaseApkMember_nullWhenNoApk() {
        assertNull(ApkArchiveReader.pickBaseApkMember(listOf("manifest.json")))
    }

    @Test
    fun listZipMembers_returnsSortedEntries(@TempDir dir: File) {
        val zip = File(dir, "sample.apks")
        ZipOutputStream(zip.outputStream()).use { out ->
            out.putNextEntry(ZipEntry("z.apk"))
            out.write(byteArrayOf(1))
            out.closeEntry()
            out.putNextEntry(ZipEntry("base.apk"))
            out.write(byteArrayOf(2))
            out.closeEntry()
        }
        assertEquals(listOf("base.apk", "z.apk"), ApkArchiveReader.listZipMembers(zip))
    }
}
