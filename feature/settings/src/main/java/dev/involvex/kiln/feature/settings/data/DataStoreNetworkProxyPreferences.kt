package dev.involvex.kiln.feature.settings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.model.NetworkProxyPreferences
import dev.involvex.kiln.core.model.NetworkProxyPrefs
import dev.involvex.kiln.core.model.NetworkProxyType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.networkProxyDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "kiln_network_proxy",
)

@Singleton
class DataStoreNetworkProxyPreferences @Inject constructor(@ApplicationContext context: Context) : NetworkProxyPreferences {
    private val store = context.networkProxyDataStore

    override fun observe(): Flow<NetworkProxyPrefs> = store.data.map { it.toPrefs() }

    override suspend fun current(): NetworkProxyPrefs = store.data.first().toPrefs()

    override suspend fun setType(type: NetworkProxyType) {
        store.edit { it[KEY_TYPE] = type.name }
    }

    override suspend fun setHost(host: String) {
        store.edit { it[KEY_HOST] = host.trim() }
    }

    override suspend fun setPort(port: Int) {
        store.edit { it[KEY_PORT] = port.coerceIn(0, NetworkProxyPrefs.MAX_TCP_PORT) }
    }

    override suspend fun setUsername(username: String) {
        store.edit { it[KEY_USERNAME] = username }
    }

    override suspend fun setPassword(password: String) {
        store.edit { it[KEY_PASSWORD] = password }
    }

    override suspend fun setOrbotPreset(enabled: Boolean) {
        store.edit { prefs ->
            prefs[KEY_ORBOT] = enabled
            if (enabled) {
                prefs[KEY_TYPE] = NetworkProxyType.SOCKS.name
                prefs[KEY_HOST] = NetworkProxyPrefs.ORBOT_HOST
                prefs[KEY_PORT] = NetworkProxyPrefs.ORBOT_SOCKS_PORT
            }
        }
    }

    private fun Preferences.toPrefs() = NetworkProxyPrefs(
        type = runCatching { NetworkProxyType.valueOf(this[KEY_TYPE] ?: NetworkProxyType.NONE.name) }
            .getOrDefault(NetworkProxyType.NONE),
        host = this[KEY_HOST].orEmpty(),
        port = this[KEY_PORT] ?: 0,
        username = this[KEY_USERNAME].orEmpty(),
        password = this[KEY_PASSWORD].orEmpty(),
        orbotPreset = this[KEY_ORBOT] ?: false,
    )

    private companion object {
        val KEY_TYPE = stringPreferencesKey("proxy_type")
        val KEY_HOST = stringPreferencesKey("proxy_host")
        val KEY_PORT = intPreferencesKey("proxy_port")
        val KEY_USERNAME = stringPreferencesKey("proxy_username")
        val KEY_PASSWORD = stringPreferencesKey("proxy_password")
        val KEY_ORBOT = booleanPreferencesKey("proxy_orbot")
    }
}
