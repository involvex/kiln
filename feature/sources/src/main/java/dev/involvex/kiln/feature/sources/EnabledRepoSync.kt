package dev.involvex.kiln.feature.sources

import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.database.RepoEntity
import dev.involvex.kiln.core.model.UpdateCheckProgress
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Syncs enabled F-Droid repos with bounded concurrency and progress callbacks.
 */
object EnabledRepoSync {
    const val DEFAULT_CONCURRENCY = 2

    suspend fun syncAll(
        repos: List<RepoEntity>,
        concurrency: Int = DEFAULT_CONCURRENCY,
        onProgress: suspend (UpdateCheckProgress) -> Unit = {},
        syncOne: suspend (RepoEntity) -> Result<Unit>,
    ): Pair<Int, Int> {
        if (repos.isEmpty()) return 0 to 0
        val ok = AtomicInteger(0)
        val fail = AtomicInteger(0)
        val completed = AtomicInteger(0)
        val total = repos.size
        val permits = Semaphore(concurrency.coerceAtLeast(1))
        coroutineScope {
            repos.map { repo ->
                async {
                    permits.withPermit {
                        ensureActive()
                        onProgress(
                            UpdateCheckProgress(
                                current = completed.get() + 1,
                                total = total,
                                label = repo.name,
                            ),
                        )
                        when (syncOne(repo)) {
                            is Result.Success -> ok.incrementAndGet()
                            is Result.Failure -> fail.incrementAndGet()
                        }
                        completed.incrementAndGet()
                    }
                }
            }.awaitAll()
        }
        return ok.get() to fail.get()
    }
}
