package dev.involvex.kiln.feature.settings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.model.DevPreferences
import dev.involvex.kiln.core.model.DevPrefs
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.devPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "kiln_dev_prefs",
)

@Singleton
class DataStoreDevPreferences @Inject constructor(@ApplicationContext context: Context) : DevPreferences {
    private val store = context.devPrefsDataStore

    override fun observe(): Flow<DevPrefs> = store.data.map { it.toPrefs() }

    override suspend fun current(): DevPrefs = store.data.first().toPrefs()

    override suspend fun setDebugLogEnabled(enabled: Boolean) {
        store.edit { it[KEY_DEBUG_LOG] = enabled }
    }

    private fun Preferences.toPrefs() = DevPrefs(
        debugLogEnabled = this[KEY_DEBUG_LOG] ?: false,
    )

    private companion object {
        val KEY_DEBUG_LOG = booleanPreferencesKey("debug_log_enabled")
    }
}
