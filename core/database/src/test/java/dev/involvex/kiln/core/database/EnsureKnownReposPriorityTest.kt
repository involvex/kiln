package dev.involvex.kiln.core.database

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Documents repair behavior: existing rows keep user [RepoEntity.priority]
 * (mirrors [ensureKnownRepos] copy path).
 */
class EnsureKnownReposPriorityTest {
    @Test
    fun repair_copy_keeps_user_priority() {
        val prior = RepoEntity(
            id = "izzy",
            name = "old",
            address = "https://example.com",
            fingerprint = "AA",
            kind = "FDROID",
            enabled = true,
            lastSyncedAtEpochMs = 1L,
            priority = 42,
            mirrorsCsv = "https://mirror.example/",
        )
        val spec = DefaultRepos.ALL.first { it.id == "izzy" }
        val repaired = prior.copy(
            name = spec.name,
            address = spec.address,
            fingerprint = spec.fingerprint,
            kind = "FDROID",
        )
        assertEquals(42, repaired.priority)
        assertEquals("https://mirror.example/", repaired.mirrorsCsv)
        assertEquals(spec.name, repaired.name)
    }
}
