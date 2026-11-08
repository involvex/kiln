package dev.involvex.kiln.feature.apps.util

import java.util.Locale

private const val BYTES_PER_KIB = 1024.0

fun formatApkSize(bytes: Long?): String? {
    if (bytes == null) return null
    val kib = bytes / BYTES_PER_KIB
    val mib = kib / BYTES_PER_KIB
    val gib = mib / BYTES_PER_KIB
    return when {
        bytes < BYTES_PER_KIB -> "$bytes B"
        kib < BYTES_PER_KIB -> String.format(Locale.US, "%.1f KB", kib)
        mib < BYTES_PER_KIB -> String.format(Locale.US, "%.1f MB", mib)
        else -> String.format(Locale.US, "%.2f GB", gib)
    }
}
