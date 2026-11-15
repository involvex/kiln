package dev.involvex.kiln.feature.apps.apktools

import dev.involvex.kiln.core.model.ApkArchiveKind
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class UniversalApkMergerTest {
    @Test
    fun isMergeableKind_acceptsBundlesOnly() {
        assertTrue(UniversalApkMerger.isMergeableKind(ApkArchiveKind.APKS))
        assertTrue(UniversalApkMerger.isMergeableKind(ApkArchiveKind.XAPK))
        assertTrue(UniversalApkMerger.isMergeableKind(ApkArchiveKind.APKM))
        assertFalse(UniversalApkMerger.isMergeableKind(ApkArchiveKind.APK))
        assertFalse(UniversalApkMerger.isMergeableKind(ApkArchiveKind.UNKNOWN))
    }

    @Test
    fun kindOf_detectsApkm() {
        assertEquals(ApkArchiveKind.APKM, ApkArchiveReader.kindOf("app.apkm"))
        assertEquals(ApkArchiveKind.APKM, ApkArchiveReader.kindOf("APP.APKM"))
    }

    @Test
    fun mergeBundleToApk_rejectsPlainApkExtensionViaKind(@TempDir dir: File) {
        // Plain .apk is not mergeable; repository gates on kind before calling merge.
        assertFalse(UniversalApkMerger.isMergeableKind(ApkArchiveReader.kindOf("single.apk")))
        val fake = File(dir, "not-a-bundle.apk")
        fake.writeBytes(byteArrayOf(0x50, 0x4B, 0x03, 0x04))
        val work = File(dir, "work").apply { mkdirs() }
        val out = File(dir, "out.apk")
        // Zip with no .apk members → extract fails with bundle_no_apk
        val emptyZip = File(dir, "empty.apks")
        ZipOutputStream(emptyZip.outputStream()).use { zos ->
            zos.putNextEntry(ZipEntry("readme.txt"))
            zos.write("no apk".toByteArray())
            zos.closeEntry()
        }
        val result = UniversalApkMerger.mergeBundleToApk(emptyZip, work, out)
        assertTrue(result.isFailure)
        assertEquals("bundle_no_apk", result.exceptionOrNull()?.message)
    }
}
