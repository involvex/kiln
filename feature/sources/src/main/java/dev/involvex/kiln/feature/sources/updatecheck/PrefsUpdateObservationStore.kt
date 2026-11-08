package dev.involvex.kiln.feature.sources.updatecheck

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.model.UpdateObservationKey
import dev.involvex.kiln.core.model.UpdateObservationStore
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class PrefsUpdateObservationStore @Inject constructor(@ApplicationContext context: Context) : UpdateObservationStore {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val mutex = Mutex()

    override suspend fun ensureFirstSeen(keys: Collection<UpdateObservationKey>, nowEpochMs: Long): Map<String, Long> =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val editor = prefs.edit()
                val result = LinkedHashMap<String, Long>(keys.size)
                for (key in keys) {
                    val storage = key.storageKey()
                    val existing = if (prefs.contains(storage)) prefs.getLong(storage, nowEpochMs) else null
                    if (existing == null) {
                        editor.putLong(storage, nowEpochMs)
                        result[storage] = nowEpochMs
                    } else {
                        result[storage] = existing
                    }
                }
                editor.apply()
                result
            }
        }

    private companion object {
        const val PREFS_NAME = "kiln_update_first_seen"
    }
}
