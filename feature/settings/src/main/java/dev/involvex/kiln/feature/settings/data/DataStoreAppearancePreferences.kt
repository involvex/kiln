package dev.involvex.kiln.feature.settings.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.model.AppearancePreferences
import dev.involvex.kiln.core.model.AppearancePrefs
import dev.involvex.kiln.core.model.KilnFontStyle
import dev.involvex.kiln.core.model.KilnLanguage
import dev.involvex.kiln.core.model.KilnThemeStyle
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.appearanceDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "kiln_appearance",
)

@Singleton
class DataStoreAppearancePreferences @Inject constructor(@ApplicationContext context: Context) : AppearancePreferences {
    private val store = context.appearanceDataStore

    override fun observe(): Flow<AppearancePrefs> = store.data.map { it.toPrefs() }

    override suspend fun current(): AppearancePrefs = store.data.first().toPrefs()

    override suspend fun setTheme(theme: KilnThemeStyle) {
        store.edit { it[KEY_THEME] = theme.name }
    }

    override suspend fun setLanguage(language: KilnLanguage) {
        store.edit { it[KEY_LANGUAGE] = language.name }
    }

    override suspend fun setFontStyle(fontStyle: KilnFontStyle) {
        store.edit { it[KEY_FONT] = fontStyle.name }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        store.edit { it[KEY_DYNAMIC] = enabled }
    }

    private fun Preferences.toPrefs() = AppearancePrefs(
        theme = this[KEY_THEME]?.let { runCatching { KilnThemeStyle.valueOf(it) }.getOrNull() }
            ?: KilnThemeStyle.DARK,
        language = this[KEY_LANGUAGE]?.let { runCatching { KilnLanguage.valueOf(it) }.getOrNull() }
            ?: KilnLanguage.SYSTEM,
        fontStyle = this[KEY_FONT]?.let { runCatching { KilnFontStyle.valueOf(it) }.getOrNull() }
            ?: KilnFontStyle.DEFAULT,
        dynamicColor = this[KEY_DYNAMIC] ?: false,
    )

    private companion object {
        val KEY_THEME = stringPreferencesKey("theme")
        val KEY_LANGUAGE = stringPreferencesKey("language")
        val KEY_FONT = stringPreferencesKey("font_style")
        val KEY_DYNAMIC = booleanPreferencesKey("dynamic_color")
    }
}
