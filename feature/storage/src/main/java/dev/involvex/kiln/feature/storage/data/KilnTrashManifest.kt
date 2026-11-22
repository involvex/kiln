package dev.involvex.kiln.feature.storage.data

import dev.involvex.kiln.core.model.KilnTrashEntry

/**
 * Line-oriented trash manifest (no kotlinx.serialization dependency in `:feature:storage`).
 * Header `v1`, then one TSV row per entry:
 * `id\toriginalRelativePath\ttrashFileName\tdisplayName\tsizeBytes\tmovedAtEpochMs`
 */
object KilnTrashManifest {
    const val HEADER = "v1"

    fun encode(entries: List<KilnTrashEntry>): String = buildString {
        appendLine(HEADER)
        entries.forEach { entry ->
            append(escape(entry.id))
            append('\t')
            append(escape(entry.originalRelativePath))
            append('\t')
            append(escape(entry.trashFileName))
            append('\t')
            append(escape(entry.displayName))
            append('\t')
            append(entry.sizeBytes)
            append('\t')
            append(entry.movedAtEpochMs)
            append('\n')
        }
    }

    fun decode(text: String, trashUriFor: (String) -> String): List<KilnTrashEntry> {
        val lines = text.lineSequence().map { it.trimEnd('\r') }.filter { it.isNotBlank() }.toList()
        if (lines.isEmpty() || lines.first() != HEADER) return emptyList()
        return lines.drop(1).mapNotNull { line ->
            val parts = line.split('\t')
            if (parts.size < COLUMN_COUNT) return@mapNotNull null
            val trashFileName = unescape(parts[COL_TRASH_NAME])
            KilnTrashEntry(
                id = unescape(parts[COL_ID]),
                originalRelativePath = unescape(parts[COL_ORIGINAL_PATH]),
                trashFileName = trashFileName,
                displayName = unescape(parts[COL_DISPLAY_NAME]),
                sizeBytes = parts[COL_SIZE].toLongOrNull() ?: return@mapNotNull null,
                movedAtEpochMs = parts[COL_MOVED_AT].toLongOrNull() ?: return@mapNotNull null,
                uriString = trashUriFor(trashFileName),
            )
        }
    }

    private const val COLUMN_COUNT = 6
    private const val COL_ID = 0
    private const val COL_ORIGINAL_PATH = 1
    private const val COL_TRASH_NAME = 2
    private const val COL_DISPLAY_NAME = 3
    private const val COL_SIZE = 4
    private const val COL_MOVED_AT = 5

    private fun escape(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\t", "\\t")
        .replace("\n", "\\n")
        .replace("\r", "\\r")

    private fun unescape(value: String): String {
        val out = StringBuilder(value.length)
        var i = 0
        while (i < value.length) {
            val c = value[i]
            if (c == '\\' && i + 1 < value.length) {
                when (value[i + 1]) {
                    't' -> {
                        out.append('\t')
                        i += 2
                    }

                    'n' -> {
                        out.append('\n')
                        i += 2
                    }

                    'r' -> {
                        out.append('\r')
                        i += 2
                    }

                    '\\' -> {
                        out.append('\\')
                        i += 2
                    }

                    else -> {
                        out.append(c)
                        i++
                    }
                }
            } else {
                out.append(c)
                i++
            }
        }
        return out.toString()
    }
}
