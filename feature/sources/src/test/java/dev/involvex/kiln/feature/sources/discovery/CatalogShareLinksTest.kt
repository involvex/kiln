package dev.involvex.kiln.feature.sources.discovery

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CatalogShareLinksTest {
    @Test
    fun `fdroid store page from repo id`() {
        assertEquals(
            "https://f-droid.org/packages/com.example/",
            CatalogShareLinks.storePageUrl("com.example", "fdroid", ""),
        )
    }

    @Test
    fun `izzy store page from address`() {
        assertEquals(
            "https://apt.izzysoft.de/fdroid/index/apk/com.example",
            CatalogShareLinks.storePageUrl(
                "com.example",
                "custom",
                "https://apt.izzysoft.de/fdroid/repo",
            ),
        )
    }

    @Test
    fun `share body includes store and source urls`() {
        val body = CatalogShareLinks.shareBody(
            name = "Example",
            packageName = "com.example",
            repoId = "fdroid",
            repoAddress = "https://f-droid.org/repo",
            sourceCodeUrl = "https://github.com/example/app",
            repoName = "F-Droid",
        )
        assertTrue(body.contains("Example"))
        assertTrue(body.contains("com.example"))
        assertTrue(body.contains("https://f-droid.org/packages/com.example/"))
        assertTrue(body.contains("https://github.com/example/app"))
    }
}
