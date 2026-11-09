package dev.involvex.kiln.feature.sources.discovery

/**
 * Builds share text / store URLs for a catalog app. Pure logic for unit tests.
 */
object CatalogShareLinks {
    fun storePageUrl(packageName: String, repoId: String, repoAddress: String): String? {
        val address = repoAddress.lowercase()
        return when {
            repoId.equals("fdroid", ignoreCase = true) ||
                address.contains("f-droid.org") ->
                "https://f-droid.org/packages/$packageName/"

            repoId.equals("izzy", ignoreCase = true) ||
                address.contains("izzysoft.de") ->
                "https://apt.izzysoft.de/fdroid/index/apk/$packageName"

            else -> null
        }
    }

    fun shareBody(
        name: String,
        packageName: String,
        repoId: String,
        repoAddress: String,
        sourceCodeUrl: String?,
        repoName: String,
    ): String {
        val lines = buildList {
            add(name)
            add(packageName)
            storePageUrl(packageName, repoId, repoAddress)?.let { add(it) }
            sourceCodeUrl?.takeIf { it.isNotBlank() }?.let { add(it) }
            if (none { it.startsWith("http") }) {
                add(repoName)
                repoAddress.takeIf { it.isNotBlank() }?.let { add(it) }
            }
        }
        return lines.joinToString("\n")
    }
}
