package dev.involvex.kiln.feature.termux.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.model.TermuxPreferences
import dev.involvex.kiln.core.model.TermuxPrefs
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.termuxPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "kiln_termux_prefs",
)

@Singleton
class DataStoreTermuxPreferences @Inject constructor(@ApplicationContext context: Context) : TermuxPreferences {
    private val store = context.termuxPrefsDataStore

    override fun observe(): Flow<TermuxPrefs> = store.data.map { it.toPrefs() }

    override suspend fun current(): TermuxPrefs = store.data.first().toPrefs()

    override suspend fun setEnabled(enabled: Boolean) {
        store.edit { it[KEY_ENABLED] = enabled }
    }

    override suspend fun setPackageId(packageId: String) {
        val normalized = packageId.trim().ifBlank { TermuxPrefs.DEFAULT_TERMUX_PACKAGE_ID }
        store.edit { it[KEY_PACKAGE_ID] = normalized }
    }

    private fun Preferences.toPrefs() = TermuxPrefs(
        enabled = this[KEY_ENABLED] ?: false,
        packageId = this[KEY_PACKAGE_ID] ?: TermuxPrefs.DEFAULT_TERMUX_PACKAGE_ID,
    )

    private companion object {
        val KEY_ENABLED = booleanPreferencesKey("termux_enabled")
        val KEY_PACKAGE_ID = stringPreferencesKey("termux_package_id")
    }
}
