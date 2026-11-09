package dev.involvex.kiln.feature.sources

/** Builds an HTTPS media URL from an F-Droid-style repo address and index-relative path. */
object FdroidMediaUrl {
    fun join(repoAddress: String, relativePath: String?): String? {
        val trimmed = relativePath?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val base = repoAddress.trimEnd('/')
        val path = trimmed.trimStart('/')
        return "$base/$path"
    }
}
