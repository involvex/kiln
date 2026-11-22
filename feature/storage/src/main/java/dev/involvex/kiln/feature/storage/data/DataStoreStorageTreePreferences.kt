package dev.involvex.kiln.feature.storage.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.model.StorageTreePreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.storageTreeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "kiln_storage_tree",
)

@Singleton
class DataStoreStorageTreePreferences @Inject constructor(@ApplicationContext context: Context) : StorageTreePreferences {
    private val store = context.storageTreeDataStore

    override suspend fun treeUri(): String? = store.data.map { it[KEY_TREE_URI] }.first()

    override suspend fun setTreeUri(uri: String?) {
        store.edit { prefs ->
            if (uri.isNullOrBlank()) {
                prefs.remove(KEY_TREE_URI)
            } else {
                prefs[KEY_TREE_URI] = uri
            }
        }
    }

    private companion object {
        val KEY_TREE_URI = stringPreferencesKey("tree_uri")
    }
}
