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
        )
        val decoded = BackupMetadataCodec.decode(BackupMetadataCodec.encode(original))
        assertEquals(original, decoded)
    }
}
