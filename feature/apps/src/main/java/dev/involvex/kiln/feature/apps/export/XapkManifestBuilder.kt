package dev.involvex.kiln.feature.apps.export

import dev.involvex.kiln.core.model.ApkPathEntry
import dev.involvex.kiln.core.model.InstalledAppDetail

/**
 * Builds XAPK `manifest.json` (xapk_version = 2). No OBB fields in M1.
 *
 * Deviations vs typical SAI/APKPure XAPKs are documented in the M1 report
 * (no locales/icons/expansions; version_code as String per Kiln prompt).
 */
object XapkManifestBuilder {
    fun buildJson(detail: InstalledAppDetail): String {
        val permissions = detail.requestedPermissions.joinToString(separator = ",") { "\"${escape(it.name)}\"" }
        val splits = detail.apkPaths.joinToString(separator = ",") { entry ->
            val fileName = entry.path.substringAfterLast('/').substringAfterLast('\\')
            val id = entry.splitId ?: "base"
            """{"file":"${escape(fileName)}","id":"${escape(id)}"}"""
        }
        val totalSize = detail.apkPaths.mapNotNull { it.sizeBytes }.sum()
        return buildString {
            append("{\n")
            append("""  "xapk_version": 2,""").append('\n')
            append("""  "package_name": "${escape(detail.packageName)}",""").append('\n')
            append("""  "name": "${escape(detail.label)}",""").append('\n')
            // Prompt requires version_code as String.
            append("""  "version_code": "${detail.versionCode}",""").append('\n')
            append("""  "version_name": "${escape(detail.versionName ?: "")}",""").append('\n')
            append("""  "min_sdk_version": "${detail.minSdk}",""").append('\n')
            append("""  "target_sdk_version": "${detail.targetSdk}",""").append('\n')
            append("""  "permissions": [$permissions],""").append('\n')
            append("""  "total_size": $totalSize,""").append('\n')
            append("""  "split_apks": [$splits]""").append('\n')
            append('}')
        }
    }

    fun buildJson(
        packageName: String,
        name: String,
        versionCode: Long,
        versionName: String?,
        minSdk: Int,
        targetSdk: Int,
        permissions: List<String>,
        apkPaths: List<ApkPathEntry>,
    ): String {
        val detail = InstalledAppDetail(
            packageName = packageName,
            label = name,
            versionName = versionName,
            versionCode = versionCode,
            isSystem = false,
            isUpdatedSystemApp = false,
            apkSizeBytes = null,
            hasSplits = apkPaths.size > 1,
            lastUpdateTime = 0L,
            firstInstallTime = 0L,
            installerPackage = null,
            minSdk = minSdk,
            targetSdk = targetSdk,
            apkPaths = apkPaths,
            signerSha256 = emptyList(),
            requestedPermissions = permissions.map {
                dev.involvex.kiln.core.model.AppPermissionEntry(
                    name = it,
                    granted = false,
                    isDangerous = false,
                )
            },
            hasLaunchIntent = false,
        )
        return buildJson(detail)
    }

    private fun escape(value: String): String = buildString(value.length) {
        for (ch in value) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
    }
}
