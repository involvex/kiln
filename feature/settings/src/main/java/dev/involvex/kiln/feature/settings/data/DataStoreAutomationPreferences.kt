package dev.involvex.kiln.feature.settings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.model.AutomationPreferences
import dev.involvex.kiln.core.model.AutomationPrefs
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.automationPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "kiln_automation_prefs",
)

@Singleton
class DataStoreAutomationPreferences @Inject constructor(@ApplicationContext context: Context) : AutomationPreferences {
    private val store = context.automationPrefsDataStore

    override fun observe(): Flow<AutomationPrefs> = store.data.map { it.toPrefs() }

    override suspend fun current(): AutomationPrefs = store.data.first().toPrefs()

    override suspend fun setEnabled(enabled: Boolean) {
        store.edit { it[KEY_ENABLED] = enabled }
    }

    private fun Preferences.toPrefs() = AutomationPrefs(
        enabled = this[KEY_ENABLED] ?: false,
    )

    private companion object {
        val KEY_ENABLED = booleanPreferencesKey("automation_enabled")
    }
}
