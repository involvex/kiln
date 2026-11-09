package dev.involvex.kiln.feature.settings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.model.MaintenancePreferences
import dev.involvex.kiln.core.model.MaintenancePrefs
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.maintenanceDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "kiln_maintenance",
)

@Singleton
class DataStoreMaintenancePreferences @Inject constructor(@ApplicationContext context: Context) : MaintenancePreferences {
    private val store = context.maintenanceDataStore

    override fun observe(): Flow<MaintenancePrefs> = store.data.map { it.toPrefs() }

    override suspend fun current(): MaintenancePrefs = store.data.first().toPrefs()

    override suspend fun setEnabled(enabled: Boolean) {
        store.edit { it[KEY_ENABLED] = enabled }
    }

    override suspend fun setTrimCaches(enabled: Boolean) {
        store.edit { it[KEY_TRIM] = enabled }
    }

    override suspend fun setPruneDownloads(enabled: Boolean) {
        store.edit { it[KEY_DOWNLOADS] = enabled }
    }

    override suspend fun setPruneRollback(enabled: Boolean) {
        store.edit { it[KEY_ROLLBACK] = enabled }
    }

    override suspend fun setPruneBackupTemp(enabled: Boolean) {
        store.edit { it[KEY_BACKUP_TEMP] = enabled }
    }

    private fun Preferences.toPrefs() = MaintenancePrefs(
        enabled = this[KEY_ENABLED] ?: false,
        trimCaches = this[KEY_TRIM] ?: true,
        pruneDownloads = this[KEY_DOWNLOADS] ?: true,
        pruneRollback = this[KEY_ROLLBACK] ?: true,
        pruneBackupTemp = this[KEY_BACKUP_TEMP] ?: true,
    )

    private companion object {
        val KEY_ENABLED = booleanPreferencesKey("maintenance_enabled")
        val KEY_TRIM = booleanPreferencesKey("trim_caches")
        val KEY_DOWNLOADS = booleanPreferencesKey("prune_downloads")
        val KEY_ROLLBACK = booleanPreferencesKey("prune_rollback")
        val KEY_BACKUP_TEMP = booleanPreferencesKey("prune_backup_temp")
    }
}
