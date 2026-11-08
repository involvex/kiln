package dev.involvex.kiln.feature.backup.archive

import dev.involvex.kiln.core.model.BackupArchiveMetadata
import dev.involvex.kiln.core.model.BackupContentTier
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class BackupArchiveRoundTripTest {
    @TempDir
    lateinit var temp: File

    @Test
    fun zip_round_trip_preserves_metadata_and_apk() {
        val apk = File(temp, "base.apk").apply { writeBytes(byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x00)) }
        val metadata = BackupArchiveMetadata(
            packageName = "com.example.app",
            label = "Example",
            versionName = "1.2.3",
            versionCode = 123,
            signerSha256 = listOf("AA:BB"),
            tier = BackupContentTier.NONE,
            createdAtEpochMs = 42L,
            includesAppData = false,
            includesAndroidData = false,
            apkEntries = listOf("base.apk"),
            hasSplits = false,
        )
        val bytes = ByteArrayOutputStream().use { out ->
            BackupArchiveWriter.write(listOf(apk), metadata, out)
            out.toByteArray()
        }
        val extractDir = File(temp, "out").apply { mkdirs() }
        val result = BackupArchiveReader.extract(ByteArrayInputStream(bytes), extractDir)
        assertTrue(result.isSuccess)
        val extracted = result.getOrNull()!!
        assertEquals("com.example.app", extracted.metadata.packageName)
        assertEquals(123L, extracted.metadata.versionCode)
        assertTrue(extracted.baseApk.isFile)
        assertEquals(apk.length(), extracted.baseApk.length())
    }
}
