package dev.involvex.kiln.core.database

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DefaultReposTest {
    @Test
    fun izzy_fingerprint_matches_published() {
        assertEquals(
            "3BF0D6ABFEAE2F401707B6D966BE743BF0EEE49C2561B9BA39073711F628937A",
            DefaultRepos.IZZY_FINGERPRINT,
        )
    }

    @Test
    fun catalog_includes_archive_and_guardian() {
        val ids = DefaultRepos.ALL.map { it.id }.toSet()
        assertTrue(ids.containsAll(setOf("fdroid", "fdroid-archive", "izzy", "guardian")))
    }

    @Test
    fun archive_shares_fdroid_signing_key() {
        val archive = DefaultRepos.ALL.first { it.id == "fdroid-archive" }
        assertEquals(DefaultRepos.FDROID_FINGERPRINT, archive.fingerprint)
    }
}
