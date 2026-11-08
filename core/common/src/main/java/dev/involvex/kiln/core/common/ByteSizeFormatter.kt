package dev.involvex.kiln.core.common

import java.util.Locale
import kotlin.math.abs

/** Human-readable byte sizes (binary units). Pure logic for unit tests. */
object ByteSizeFormatter {
    private const val UNIT_STEP = 1024.0
    private val UNITS = arrayOf("B", "KiB", "MiB", "GiB", "TiB")

    fun format(bytes: Long, locale: Locale = Locale.US): String {
        if (bytes == 0L) return "0 B"
        val sign = if (bytes < 0) "-" else ""
        var value = abs(bytes).toDouble()
        var unit = 0
        while (value >= UNIT_STEP && unit < UNITS.lastIndex) {
            value /= UNIT_STEP
            unit++
        }
        val pattern = if (unit == 0) "%.0f %s" else "%.1f %s"
        return sign + String.format(locale, pattern, value, UNITS[unit])
    }
}
