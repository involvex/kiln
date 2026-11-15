package dev.involvex.kiln.feature.sources.exodus

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.model.ExodusPrivacyPreferences
import dev.involvex.kiln.core.model.ExodusPrivacyPrefs
import dev.involvex.kiln.core.model.ExodusReportUrls
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class DefaultExodusPrivacyPreferences @Inject constructor(@ApplicationContext context: Context) : ExodusPrivacyPreferences {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "kiln_exodus_secrets",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private val state = MutableStateFlow(readPrefs())

    override fun observe(): Flow<ExodusPrivacyPrefs> = state.asStateFlow()

    override suspend fun current(): ExodusPrivacyPrefs = state.value

    override suspend fun setApiBaseUrl(url: String) {
        val normalized = ExodusReportUrls.normalizeApiBase(url)
        prefs.edit().apply {
            if (normalized.isBlank()) remove(KEY_BASE) else putString(KEY_BASE, normalized)
        }.apply()
        state.value = readPrefs()
    }

    override suspend fun setToken(token: String?) {
        prefs.edit().apply {
            if (token.isNullOrBlank()) {
                remove(KEY_TOKEN)
            } else {
                putString(KEY_TOKEN, token.trim())
            }
        }.apply()
        state.value = readPrefs()
    }

    override fun peekToken(): String? = prefs.getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() }

    private fun readPrefs() = ExodusPrivacyPrefs(
        apiBaseUrl = prefs.getString(KEY_BASE, null).orEmpty(),
        tokenSet = !prefs.getString(KEY_TOKEN, null).isNullOrBlank(),
    )

    companion object {
        private const val KEY_BASE = "exodus_api_base"
        private const val KEY_TOKEN = "exodus_api_token"
    }
}
