package dev.involvex.kiln.feature.sources

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FdroidApkUrlTest {
    @Test
    fun `joins repo and relative apk name`() {
        assertEquals(
            "https://f-droid.org/repo/com.example_1.apk",
            FdroidApkUrl.build("https://f-droid.org/repo/", "/com.example_1.apk"),
        )
    }
}
