package dev.involvex.kiln.feature.sources.forge

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.model.ForgeTokenRepository
import dev.involvex.kiln.core.model.ReleaseForge
import javax.inject.Inject
import javax.inject.Singleton

/** Encrypted tokens for GitLab.com / Codeberg (GitHub stays in [dev.involvex.kiln.feature.sources.github.GitHubTokenStore]). */
@Singleton
class ForgeTokenStore @Inject constructor(@ApplicationContext context: Context) : ForgeTokenRepository {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "kiln_forge_secrets",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun getToken(forge: ReleaseForge): String? {
        val key = keyFor(forge) ?: return null
        return prefs.getString(key, null)?.takeIf { it.isNotBlank() }
    }

    fun setToken(forge: ReleaseForge, token: String?) {
        val key = keyFor(forge) ?: return
        prefs.edit().apply {
            if (token.isNullOrBlank()) {
                remove(key)
            } else {
                putString(key, token.trim())
            }
        }.apply()
    }

    override fun isTokenSet(forge: ReleaseForge): Boolean = getToken(forge) != null

    override fun saveToken(forge: ReleaseForge, token: String) {
        setToken(forge, token)
    }

    override fun clearToken(forge: ReleaseForge) {
        setToken(forge, null)
    }

    private fun keyFor(forge: ReleaseForge): String? = when (forge) {
        ReleaseForge.GITLAB -> KEY_GITLAB
        ReleaseForge.CODEBERG -> KEY_CODEBERG
        ReleaseForge.GITHUB -> null
    }

    private companion object {
        const val KEY_GITLAB = "gitlab_pat"
        const val KEY_CODEBERG = "codeberg_token"
    }
}
