package dev.involvex.kiln.feature.backup.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.model.WebDavSecretRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebDavCredentialsStore @Inject constructor(@ApplicationContext context: Context) : WebDavSecretRepository {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "kiln_webdav_secrets",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override fun getPassword(): String? = prefs.getString(KEY_PASSWORD, null)?.takeIf { it.isNotEmpty() }

    override fun isPasswordSet(): Boolean = getPassword() != null

    override fun savePassword(password: String) {
        prefs.edit().putString(KEY_PASSWORD, password).apply()
    }

    override fun clearPassword() {
        prefs.edit().remove(KEY_PASSWORD).apply()
    }

    private companion object {
        const val KEY_PASSWORD = "webdav_password"
    }
}
