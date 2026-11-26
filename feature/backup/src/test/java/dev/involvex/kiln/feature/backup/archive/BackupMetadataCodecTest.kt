package dev.involvex.kiln.feature.backup.archive

import dev.involvex.kiln.core.model.BackupArchiveMetadata
import dev.involvex.kiln.core.model.BackupContentTier
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BackupMetadataCodecTest {
    @Test
    fun encode_decode_round_trip() {
        val original = BackupArchiveMetadata(
            packageName = "dev.involvex.demo",
            label = "Demo",
            versionName = "0.1.0",
            versionCode = 1,
            signerSha256 = listOf("AB:CD"),
            tier = BackupContentTier.NONE,
            createdAtEpochMs = 99L,
            includesAppData = false,
            includesAndroidData = false,
            apkEntries = listOf("base.apk"),
            hasSplits = false,
            targetUserId = 10,
        )
        val decoded = BackupMetadataCodec.decode(BackupMetadataCodec.encode(original))
        assertEquals(original, decoded)
    }

    @Test
    fun decode_without_targetUserId_defaults_null() {
        val legacy = """
            {
              "formatVersion": 1,
              "packageName": "dev.involvex.demo",
              "label": "Demo",
              "versionCode": 1,
              "tier": "NONE",
              "createdAtEpochMs": 1,
              "includesAppData": false,
              "includesAndroidData": false,
              "apkEntries": ["base.apk"],
              "hasSplits": false
            }
        """.trimIndent().toByteArray(Charsets.UTF_8)
        val decoded = BackupMetadataCodec.decode(legacy)
        assertEquals(null, decoded.targetUserId)
        assertEquals("dev.involvex.demo", decoded.packageName)
    }
}
