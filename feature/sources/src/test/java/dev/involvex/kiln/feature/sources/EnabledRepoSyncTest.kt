package dev.involvex.kiln.feature.sources

import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.database.RepoEntity
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EnabledRepoSyncTest {
    @Test
    fun syncAll_countsSuccessAndFailure() = runBlocking {
        val repos = listOf(repo("a"), repo("b"), repo("c"))
        val (ok, fail) = EnabledRepoSync.syncAll(repos, concurrency = 2) { entity ->
            if (entity.id == "b") Result.failure("boom") else Result.success(Unit)
        }
        assertEquals(2, ok)
        assertEquals(1, fail)
    }

    @Test
    fun syncAll_respectsConcurrencyBound() = runBlocking {
        val repos = (1..6).map { repo("r$it") }
        val inFlight = AtomicInteger(0)
        var maxInFlight = 0
        EnabledRepoSync.syncAll(repos, concurrency = 2) {
            val now = inFlight.incrementAndGet()
            maxInFlight = maxOf(maxInFlight, now)
            delay(20)
            inFlight.decrementAndGet()
            Result.success(Unit)
        }
        assertTrue(maxInFlight <= 2, "maxInFlight=$maxInFlight")
    }

    private fun repo(id: String) = RepoEntity(
        id = id,
        name = id,
        address = "https://example.com/$id",
        fingerprint = "A".repeat(64),
        kind = "FDROID",
        enabled = true,
        lastSyncedAtEpochMs = null,
        priority = 1,
    )
}
