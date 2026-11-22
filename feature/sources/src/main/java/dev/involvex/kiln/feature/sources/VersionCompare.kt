package dev.involvex.kiln.feature.sources

/**
 * Primary compare by versionCode; fallback to loose versionName segments.
 * @return true if [remoteCode]/[remoteName] is newer than installed.
 */
fun isRemoteNewer(installedCode: Long, installedName: String?, remoteCode: Long, remoteName: String?): Boolean {
    if (remoteCode > 0 && installedCode > 0) {
        return remoteCode > installedCode
    }
    return compareVersionNames(remoteName, installedName) > 0
}

/**
 * GitHub tags are not Android versionCodes. Prefer dotted version-name compare when both
 * sides look like `1.2.3` / `v158.0.8127.42`; otherwise fall back to [isRemoteNewer].
 */
fun isGitHubRemoteNewer(installedCode: Long, installedName: String?, remoteCode: Long, remoteName: String?): Boolean {
    if (hasDottedVersion(installedName) && hasDottedVersion(remoteName)) {
        return compareVersionNames(remoteName, installedName) > 0
    }
    return isRemoteNewer(installedCode, installedName, remoteCode, remoteName)
}

fun hasDottedVersion(raw: String?): Boolean {
    if (raw.isNullOrBlank()) return false
    val trimmed = raw.trim().trimStart('v', 'V')
    return trimmed.contains('.') && trimmed.any { it.isDigit() }
}

fun compareVersionNames(a: String?, b: String?): Int {
    val left = a ?: return if (b == null) 0 else -1
    val right = b ?: return 1
    val aParts = left.trimStart('v', 'V').split(Regex("[^0-9A-Za-z]+"))
    val bParts = right.trimStart('v', 'V').split(Regex("[^0-9A-Za-z]+"))
    val len = maxOf(aParts.size, bParts.size)
    for (i in 0 until len) {
        val ap = aParts.getOrNull(i).orEmpty()
        val bp = bParts.getOrNull(i).orEmpty()
        val an = ap.toLongOrNull()
        val bn = bp.toLongOrNull()
        val cmp = when {
            an != null && bn != null -> an.compareTo(bn)
            else -> ap.compareTo(bp, ignoreCase = true)
        }
        if (cmp != 0) return cmp
    }
    return 0
}
