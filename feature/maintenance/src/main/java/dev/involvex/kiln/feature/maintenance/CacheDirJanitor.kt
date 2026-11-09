package dev.involvex.kiln.feature.maintenance

import java.io.File

/** Pure helpers for deleting cache directory trees; unit-tested without Android. */
object CacheDirJanitor {
    /**
     * Deletes files (not directories) under [dir] whose lastModified is older than
     * [nowEpochMs] - [maxAgeMs]. Recurses into subdirectories. Returns bytes freed.
     */
    fun pruneFilesOlderThan(dir: File, maxAgeMs: Long, nowEpochMs: Long): Long {
        if (!dir.isDirectory) return 0L
        val cutoff = nowEpochMs - maxAgeMs
        var freed = 0L
        dir.walkTopDown().forEach { file ->
            if (file.isFile && file.lastModified() < cutoff) {
                val size = file.length()
                if (file.delete()) {
                    freed += size
                }
            }
        }
        return freed
    }

    /** Deletes all files under [dir] recursively; keeps the root directory. Returns bytes freed. */
    fun deleteContents(dir: File): Long {
        if (!dir.isDirectory) return 0L
        var freed = 0L
        dir.listFiles()?.forEach { child ->
            freed += deleteRecursivelyCounting(child)
        }
        return freed
    }

    private fun deleteRecursivelyCounting(file: File): Long {
        var freed = 0L
        if (file.isDirectory) {
            file.listFiles()?.forEach { freed += deleteRecursivelyCounting(it) }
        }
        val size = if (file.isFile) file.length() else 0L
        return if (file.delete()) freed + size else freed
    }
}
