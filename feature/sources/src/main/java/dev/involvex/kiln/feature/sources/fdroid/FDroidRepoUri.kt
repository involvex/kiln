package dev.involvex.kiln.feature.sources.fdroid

import dev.involvex.kiln.core.common.normalizeFingerprint
import java.net.URI
import java.util.Locale

/**
 * Parsed F-Droid repository add URI.
 * Supports `fdroidrepos://`, `fdroidrepo://`, and `https://…?fingerprint=`.
 */
data class ParsedFDroidRepoUri(val address: String, val fingerprint: String?, val suggestedName: String, val usesHttp: Boolean)

data class ParsedFDroidPackagePage(val packageName: String)

object FDroidRepoUri {
    private val PACKAGE_PATH = Regex("""^/packages/([A-Za-z0-9._]+)/?$""")

    fun parseRepo(raw: String): ParsedFDroidRepoUri? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val uri = runCatching { URI(trimmed) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase(Locale.US) ?: return null
        return when (scheme) {
            "fdroidrepos", "fdroidrepo" -> parseSpecialScheme(uri, scheme)
            "https", "http" -> parseHttpRepo(uri, scheme)
            else -> null
        }
    }

    /** `https://f-droid.org/packages/<pkg>/` style package page (not a repo add). */
    fun parsePackagePage(raw: String): ParsedFDroidPackagePage? {
        val uri = runCatching { URI(raw.trim()) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase(Locale.US)
        val host = uri.host?.lowercase(Locale.US)
        val match = PACKAGE_PATH.matchEntire(uri.path.orEmpty())
        val isHttp = scheme == "https" || scheme == "http"
        val isFdroidHost = host == "f-droid.org" || host == "www.f-droid.org"
        if (!isHttp || !isFdroidHost || match == null) return null
        return ParsedFDroidPackagePage(packageName = match.groupValues[1])
    }

    private fun parseSpecialScheme(uri: URI, scheme: String): ParsedFDroidRepoUri? {
        // fdroidrepos://host/path?fingerprint=…  → https://host/path
        val host = uri.host?.lowercase(Locale.US) ?: return null
        val path = uri.path?.trimEnd('/')?.ifEmpty { null } ?: ""
        val https = scheme == "fdroidrepos"
        val address = buildString {
            append(if (https) "https://" else "http://")
            append(host)
            if (uri.port != -1) append(':').append(uri.port)
            append(path)
        }.trimEnd('/')
        if (address.removePrefix("https://").removePrefix("http://").isBlank()) return null
        val fingerprint = queryParam(uri, "fingerprint")?.let { normalizeFingerprint(it) }
            ?.takeIf { it.isNotEmpty() }
        return ParsedFDroidRepoUri(
            address = address,
            fingerprint = fingerprint,
            suggestedName = suggestedName(host),
            usesHttp = !https,
        )
    }

    private fun parseHttpRepo(uri: URI, scheme: String): ParsedFDroidRepoUri? {
        val host = uri.host?.lowercase(Locale.US) ?: return null
        // Package pages are not repo URIs.
        if (parsePackagePage(uri.toString()) != null) return null
        val fingerprint = queryParam(uri, "fingerprint")?.let { normalizeFingerprint(it) }
            ?.takeIf { it.isNotEmpty() }
        val path = (uri.path ?: "").lowercase(Locale.US).trimEnd('/')
        val looksLikeRepo = path.endsWith("/fdroid/repo") ||
            path.endsWith("/repo") ||
            path.endsWith("/archive") ||
            path.endsWith("/fdroid/archive")
        if (fingerprint == null && !looksLikeRepo) return null
        val address = buildString {
            append(scheme)
            append("://")
            append(host)
            if (uri.port != -1) append(':').append(uri.port)
            append(uri.path?.trimEnd('/') ?: "")
        }.trimEnd('/')
        return ParsedFDroidRepoUri(
            address = address,
            fingerprint = fingerprint,
            suggestedName = suggestedName(host),
            usesHttp = scheme == "http",
        )
    }

    private fun queryParam(uri: URI, name: String): String? {
        val raw = uri.rawQuery ?: return null
        return raw.split('&')
            .asSequence()
            .map { it.split('=', limit = 2) }
            .firstOrNull { it.firstOrNull() == name }
            ?.getOrNull(1)
            // String charset name: Charset overload needs API 33; minSdk is 26.
            ?.let { java.net.URLDecoder.decode(it, "UTF-8") }
    }

    private fun suggestedName(host: String): String = when {
        host.contains("izzysoft") -> "IzzyOnDroid"

        host.contains("guardianproject") -> "Guardian Project"

        host == "f-droid.org" || host == "www.f-droid.org" -> "F-Droid"

        else -> host.substringBefore('.')
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
    }
}
