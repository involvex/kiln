package dev.involvex.kiln.core.common

import java.util.Locale
import kotlin.math.abs

/**
 * Picks which APK members from a split bundle to install for a given device.
 * Pure logic — callers pass ABI / density / locales (no Android framework types).
 */
object SplitApkSelector {
    data class DeviceProfile(
        /** Preferred ABI order, e.g. `arm64-v8a`, `armeabi-v7a`. */
        val abis: List<String>,
        val densityDpi: Int,
        /** BCP-47-ish tags, most preferred first (`de`, `en-US`). */
        val languageTags: List<String>,
    )

    data class Selection(val members: List<String>, val errorCode: String? = null)

    private val ABI_TOKENS = listOf(
        "arm64-v8a",
        "armeabi-v7a",
        "armeabi",
        "x86_64",
        "x86",
        "mips64",
        "mips",
    )

    private val DENSITY_BY_NAME = mapOf(
        "ldpi" to 120,
        "mdpi" to 160,
        "tvdpi" to 213,
        "hdpi" to 240,
        "xhdpi" to 320,
        "xxhdpi" to 480,
        "xxxhdpi" to 640,
    )

    fun select(memberNames: List<String>, device: DeviceProfile): Selection {
        val apks = memberNames
            .map { it.replace('\\', '/') }
            .filter { it.substringAfterLast('/').endsWith(".apk", ignoreCase = true) }
            .distinct()
        if (apks.isEmpty()) {
            return Selection(emptyList(), "bundle_no_apk")
        }
        val base = findBase(apks) ?: return Selection(emptyList(), "bundle_no_base")
        val others = apks.filterNot { it.equals(base, ignoreCase = true) }
        val classified = others.map { it to classify(configToken(it.substringAfterLast('/'))) }

        val abiSplits = classified.filter { it.second is Kind.Abi }
        val densitySplits = classified.filter { it.second is Kind.Density }
        val localeSplits = classified.filter { it.second is Kind.Locale }
        val otherSplits = classified.filter { it.second is Kind.Other }

        val chosen = ArrayList<String>()
        chosen += base

        if (abiSplits.isNotEmpty()) {
            val match = pickBestAbi(abiSplits, device.abis)
                ?: return Selection(emptyList(), "no_compatible_abi")
            chosen += match
        }

        pickBestDensity(densitySplits, device.densityDpi)?.let { chosen += it }
        chosen += pickLocales(localeSplits, device.languageTags)
        // Feature / unknown splits: include rather than fail.
        otherSplits.forEach { (name, _) -> chosen += name }

        return Selection(chosen.distinct())
    }

    private fun findBase(apks: List<String>): String? {
        val fileNames = apks.map { it to it.substringAfterLast('/') }
        return fileNames.firstOrNull { (_, name) -> name.equals("base.apk", ignoreCase = true) }?.first
            ?: fileNames.firstOrNull { (_, name) ->
                val lower = name.lowercase(Locale.US)
                !lower.contains("config.") && !lower.startsWith("split_")
            }?.first
            ?: fileNames.minByOrNull { it.second.length }?.first
    }

    /** Extract the config token from `config.xxhdpi.apk` / `split_config.arm64_v8a.apk`. */
    internal fun configToken(fileName: String): String {
        val stem = fileName.removeSuffix(".apk").removeSuffix(".APK")
        return listOf(
            stem.substringAfter("split_config.", missingDelimiterValue = ""),
            stem.substringAfter("___config.", missingDelimiterValue = ""),
            stem.substringAfter("config.", missingDelimiterValue = ""),
        ).firstOrNull { it.isNotEmpty() } ?: stem
    }

    private sealed class Kind {
        data class Abi(val abi: String) : Kind()
        data class Density(val dpi: Int?, val nodpi: Boolean) : Kind()
        data class Locale(val tag: String) : Kind()
        data object Other : Kind()
    }

    private fun classify(rawToken: String): Kind {
        val token = rawToken.lowercase(Locale.US).replace('-', '_')
        val abi = ABI_TOKENS.firstOrNull { abi ->
            abi.lowercase(Locale.US).replace('-', '_') == token
        }
        if (abi != null) return Kind.Abi(abi)
        if (token == "nodpi" || token == "anydpi") return Kind.Density(dpi = null, nodpi = true)
        val density = DENSITY_BY_NAME[token]
        if (density != null) return Kind.Density(dpi = density, nodpi = false)
        // Locales: en, de, en_us, b+en+US
        val localeTag = rawToken.lowercase(Locale.US)
            .removePrefix("b+")
            .replace('+', '-')
            .replace('_', '-')
        val isLocale = localeTag.matches(Regex("^[a-z]{2,3}(-[a-z0-9]+)*$")) &&
            localeTag !in DENSITY_BY_NAME &&
            ABI_TOKENS.none { it.equals(localeTag, ignoreCase = true) }
        return if (isLocale) Kind.Locale(localeTag) else Kind.Other
    }

    private fun pickBestAbi(abiSplits: List<Pair<String, Kind>>, preferredAbis: List<String>): String? {
        val byAbi = abiSplits.mapNotNull { (name, kind) ->
            (kind as? Kind.Abi)?.let { name to it.abi }
        }
        preferredAbis.forEach { want ->
            byAbi.firstOrNull { (_, abi) -> abi.equals(want, ignoreCase = true) }?.let { return it.first }
        }
        return null
    }

    private fun pickBestDensity(densitySplits: List<Pair<String, Kind>>, deviceDpi: Int): String? {
        if (densitySplits.isEmpty()) return null
        val scored = densitySplits.mapNotNull { (name, kind) ->
            val d = kind as? Kind.Density ?: return@mapNotNull null
            if (d.nodpi) {
                name to Int.MAX_VALUE / 2
            } else {
                val dpi = d.dpi ?: return@mapNotNull null
                name to abs(dpi - deviceDpi)
            }
        }
        return scored.minByOrNull { it.second }?.first
    }

    private fun pickLocales(localeSplits: List<Pair<String, Kind>>, languageTags: List<String>): List<String> {
        if (localeSplits.isEmpty() || languageTags.isEmpty()) return emptyList()
        val preferred = languageTags.map { it.lowercase(Locale.US).replace('_', '-') }
        return localeSplits.mapNotNull { (name, kind) ->
            val tag = (kind as? Kind.Locale)?.tag?.lowercase(Locale.US) ?: return@mapNotNull null
            val lang = tag.substringBefore('-')
            val hit = preferred.any { pref ->
                pref == tag || pref.startsWith("$tag-") || pref.substringBefore('-') == lang
            }
            if (hit) name else null
        }
    }
}
