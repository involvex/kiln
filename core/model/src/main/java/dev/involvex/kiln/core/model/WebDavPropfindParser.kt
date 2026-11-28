package dev.involvex.kiln.core.model

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Minimal PROPFIND multistatus parser (Depth 0/1). Ignores namespaces beyond local names.
 */
object WebDavPropfindParser {
    data class Entry(val href: String, val displayName: String, val lastModifiedEpochMs: Long, val isCollection: Boolean)

    fun parse(xml: String): List<Entry> {
        val responses = RESPONSE_REGEX.findAll(xml).map { it.groupValues[1] }.toList()
        return responses.mapNotNull { block ->
            val href = HREF_REGEX.find(block)?.groupValues?.get(1)?.trim()?.let { decodeXml(it) }
                ?: return@mapNotNull null
            val isCollection = COLLECTION_REGEX.containsMatchIn(block)
            val display = DISPLAYNAME_REGEX.find(block)?.groupValues?.get(1)?.trim()?.let { decodeXml(it) }
                ?: WebDavUrl.hrefToDisplayName(href)
            val modifiedRaw = GETLASTMODIFIED_REGEX.find(block)?.groupValues?.get(1)?.trim()
            val modified = parseHttpDate(modifiedRaw) ?: 0L
            Entry(
                href = href,
                displayName = display,
                lastModifiedEpochMs = modified,
                isCollection = isCollection,
            )
        }
    }

    /** File archives only (non-collections whose name ends with .kilnbackup.zip). */
    fun archiveEntries(xml: String): List<BackupTreeEntry> = parse(xml)
        .filter { !it.isCollection && it.displayName.endsWith(ARCHIVE_SUFFIX, ignoreCase = true) }
        .map {
            BackupTreeEntry(
                displayName = it.displayName,
                uriString = it.href,
                lastModifiedEpochMs = it.lastModifiedEpochMs,
            )
        }

    private fun parseHttpDate(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            Instant.from(HTTP_DATE.parse(raw)).toEpochMilli()
        }.getOrNull()
    }

    private fun decodeXml(value: String): String = value
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")

    private val RESPONSE_REGEX = Regex(
        """<(?:[\w.-]+:)?response\b[^>]*>(.*?)</(?:[\w.-]+:)?response>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val HREF_REGEX = Regex(
        """<(?:[\w.-]+:)?href\b[^>]*>(.*?)</(?:[\w.-]+:)?href>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val DISPLAYNAME_REGEX = Regex(
        """<(?:[\w.-]+:)?displayname\b[^>]*>(.*?)</(?:[\w.-]+:)?displayname>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val GETLASTMODIFIED_REGEX = Regex(
        """<(?:[\w.-]+:)?getlastmodified\b[^>]*>(.*?)</(?:[\w.-]+:)?getlastmodified>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val COLLECTION_REGEX = Regex(
        """<(?:[\w.-]+:)?collection\b""",
        RegexOption.IGNORE_CASE,
    )
    private val HTTP_DATE = DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.US)
    private const val ARCHIVE_SUFFIX = ".kilnbackup.zip"
}
