package dev.involvex.kiln.feature.sources.github

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.model.GitHubAuthHeaderProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubTokenStore @Inject constructor(@ApplicationContext context: Context) : GitHubAuthHeaderProvider {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "kiln_github_secrets",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun getToken(): String? = prefs.getString(KEY_PAT, null)?.takeIf { it.isNotBlank() }

    fun setToken(token: String?) {
        prefs.edit().apply {
            if (token.isNullOrBlank()) {
                remove(KEY_PAT)
            } else {
                putString(KEY_PAT, token.trim())
            }
        }.apply()
    }

    override suspend fun bearerHeaders(): Map<String, String> {
        val token = getToken() ?: return emptyMap()
        return mapOf(
            "Authorization" to "Bearer $token",
            "Accept" to "application/vnd.github+json",
            "X-GitHub-Api-Version" to "2022-11-28",
        )
    }

    override suspend fun hasToken(): Boolean = getToken() != null

    companion object {
        private const val KEY_PAT = "github_pat"
    }
}
