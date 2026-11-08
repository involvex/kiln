package dev.involvex.kiln.feature.onboarding.github

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GitHubMapEntry(val packageName: String, val ownerRepo: String, val assetRegex: String = ".*\\.apk$")

@Singleton
class GitHubMap @Inject constructor(@ApplicationContext private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val entries: List<GitHubMapEntry> by lazy { load() }

    fun lookup(packageName: String): GitHubMapEntry? = entries.firstOrNull { it.packageName == packageName }

    fun all(): List<GitHubMapEntry> = entries

    private fun load(): List<GitHubMapEntry> = try {
        context.assets.open("github-map.json").bufferedReader().use { reader ->
            json.decodeFromString<List<GitHubMapEntry>>(reader.readText())
        }
    } catch (_: Exception) {
        emptyList()
    }
}
