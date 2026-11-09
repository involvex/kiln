package dev.involvex.kiln.navigation

import kotlinx.serialization.Serializable

@Serializable
data object AppsKey

@Serializable
data class AppDetailKey(val packageName: String)

@Serializable
data object SourcesKey

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
data class CatalogDetailKey(val packageName: String, val repoId: String)

@Serializable
data object PrivilegeSetupKey

@Serializable
data object DebloatKey

@Serializable
data object TermuxKey

@Serializable
data object DiscoveryKey

@Serializable
data object ActivityLogKey
