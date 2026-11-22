package dev.involvex.kiln.navigation

import kotlinx.serialization.Serializable

@Serializable
data object AppsKey

@Serializable
data class AppDetailKey(val packageName: String)

@Serializable
data object SourcesKey

@Serializable
data object SourcesReposKey

@Serializable
data object SourcesWatchlistKey

@Serializable
data object SourcesStarsKey

@Serializable
data object SourcesImportKey

@Serializable
data object AutoMatchKey

@Serializable
data object StorageKey

@Serializable
data object BackupKey

@Serializable
data object ManageKey

@Serializable
data object SettingsKey

@Serializable
data object SettingsAppearanceKey

@Serializable
data object SettingsUpdatesKey

@Serializable
data object SettingsIgnoredUpdatesKey

@Serializable
data object SettingsNetworkKey

@Serializable
data object SettingsInstallationKey

@Serializable
data object SettingsAutomationKey

@Serializable
data object SettingsMaintenanceKey

@Serializable
data object SettingsToolsKey

@Serializable
data object SettingsDataKey

@Serializable
data object SettingsAboutKey

@Serializable
data class CatalogDetailKey(val packageName: String, val repoId: String)

@Serializable
data object PrivilegeSetupKey

@Serializable
data object DebloatKey

@Serializable
data object PermissionAuditKey

@Serializable
data object BackgroundControlsKey

@Serializable
data object FreezeKey

@Serializable
data object TermuxKey

@Serializable
data object DiscoveryKey

@Serializable
data object ActivityLogKey
