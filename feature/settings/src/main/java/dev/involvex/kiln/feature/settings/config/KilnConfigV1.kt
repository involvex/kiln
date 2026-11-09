package dev.involvex.kiln.feature.settings.config

import kotlinx.serialization.Serializable

@Serializable
data class KilnConfigV1(
    val schemaVersion: Int = KilnConfigCodec.SCHEMA_VERSION,
    val exportedAtEpochMs: Long = 0L,
    val kilnVersionName: String = "",
    val kilnVersionCode: Long = 0L,
    val repos: List<KilnConfigRepoV1> = emptyList(),
    val managedApps: List<KilnConfigManagedAppV1> = emptyList(),
    val preferences: KilnConfigPreferencesV1 = KilnConfigPreferencesV1(),
    val hints: KilnConfigHintsV1 = KilnConfigHintsV1(),
)

@Serializable
data class KilnConfigRepoV1(
    val id: String,
    val name: String,
    val address: String,
    val fingerprint: String = "",
    val kind: String = "FDROID",
    val enabled: Boolean = true,
    val priority: Int = 0,
)

@Serializable
data class KilnConfigManagedAppV1(
    val packageName: String,
    val sourceId: String,
    val confidence: String = "MEDIUM",
    val pinned: Boolean = false,
    val ignoreUpdates: Boolean = false,
    val githubOwnerRepo: String? = null,
    val githubAssetRegex: String? = null,
    val githubChannel: String = "STABLE",
    val watchOnly: Boolean = false,
)

@Serializable
data class KilnConfigPreferencesV1(
    val backgroundUpdates: KilnConfigBackgroundUpdatesV1? = null,
    val automation: KilnConfigAutomationV1? = null,
    val maintenance: KilnConfigMaintenanceV1? = null,
    val appearance: KilnConfigAppearanceV1? = null,
    val installer: KilnConfigInstallerV1? = null,
    val termux: KilnConfigTermuxV1? = null,
)

@Serializable
data class KilnConfigBackgroundUpdatesV1(
    val enabled: Boolean = false,
    val unmeteredOnly: Boolean = true,
    val autoInstallMatching: Boolean = false,
    val updateCooldownDays: Int = 0,
)

@Serializable
data class KilnConfigAutomationV1(val enabled: Boolean = false)

@Serializable
data class KilnConfigMaintenanceV1(
    val enabled: Boolean = false,
    val trimCaches: Boolean = true,
    val pruneDownloads: Boolean = true,
    val pruneRollback: Boolean = true,
    val pruneBackupTemp: Boolean = true,
)

@Serializable
data class KilnConfigAppearanceV1(val theme: String = "DARK", val language: String = "SYSTEM", val fontStyle: String = "DEFAULT")

@Serializable
data class KilnConfigInstallerV1(val preferred: String = "SYSTEM")

@Serializable
data class KilnConfigTermuxV1(val enabled: Boolean = false, val packageId: String = "com.termux")

@Serializable
data class KilnConfigHintsV1(val githubPatWasConfigured: Boolean = false)
