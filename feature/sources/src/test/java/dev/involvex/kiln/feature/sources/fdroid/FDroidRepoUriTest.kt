package dev.involvex.kiln.feature.sources.fdroid

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FDroidRepoUriTest {
    @Test
    fun parse_fdroidrepos_with_fingerprint() {
        val parsed = FDroidRepoUri.parseRepo(
            "fdroidrepos://apt.izzysoft.de/fdroid/repo?fingerprint=3BF0D6ABFEAE2F401707B6D966BE743BF0EEE49C2561B9BA39073711F628937A",
        )
        assertNotNull(parsed)
        assertEquals("https://apt.izzysoft.de/fdroid/repo", parsed!!.address)
        assertEquals("3BF0D6ABFEAE2F401707B6D966BE743BF0EEE49C2561B9BA39073711F628937A", parsed.fingerprint)
        assertFalse(parsed.usesHttp)
        assertEquals("IzzyOnDroid", parsed.suggestedName)
    }

    @Test
    fun parse_https_with_fingerprint_strips_query() {
        val parsed = FDroidRepoUri.parseRepo(
            "https://apt.izzysoft.de/fdroid/repo/?fingerprint=3BF0D6ABFEAE2F401707B6D966BE743BF0EEE49C2561B9BA39073711F628937A",
        )
        assertNotNull(parsed)
        assertEquals("https://apt.izzysoft.de/fdroid/repo", parsed!!.address)
        assertEquals("3BF0D6ABFEAE2F401707B6D966BE743BF0EEE49C2561B9BA39073711F628937A", parsed.fingerprint)
    }

    @Test
    fun parse_fdroidrepo_http_scheme() {
        val parsed = FDroidRepoUri.parseRepo("fdroidrepo://example.com/fdroid/repo")
        assertNotNull(parsed)
        assertTrue(parsed!!.usesHttp)
        assertEquals("http://example.com/fdroid/repo", parsed.address)
    }

    @Test
    fun parse_package_page() {
        val page = FDroidRepoUri.parsePackagePage("https://f-droid.org/packages/org.fdroid.fdroid/")
        assertEquals("org.fdroid.fdroid", page!!.packageName)
        assertNull(FDroidRepoUri.parseRepo("https://f-droid.org/packages/org.fdroid.fdroid/"))
    }
}
