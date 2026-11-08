package dev.involvex.kiln.core.model

import kotlinx.coroutines.flow.Flow

enum class KilnThemeStyle {
    LIGHT,
    DARK,
    AMOLED,
    MONOKAI,
}

enum class KilnLanguage {
    SYSTEM,
    ENGLISH,
    GERMAN,
}

data class AppearancePrefs(val theme: KilnThemeStyle = KilnThemeStyle.DARK, val language: KilnLanguage = KilnLanguage.SYSTEM)

interface AppearancePreferences {
    fun observe(): Flow<AppearancePrefs>

    suspend fun current(): AppearancePrefs

    suspend fun setTheme(theme: KilnThemeStyle)

    suspend fun setLanguage(language: KilnLanguage)
}
