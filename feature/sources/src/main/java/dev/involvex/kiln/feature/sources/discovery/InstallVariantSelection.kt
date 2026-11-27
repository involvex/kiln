package dev.involvex.kiln.feature.sources.discovery

import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.ReleaseForge

/**
 * Pure selection helpers for Discover install variant sheets.
 * Empty [nativeCodes] means a universal APK that runs on any ABI.
 */
object InstallVariantSelection {
    const val ABI_AUTO = "auto"
    const val ABI_UNIVERSAL = "universal"

    val KNOWN_ABIS = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")

    fun parseNativeCodesCsv(csv: String?): List<String> = csv.orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }

    fun availableAbiChoices(nativeCodes: List<String>): List<String> {
        val choices = linkedSetOf(ABI_AUTO)
        if (nativeCodes.isEmpty()) {
            choices += ABI_UNIVERSAL
        } else {
            nativeCodes.forEach { choices += it }
            if (nativeCodes.any { it !in KNOWN_ABIS }) {
                // keep unknown codes as selectable chips
            }
        }
        return choices.toList()
    }

    fun availableAbiChoicesFromAssets(assetNames: List<String>): List<String> {
        val found = linkedSetOf(ABI_AUTO)
        var hasUniversal = false
        for (name in assetNames) {
            val abi = abiFromAssetName(name)
            if (abi == null) {
                hasUniversal = true
            } else {
                found += abi
            }
        }
        if (hasUniversal || found.size == 1) {
            found += ABI_UNIVERSAL
        }
        return found.toList()
    }

    fun abiFromAssetName(name: String): String? = KNOWN_ABIS.firstOrNull { name.contains(it, ignoreCase = true) }

    fun isNightlyLabel(tagName: String, releaseName: String?): Boolean {
        val hay = "$tagName ${releaseName.orEmpty()}"
        return hay.contains("nightly", ignoreCase = true)
    }

    /**
     * Prefer a release whose nativecode is empty or intersects [deviceAbis].
     * Among those, prefer a device ABI match over universal when [abiChoice] is auto.
     */
    fun <T> pickRelease(
        releases: List<T>,
        deviceAbis: List<String>,
        abiChoice: String,
        versionCode: (T) -> Long,
        nativeCodes: (T) -> List<String>,
    ): T? {
        if (releases.isEmpty()) return null
        val sorted = releases.sortedByDescending(versionCode)
        return when (abiChoice) {
            ABI_AUTO -> {
                sorted.firstOrNull { release ->
                    val codes = nativeCodes(release)
                    codes.isEmpty() || codes.any { it in deviceAbis }
                } ?: sorted.first()
            }

            ABI_UNIVERSAL -> sorted.firstOrNull { nativeCodes(it).isEmpty() } ?: sorted.firstOrNull()

            else -> sorted.firstOrNull { abiChoice in nativeCodes(it) } ?: sorted.firstOrNull()
        }
    }

    fun pickAssetName(assetNames: List<String>, deviceAbis: List<String>, abiChoice: String): String? {
        if (assetNames.isEmpty()) return null
        return when (abiChoice) {
            ABI_AUTO -> {
                for (abi in deviceAbis) {
                    assetNames.firstOrNull { it.contains(abi, ignoreCase = true) }?.let { return it }
                }
                assetNames.firstOrNull { abiFromAssetName(it) == null } ?: assetNames.first()
            }

            ABI_UNIVERSAL -> assetNames.firstOrNull { abiFromAssetName(it) == null } ?: assetNames.first()

            else -> assetNames.firstOrNull { it.contains(abiChoice, ignoreCase = true) }
                ?: assetNames.firstOrNull()
        }
    }

    /** Collapse same versionCode rows that differ only by ABI into one list entry. */
    fun groupByVersionCode(
        versionCode: Long,
        versionName: String,
        nativeCodes: List<String>,
        existing: MutableMap<Long, VersionGroup>,
        apkName: String,
        apkSha256: String?,
        sizeBytes: Long?,
        whatsNew: String?,
    ) {
        val group = existing.getOrPut(versionCode) {
            VersionGroup(
                versionCode = versionCode,
                versionName = versionName,
                variants = mutableListOf(),
                whatsNew = whatsNew,
            )
        }
        group.variants += ReleaseVariant(
            nativeCodes = nativeCodes,
            apkName = apkName,
            apkSha256 = apkSha256,
            sizeBytes = sizeBytes,
        )
    }
}

data class ReleaseVariant(val nativeCodes: List<String>, val apkName: String, val apkSha256: String?, val sizeBytes: Long?)

data class VersionGroup(val versionCode: Long, val versionName: String, val variants: MutableList<ReleaseVariant>, val whatsNew: String?) {
    fun allNativeCodes(): List<String> = variants.flatMap { it.nativeCodes }.distinct().ifEmpty { emptyList() }

    fun variantForAbi(abiChoice: String, deviceAbis: List<String>): ReleaseVariant? {
        return when (abiChoice) {
            InstallVariantSelection.ABI_AUTO -> {
                for (abi in deviceAbis) {
                    variants.firstOrNull { abi in it.nativeCodes }?.let { return it }
                }
                variants.firstOrNull { it.nativeCodes.isEmpty() } ?: variants.firstOrNull()
            }

            InstallVariantSelection.ABI_UNIVERSAL ->
                variants.firstOrNull { it.nativeCodes.isEmpty() } ?: variants.firstOrNull()

            else -> variants.firstOrNull { abiChoice in it.nativeCodes } ?: variants.firstOrNull()
        }
    }
}

data class GithubReleaseVariantRow(
    val tagName: String,
    val releaseName: String?,
    val prerelease: Boolean,
    val nightly: Boolean,
    val latestStable: Boolean,
    val assets: List<GithubApkAsset>,
    val releaseNotes: String?,
)

data class GithubApkAsset(val name: String, val downloadUrl: String, val sizeBytes: Long)

sealed class InstallVariantTarget {
    data class Fdroid(val packageName: String, val repoId: String, val displayName: String) : InstallVariantTarget()
    data class Github(
        val ownerRepo: String,
        val packageName: String?,
        val displayName: String,
        val forge: ReleaseForge = ReleaseForge.GITHUB,
    ) : InstallVariantTarget()
}

class InstallVariantActions(
    val loadFdroid: suspend (packageName: String, repoId: String) -> Result<FdroidInstallOptions>,
    val installFdroid: suspend (
        packageName: String,
        repoId: String,
        versionCode: Long,
        versionName: String,
        apkName: String,
        apkSha256: String?,
        sizeBytes: Long?,
        releaseNotes: String?,
    ) -> Result<String>,
    val loadGithub: suspend (ownerRepo: String) -> Result<GithubInstallOptions>,
    val installGithub: suspend (
        packageName: String,
        ownerRepo: String,
        tagName: String,
        downloadUrl: String,
        sizeBytes: Long?,
        releaseNotes: String?,
        forge: ReleaseForge,
    ) -> Result<String>,
)
