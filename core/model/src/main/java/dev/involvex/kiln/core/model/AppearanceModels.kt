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

enum class KilnFontStyle {
    DEFAULT,
    ROUNDED_DISPLAY,
}

data class AppearancePrefs(
    val theme: KilnThemeStyle = KilnThemeStyle.DARK,
    val language: KilnLanguage = KilnLanguage.SYSTEM,
    val fontStyle: KilnFontStyle = KilnFontStyle.DEFAULT,
)

interface AppearancePreferences {
    fun observe(): Flow<AppearancePrefs>

    suspend fun current(): AppearancePrefs

    suspend fun setTheme(theme: KilnThemeStyle)

    suspend fun setLanguage(language: KilnLanguage)

    suspend fun setFontStyle(fontStyle: KilnFontStyle)
}
