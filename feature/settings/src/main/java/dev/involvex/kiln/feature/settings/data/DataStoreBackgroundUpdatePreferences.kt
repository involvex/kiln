package dev.involvex.kiln.feature.settings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.model.BackgroundUpdatePreferences
import dev.involvex.kiln.core.model.BackgroundUpdatePrefs
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.backgroundUpdateDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "kiln_background_updates",
)

@Singleton
class DataStoreBackgroundUpdatePreferences @Inject constructor(@ApplicationContext context: Context) : BackgroundUpdatePreferences {
    private val store = context.backgroundUpdateDataStore

    override fun observe(): Flow<BackgroundUpdatePrefs> = store.data.map { it.toPrefs() }

    override suspend fun current(): BackgroundUpdatePrefs = store.data.first().toPrefs()

    override suspend fun setEnabled(enabled: Boolean) {
        store.edit { it[KEY_ENABLED] = enabled }
    }

    override suspend fun setUnmeteredOnly(unmeteredOnly: Boolean) {
        store.edit { it[KEY_UNMETERED] = unmeteredOnly }
    }

    override suspend fun setAutoInstallMatching(autoInstallMatching: Boolean) {
        store.edit { it[KEY_AUTO_INSTALL] = autoInstallMatching }
    }

    override suspend fun setUpdateCooldownDays(days: Int) {
        store.edit { it[KEY_COOLDOWN_DAYS] = days.coerceIn(0, MAX_COOLDOWN_DAYS) }
    }

    private fun Preferences.toPrefs() = BackgroundUpdatePrefs(
        enabled = this[KEY_ENABLED] ?: false,
        unmeteredOnly = this[KEY_UNMETERED] ?: true,
        autoInstallMatching = this[KEY_AUTO_INSTALL] ?: false,
        updateCooldownDays = this[KEY_COOLDOWN_DAYS] ?: 0,
    )

    private companion object {
        val KEY_ENABLED = booleanPreferencesKey("background_updates_enabled")
        val KEY_UNMETERED = booleanPreferencesKey("unmetered_only")
        val KEY_AUTO_INSTALL = booleanPreferencesKey("auto_install_matching")
        val KEY_COOLDOWN_DAYS = intPreferencesKey("update_cooldown_days")
        const val MAX_COOLDOWN_DAYS = 30
    }
}
