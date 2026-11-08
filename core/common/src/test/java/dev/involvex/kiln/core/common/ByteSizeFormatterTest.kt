package dev.involvex.kiln.core.common

import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ByteSizeFormatterTest {
    @Test
    fun formats_bytes_and_binary_units() {
        assertEquals("0 B", ByteSizeFormatter.format(0))
        assertEquals("512 B", ByteSizeFormatter.format(512))
        assertEquals("1.0 KiB", ByteSizeFormatter.format(1024, Locale.US))
        assertEquals("1.5 MiB", ByteSizeFormatter.format((1024L * 1024 * 3) / 2, Locale.US))
        assertEquals("-2.0 KiB", ByteSizeFormatter.format(-2048, Locale.US))
    }
}
