package dev.involvex.kiln.core.common

/**
 * Ordered HTTPS base URLs for an F-Droid-style repo: primary [address] then [mirrorsCsv]
 * (comma-separated). Used for index sync and APK download failover.
 */
object RepoMirrorUrls {
    fun candidates(address: String, mirrorsCsv: String): List<String> {
        val primary = normalizeBase(address) ?: return emptyList()
        val mirrors = mirrorsCsv.split(',')
            .mapNotNull { normalizeBase(it) }
            .filter { it != primary }
        return (listOf(primary) + mirrors).distinct()
    }

    fun parseMirrorsInput(raw: String): String = raw.lineSequence()
        .flatMap { line -> line.split(',').asSequence() }
        .mapNotNull { normalizeBase(it) }
        .distinct()
        .joinToString(",")

    fun mirrorsList(mirrorsCsv: String): List<String> = mirrorsCsv.split(',').mapNotNull { normalizeBase(it) }

    /**
     * Rewrites [primaryUrl] onto each candidate base when [primaryUrl] starts with the
     * primary repo [address]. Returns [primaryUrl] alone when it does not match.
     */
    fun alternateDownloadUrls(primaryUrl: String, address: String, mirrorsCsv: String): List<String> {
        val primaryBase = normalizeBase(address) ?: return listOf(primaryUrl)
        if (!primaryUrl.startsWith(primaryBase)) {
            return listOf(primaryUrl)
        }
        val suffix = primaryUrl.removePrefix(primaryBase)
        return candidates(address, mirrorsCsv).map { base -> base + suffix }.distinct()
    }

    private fun normalizeBase(raw: String): String? {
        val trimmed = raw.trim().trimEnd('/')
        if (!trimmed.startsWith("https://")) return null
        if (trimmed.length <= "https://".length) return null
        return trimmed
    }
}
