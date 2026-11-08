package dev.involvex.kiln.feature.settings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.model.InstallerPreferences
import dev.involvex.kiln.core.model.PreferredInstaller
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.installerPrefsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "kiln_installer_prefs",
)

@Singleton
class DataStoreInstallerPreferences @Inject constructor(@ApplicationContext context: Context) : InstallerPreferences {
    private val store = context.installerPrefsDataStore

    override fun observe(): Flow<PreferredInstaller> = store.data.map { it.toPreferred() }

    override suspend fun current(): PreferredInstaller = store.data.first().toPreferred()

    override suspend fun setPreferred(preferred: PreferredInstaller) {
        store.edit { it[KEY_PREFERRED] = preferred.name }
    }

    private fun Preferences.toPreferred(): PreferredInstaller =
        this[KEY_PREFERRED]?.let { runCatching { PreferredInstaller.valueOf(it) }.getOrNull() }
            ?: PreferredInstaller.SYSTEM

    private companion object {
        val KEY_PREFERRED = stringPreferencesKey("preferred_installer")
    }
}
