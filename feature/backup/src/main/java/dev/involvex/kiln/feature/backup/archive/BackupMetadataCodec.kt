package dev.involvex.kiln.feature.backup.archive

import dev.involvex.kiln.core.model.BackupArchiveMetadata
import dev.involvex.kiln.core.model.BackupContentTier
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object BackupMetadataCodec {
    const val FILE_NAME = "metadata.json"
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun encode(metadata: BackupArchiveMetadata): ByteArray =
        json.encodeToString(BackupMetadataDto.serializer(), BackupMetadataDto.from(metadata))
            .toByteArray(Charsets.UTF_8)

    fun decode(bytes: ByteArray): BackupArchiveMetadata =
        json.decodeFromString(BackupMetadataDto.serializer(), bytes.decodeToString()).toModel()

    @Serializable
    data class BackupMetadataDto(
        val formatVersion: Int = 1,
        val packageName: String,
        val label: String,
        val versionName: String? = null,
        val versionCode: Long,
        val signerSha256: List<String> = emptyList(),
        val tier: String,
        val createdAtEpochMs: Long,
        val includesAppData: Boolean = false,
        val includesAndroidData: Boolean = false,
        val apkEntries: List<String> = emptyList(),
        val hasSplits: Boolean = false,
    ) {
        fun toModel(): BackupArchiveMetadata = BackupArchiveMetadata(
            formatVersion = formatVersion,
            packageName = packageName,
            label = label,
            versionName = versionName,
            versionCode = versionCode,
            signerSha256 = signerSha256,
            tier = runCatching { BackupContentTier.valueOf(tier) }.getOrDefault(BackupContentTier.NONE),
            createdAtEpochMs = createdAtEpochMs,
            includesAppData = includesAppData,
            includesAndroidData = includesAndroidData,
            apkEntries = apkEntries,
            hasSplits = hasSplits,
        )

        companion object {
            fun from(metadata: BackupArchiveMetadata) = BackupMetadataDto(
                formatVersion = metadata.formatVersion,
                packageName = metadata.packageName,
                label = metadata.label,
                versionName = metadata.versionName,
                versionCode = metadata.versionCode,
                signerSha256 = metadata.signerSha256,
                tier = metadata.tier.name,
                createdAtEpochMs = metadata.createdAtEpochMs,
                includesAppData = metadata.includesAppData,
                includesAndroidData = metadata.includesAndroidData,
                apkEntries = metadata.apkEntries,
                hasSplits = metadata.hasSplits,
            )
        }
    }
}
