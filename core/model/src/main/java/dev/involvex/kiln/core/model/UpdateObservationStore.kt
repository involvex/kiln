package dev.involvex.kiln.core.model

/**
 * Tracks when Kiln first observed a candidate update (package + versionCode).
 * Used for auto-install cooldown — not publisher release dates.
 */
interface UpdateObservationStore {
    /**
     * Ensures each [keys] entry has a first-seen timestamp (inserts [nowEpochMs] if missing).
     * Returns packageName:versionCode → epoch ms.
     */
    suspend fun ensureFirstSeen(keys: Collection<UpdateObservationKey>, nowEpochMs: Long): Map<String, Long>
}

data class UpdateObservationKey(val packageName: String, val versionCode: Long) {
    fun storageKey(): String = "$packageName:$versionCode"
}
