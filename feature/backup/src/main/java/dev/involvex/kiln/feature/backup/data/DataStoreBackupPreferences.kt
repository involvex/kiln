package dev.involvex.kiln.feature.backup.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.model.BackupDestinationKind
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

    override suspend fun setDestinationKind(kind: BackupDestinationKind) {
        store.edit { it[KEY_DEST_KIND] = kind.name }
    }

    override suspend fun setWebDavBaseUrl(url: String) {
        store.edit { it[KEY_WEBDAV_URL] = url.trim() }
    }

    override suspend fun setWebDavUsername(username: String) {
        store.edit { it[KEY_WEBDAV_USER] = username.trim() }
    }

    override suspend fun setWebDavRemotePath(path: String) {
        store.edit { it[KEY_WEBDAV_PATH] = path.trim() }
    }

    private fun Preferences.toPrefs() = BackupPrefs(
        treeUri = this[KEY_TREE_URI],
        keepN = BackupPrefs.clampKeepN(this[KEY_KEEP_N] ?: BackupPrefs.DEFAULT_KEEP_N),
        destinationKind = runCatching {
            BackupDestinationKind.valueOf(this[KEY_DEST_KIND] ?: BackupDestinationKind.SAF.name)
        }.getOrDefault(BackupDestinationKind.SAF),
        webdavBaseUrl = this[KEY_WEBDAV_URL].orEmpty(),
        webdavUsername = this[KEY_WEBDAV_USER].orEmpty(),
        webdavRemotePath = this[KEY_WEBDAV_PATH]?.ifBlank { null } ?: BackupPrefs.DEFAULT_WEBDAV_PATH,
    )

    private companion object {
        val KEY_TREE_URI = stringPreferencesKey("tree_uri")
        val KEY_KEEP_N = intPreferencesKey("keep_n")
        val KEY_DEST_KIND = stringPreferencesKey("destination_kind")
        val KEY_WEBDAV_URL = stringPreferencesKey("webdav_base_url")
        val KEY_WEBDAV_USER = stringPreferencesKey("webdav_username")
        val KEY_WEBDAV_PATH = stringPreferencesKey("webdav_remote_path")
    }
}
