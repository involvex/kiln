package dev.involvex.kiln.core.model

import java.net.URI
import java.net.URLEncoder

/**
 * HTTPS-only WebDAV URL helpers (pure JVM). Rejects cleartext and normalizes paths.
 */
object WebDavUrl {
    fun normalizeBaseUrl(raw: String): String? {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("https://", ignoreCase = true)) return null
        return trimmed.trimEnd('/')
    }

    fun normalizeRemotePath(raw: String): String {
        val trimmed = raw.trim().ifEmpty { BackupPrefs.DEFAULT_WEBDAV_PATH }
        val withSlash = if (trimmed.startsWith("/")) trimmed else "/$trimmed"
        return withSlash.trimEnd('/').ifEmpty { BackupPrefs.DEFAULT_WEBDAV_PATH }
    }

    /**
     * Joins [baseUrl] (https, no trailing slash) with absolute [remotePath] and extra [segments].
     */
    fun join(baseUrl: String, remotePath: String, vararg segments: String): String? {
        val base = normalizeBaseUrl(baseUrl) ?: return null
        val path = buildString {
            append(normalizeRemotePath(remotePath))
            segments.forEach { segment ->
                val encoded = encodeSegment(segment)
                if (encoded.isNotEmpty()) {
                    append('/')
                    append(encoded)
                }
            }
        }
        return base + path
    }

    fun encodeSegment(segment: String): String = URLEncoder.encode(segment, Charsets.UTF_8.name()).replace("+", "%20")

    fun hrefToDisplayName(href: String): String {
        val path = runCatching { URI(href).path }.getOrNull() ?: href
        return path.trimEnd('/').substringAfterLast('/').ifEmpty { path }
    }

    fun isCollectionHref(href: String, hasCollectionType: Boolean): Boolean =
        hasCollectionType || href.trimEnd('/').let { it.endsWith("/") || !it.substringAfterLast('/').contains('.') }
}
