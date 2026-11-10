package dev.involvex.kiln.core.database

import dev.involvex.kiln.core.common.normalizeFingerprint

/**
 * Built-in F-Droid-compatible repos. [ensureKnownRepos] upserts address/fingerprint/name
 * for these ids without changing [RepoEntity.enabled] on existing rows.
 */
object DefaultRepos {
    val FDROID_FINGERPRINT = normalizeFingerprint(
        "43238D512C1E5EB2D6569F4A3AFBF5523418B82E0A3ED1552770ABB9A9C9CCAB",
    )
    val IZZY_FINGERPRINT = normalizeFingerprint(
        "3BF0D6ABFEAE2F401707B6D966BE743BF0EEE49C2561B9BA39073711F628937A",
    )
    val GUARDIAN_FINGERPRINT = normalizeFingerprint(
        "B7C2EEFD8DAC7806AF67DFCD92EB18126BC08312A7F2D6F3862E46013C7A6135",
    )

    data class Spec(
        val id: String,
        val name: String,
        val address: String,
        val fingerprint: String,
        val priority: Int,
        val enabledByDefault: Boolean = true,
    )

    val ALL: List<Spec> = listOf(
        Spec(
            id = "fdroid",
            name = "F-Droid",
            address = "https://f-droid.org/repo",
            fingerprint = FDROID_FINGERPRINT,
            priority = 1,
        ),
        Spec(
            id = "fdroid-archive",
            name = "F-Droid Archive",
            address = "https://f-droid.org/archive",
            fingerprint = FDROID_FINGERPRINT,
            priority = 2,
        ),
        Spec(
            id = "izzy",
            name = "IzzyOnDroid",
            address = "https://apt.izzysoft.de/fdroid/repo",
            fingerprint = IZZY_FINGERPRINT,
            priority = 0,
        ),
        Spec(
            id = "guardian",
            name = "Guardian Project",
            address = "https://guardianproject.info/fdroid/repo",
            fingerprint = GUARDIAN_FINGERPRINT,
            priority = 3,
        ),
    )
}

/**
 * Insert missing built-in repos and repair known address/fingerprint/name.
 * Preserves [RepoEntity.enabled] and [RepoEntity.lastSyncedAtEpochMs] for existing rows.
 */
suspend fun ensureKnownRepos(repoDao: RepoDao) {
    val existing = repoDao.getAll().associateBy { it.id }
    val toUpsert = DefaultRepos.ALL.map { spec ->
        val prior = existing[spec.id]
        if (prior == null) {
            RepoEntity(
                id = spec.id,
                name = spec.name,
                address = spec.address,
                fingerprint = spec.fingerprint,
                kind = "FDROID",
                enabled = spec.enabledByDefault,
                lastSyncedAtEpochMs = null,
                priority = spec.priority,
                mirrorsCsv = "",
            )
        } else {
            prior.copy(
                name = spec.name,
                address = spec.address,
                fingerprint = spec.fingerprint,
                kind = "FDROID",
                priority = spec.priority,
                // Preserve user mirrors when repairing known address/fingerprint.
            )
        }
    }
    repoDao.upsertAll(toUpsert)
}
