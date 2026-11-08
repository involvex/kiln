package dev.involvex.kiln.core.model

/**
 * Lightweight row for the installed-apps list.
 *
 * [firstInstallTime] is included so sort-by-install-date does not require a second
 * PackageManager pass (PackageInfo already exposes it during the summary query).
 */
data class InstalledAppSummary(
    val packageName: String,
    val label: String,
    val versionName: String?,
    val versionCode: Long,
    val isSystem: Boolean,
    val isUpdatedSystemApp: Boolean,
    val apkSizeBytes: Long?,
    val hasSplits: Boolean,
    val lastUpdateTime: Long,
    val firstInstallTime: Long,
)

data class ApkPathEntry(
    val path: String,
    val sizeBytes: Long?,
    /** Null for the base APK; otherwise the split name from PackageManager. */
    val splitId: String?,
)

data class AppPermissionEntry(val name: String, val granted: Boolean, val isDangerous: Boolean)

/**
 * Full package details loaded lazily for the detail screen / export.
 */
data class InstalledAppDetail(
    val packageName: String,
    val label: String,
    val versionName: String?,
    val versionCode: Long,
    val isSystem: Boolean,
    val isUpdatedSystemApp: Boolean,
    val apkSizeBytes: Long?,
    val hasSplits: Boolean,
    val lastUpdateTime: Long,
    val firstInstallTime: Long,
    val installerPackage: String?,
    val minSdk: Int,
    val targetSdk: Int,
    val apkPaths: List<ApkPathEntry>,
    val signerSha256: List<String>,
    val requestedPermissions: List<AppPermissionEntry>,
    val hasLaunchIntent: Boolean,
) {
    fun toSummary(): InstalledAppSummary = InstalledAppSummary(
        packageName = packageName,
        label = label,
        versionName = versionName,
        versionCode = versionCode,
        isSystem = isSystem,
        isUpdatedSystemApp = isUpdatedSystemApp,
        apkSizeBytes = apkSizeBytes,
        hasSplits = hasSplits,
        lastUpdateTime = lastUpdateTime,
        firstInstallTime = firstInstallTime,
    )

    val canUninstall: Boolean
        get() = !isSystem && !isUpdatedSystemApp
}

enum class AppListFilter {
    USER,
    SYSTEM,
    ALL,

    /** Only apps with a known available update (managed + not ignored). */
    UPDATES,
}

enum class AppSortField {
    NAME,
    SIZE,
    INSTALL_DATE,
    UPDATE_DATE,
}

enum class SortDirection {
    ASCENDING,
    DESCENDING,
}

enum class ApkExportFormat {
    APK,
    APKS,
    XAPK,
}
