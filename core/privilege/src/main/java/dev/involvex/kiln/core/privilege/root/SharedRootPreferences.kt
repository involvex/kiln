package dev.involvex.kiln.core.privilege.root

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.model.RootPreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Default-off Magisk root opt-in. SharedPreferences so [isEnabled] stays synchronous for status.
 */
@Singleton
class SharedRootPreferences @Inject constructor(@ApplicationContext context: Context) : RootPreferences {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val enabled = MutableStateFlow(prefs.getBoolean(KEY_ENABLED, false))

    override fun observeEnabled(): Flow<Boolean> = enabled.asStateFlow()

    override fun isEnabled(): Boolean = enabled.value

    override fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        this.enabled.value = enabled
    }

    private companion object {
        const val PREFS_NAME = "kiln_root_prefs"
        const val KEY_ENABLED = "root_opt_in"
    }
}
