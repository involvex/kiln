package dev.involvex.kiln.core.model

import dev.involvex.kiln.core.common.Result

/** Preview shown before confirming a merge import. */
data class KilnConfigImportPreview(
    val schemaVersion: Int,
    val reposNew: Int,
    val reposUpdated: Int,
    val managedAppsUpserted: Int,
    val prefsSections: List<String>,
    val githubPatWasConfigured: Boolean,
)

/** Result after a successful merge import. */
data class KilnConfigImportResult(val reposUpserted: Int, val managedAppsUpserted: Int, val prefsSectionsApplied: Int)

/**
 * Export / import Kiln-Config JSON (string payload).
 * SAF I/O lives in `:feature:settings`. Bound in `:app`.
 * Must never serialize GitHub PAT values.
 */
interface KilnConfigRepository {
    /** Build pretty-printed JSON for the current device config. */
    suspend fun exportJson(versionName: String, versionCode: Long, exportedAtEpochMs: Long = System.currentTimeMillis()): Result<String>

    /** Parse and compute merge preview without writing. */
    suspend fun previewImport(json: String): Result<KilnConfigImportPreview>

    /** Apply merge upsert for repos/managed apps and overwrite included prefs sections. */
    suspend fun importMerge(json: String): Result<KilnConfigImportResult>
}

/** Suggested SAF document name / MIME for a Kiln-Config export. */
object KilnConfigFilenames {
    fun suggested(exportedAtEpochMs: Long = System.currentTimeMillis()): String {
        val day = java.time.Instant.ofEpochMilli(exportedAtEpochMs)
            .atZone(java.time.ZoneOffset.UTC)
            .toLocalDate()
            .toString()
            .replace("-", "")
        return "kiln-config_$day.json"
    }

    const val MIME_JSON = "application/json"
}
