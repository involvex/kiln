package dev.involvex.kiln.feature.sources

/** Plain-text normalization for GitHub release bodies / F-Droid whatsNew. */
object ReleaseNotesText {
    const val MAX_STORE_CHARS = 4_000
    const val PREVIEW_CHARS = 600

    fun normalize(raw: String?): String? {
        val trimmed = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return if (trimmed.length <= MAX_STORE_CHARS) trimmed else trimmed.take(MAX_STORE_CHARS)
    }

    fun preview(notes: String, limit: Int = PREVIEW_CHARS): String {
        val trimmed = notes.trim()
        if (trimmed.length <= limit) return trimmed
        return trimmed.take(limit).trimEnd() + "…"
    }
}
