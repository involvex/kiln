package dev.involvex.kiln.feature.apps.export

import dev.involvex.kiln.core.model.ApkExportFormat

private const val MAX_LABEL_LENGTH = 48

/**
 * Builds `<sanitizedLabel>_<versionName>_<versionCode>.<ext>` for SAF / share targets.
 */
fun buildExportFilename(label: String, versionName: String?, versionCode: Long, format: ApkExportFormat): String {
    val sanitizedLabel = sanitizeFilenamePart(label).ifBlank { "app" }
        .take(MAX_LABEL_LENGTH)
    val sanitizedVersion = sanitizeFilenamePart(versionName ?: "unknown").ifBlank { "unknown" }
        .take(32)
    val extension = when (format) {
        ApkExportFormat.APK -> "apk"
        ApkExportFormat.APKS -> "apks"
        ApkExportFormat.XAPK -> "xapk"
    }
    return "${sanitizedLabel}_${sanitizedVersion}_$versionCode.$extension"
}

fun sanitizeFilenamePart(raw: String): String {
    val cleaned = buildString(raw.length) {
        for (ch in raw.trim()) {
            when {
                ch.isLetterOrDigit() -> append(ch)
                ch == '.' || ch == '-' || ch == '_' -> append(ch)
                ch.isWhitespace() -> append('_')
                else -> append('_')
            }
        }
    }
    return cleaned.trim('_').replace(Regex("_+"), "_")
}
