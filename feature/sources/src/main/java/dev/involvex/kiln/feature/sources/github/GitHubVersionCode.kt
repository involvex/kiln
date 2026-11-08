package dev.involvex.kiln.feature.sources.github

/**
 * Best-effort versionCode from a GitHub release tag (e.g. `v1.2.3` → segments).
 * Not a substitute for Android versionCode; used for ordering and InstallQueue metadata.
 */
object GitHubVersionCode {
    fun parse(tag: String): Long {
        val digits = Regex("""\d+""")
            .findAll(tag.trimStart('v', 'V'))
            .mapNotNull { it.value.toLongOrNull() }
            .toList()
        if (digits.isEmpty()) return 0L
        return digits.fold(0L) { acc, n ->
            acc * VERSION_SEGMENT_BASE + n.coerceAtMost(VERSION_SEGMENT_MAX)
        }
    }

    private const val VERSION_SEGMENT_BASE = 1000L
    private const val VERSION_SEGMENT_MAX = 999L
}
