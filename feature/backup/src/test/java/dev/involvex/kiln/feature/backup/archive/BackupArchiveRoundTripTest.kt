package dev.involvex.kiln.feature.backup.archive

import dev.involvex.kiln.core.model.BackupArchiveMetadata
import dev.involvex.kiln.core.model.BackupContentTier
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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

    @Test
    fun zip_round_trip_extracts_all_split_apks_ordered_for_restore() {
        val base = File(temp, "base.apk").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val split = File(temp, "config.xxhdpi.apk").apply { writeBytes(byteArrayOf(4, 5)) }
        val metadata = BackupArchiveMetadata(
            packageName = "com.example.split",
            label = "Split",
            versionName = "2.0",
            versionCode = 2,
            signerSha256 = listOf("CC:DD"),
            tier = BackupContentTier.NONE,
            createdAtEpochMs = 7L,
            includesAppData = false,
            includesAndroidData = false,
            apkEntries = listOf("base.apk", "config.xxhdpi.apk"),
            hasSplits = true,
        )
        val bytes = ByteArrayOutputStream().use { out ->
            BackupArchiveWriter.write(listOf(base, split), metadata, out)
            out.toByteArray()
        }
        val extractDir = File(temp, "split-out").apply { mkdirs() }
        val extracted = BackupArchiveReader.extract(ByteArrayInputStream(bytes), extractDir).getOrNull()!!
        assertEquals(2, extracted.allApkFiles.size)
        assertFalse(BackupRestoreApks.isIncompleteSplitArchive(extracted.metadata.hasSplits, extracted.allApkFiles.size))
        val paths = BackupRestoreApks.orderedAbsolutePaths(extracted.baseApk, extracted.allApkFiles)
        assertEquals(extracted.baseApk.absolutePath, paths.first())
        assertEquals(2, paths.size)
    }

    @Test
    fun zip_round_trip_preserves_app_data_tar() {
        val apk = File(temp, "base.apk").apply { writeBytes(byteArrayOf(0x50, 0x4B, 0x03, 0x04)) }
        val appData = File(temp, "app_data.tar").apply { writeBytes(byteArrayOf(1, 2, 3, 4, 5)) }
        val metadata = BackupArchiveMetadata(
            packageName = "com.example.root",
            label = "RootApp",
            versionName = "1.0",
            versionCode = 1,
            signerSha256 = listOf("EE:FF"),
            tier = BackupContentTier.ROOT,
            createdAtEpochMs = 99L,
            includesAppData = true,
            includesAndroidData = false,
            apkEntries = listOf("base.apk"),
            hasSplits = false,
            appDataEntry = BackupMetadataCodec.APP_DATA_ENTRY,
        )
        val bytes = ByteArrayOutputStream().use { out ->
            BackupArchiveWriter.write(listOf(apk), metadata, out, appDataTar = appData)
            out.toByteArray()
        }
        val extractDir = File(temp, "app-data-out").apply { mkdirs() }
        val extracted = BackupArchiveReader.extract(ByteArrayInputStream(bytes), extractDir).getOrNull()!!
        assertTrue(extracted.metadata.includesAppData)
        assertEquals(BackupMetadataCodec.APP_DATA_ENTRY, extracted.metadata.appDataEntry)
        assertTrue(extracted.appDataTar!!.isFile)
        assertEquals(appData.length(), extracted.appDataTar!!.length())
    }
}
