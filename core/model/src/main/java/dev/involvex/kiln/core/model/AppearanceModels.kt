package dev.involvex.kiln.core.model

import kotlinx.coroutines.flow.Flow

enum class KilnThemeStyle {
    SYSTEM,
    LIGHT,
    DARK,
    AMOLED,
    MONOKAI,
    OCEAN,
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
    val dynamicColor: Boolean = false,
)

interface AppearancePreferences {
    fun observe(): Flow<AppearancePrefs>

    suspend fun current(): AppearancePrefs

    suspend fun setTheme(theme: KilnThemeStyle)

    suspend fun setLanguage(language: KilnLanguage)

    suspend fun setFontStyle(fontStyle: KilnFontStyle)

    suspend fun setDynamicColor(enabled: Boolean)
}
