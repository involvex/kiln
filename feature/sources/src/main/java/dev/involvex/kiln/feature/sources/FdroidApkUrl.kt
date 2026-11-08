package dev.involvex.kiln.feature.sources

/** Builds an HTTPS APK URL from an F-Droid-style repo address and index `file.name`. */
object FdroidApkUrl {
    fun build(repoAddress: String, apkName: String): String {
        val base = repoAddress.trimEnd('/')
        val path = apkName.trimStart('/')
        return "$base/$path"
    }
}
