package dev.involvex.kiln.feature.backup.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.model.BackupPreferences
import dev.involvex.kiln.core.model.BackupPrefs
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.backupDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "kiln_backup",
)

@Singleton
class DataStoreBackupPreferences @Inject constructor(@ApplicationContext context: Context) : BackupPreferences {
    private val store = context.backupDataStore

    override fun observe(): Flow<BackupPrefs> = store.data.map { it.toPrefs() }

    override suspend fun current(): BackupPrefs = store.data.first().toPrefs()

    override suspend fun setTreeUri(uri: String?) {
        store.edit { prefs ->
            if (uri.isNullOrBlank()) {
                prefs.remove(KEY_TREE_URI)
            } else {
                prefs[KEY_TREE_URI] = uri
            }
        }
    }

    override suspend fun setKeepN(keepN: Int) {
        store.edit { it[KEY_KEEP_N] = BackupPrefs.clampKeepN(keepN) }
    }

    private fun Preferences.toPrefs() = BackupPrefs(
        treeUri = this[KEY_TREE_URI],
        keepN = BackupPrefs.clampKeepN(this[KEY_KEEP_N] ?: BackupPrefs.DEFAULT_KEEP_N),
    )

    private companion object {
        val KEY_TREE_URI = stringPreferencesKey("tree_uri")
        val KEY_KEEP_N = intPreferencesKey("keep_n")
    }
}
