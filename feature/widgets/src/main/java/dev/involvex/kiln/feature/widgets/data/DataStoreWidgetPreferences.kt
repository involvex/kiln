package dev.involvex.kiln.feature.widgets.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.model.WidgetPreferences
import dev.involvex.kiln.core.model.WidgetSnapshot
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.widgetPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "kiln_widget_prefs",
)

@Singleton
class DataStoreWidgetPreferences @Inject constructor(@ApplicationContext context: Context) : WidgetPreferences {
    private val store = context.widgetPrefsDataStore

    override fun observe(): Flow<WidgetSnapshot> = store.data.map { it.toSnapshot() }

    override suspend fun current(): WidgetSnapshot = store.data.first().toSnapshot()

    override suspend fun setUpdateCount(count: Int, epochMs: Long) {
        store.edit {
            it[KEY_COUNT] = count.coerceAtLeast(0)
            it[KEY_EPOCH] = epochMs
            it[KEY_CHECK_QUEUED] = false
        }
    }

    override suspend fun setCheckQueued(queued: Boolean) {
        store.edit { it[KEY_CHECK_QUEUED] = queued }
    }

    override suspend fun setTrimConfirmPending(pending: Boolean) {
        store.edit { it[KEY_TRIM_CONFIRM] = pending }
    }

    private fun Preferences.toSnapshot() = WidgetSnapshot(
        lastUpdateCount = this[KEY_COUNT] ?: 0,
        lastCheckedEpochMs = this[KEY_EPOCH] ?: 0L,
        checkQueued = this[KEY_CHECK_QUEUED] ?: false,
        trimConfirmPending = this[KEY_TRIM_CONFIRM] ?: false,
    )

    private companion object {
        val KEY_COUNT = intPreferencesKey("last_update_count")
        val KEY_EPOCH = longPreferencesKey("last_checked_epoch_ms")
        val KEY_CHECK_QUEUED = booleanPreferencesKey("check_queued")
        val KEY_TRIM_CONFIRM = booleanPreferencesKey("trim_confirm_pending")
    }
}
