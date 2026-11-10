package dev.involvex.kiln.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import dev.involvex.kiln.core.model.KilnFontStyle
import dev.involvex.kiln.core.model.KilnThemeStyle

val LocalAmoledDark = staticCompositionLocalOf { false }

private val LightColorScheme = lightColorScheme(
    primary = KilnLightPrimary,
    onPrimary = KilnLightOnPrimary,
    primaryContainer = KilnLightPrimaryContainer,
    onPrimaryContainer = KilnLightOnPrimaryContainer,
    secondary = KilnLightSecondary,
    onSecondary = KilnLightOnSecondary,
    secondaryContainer = KilnLightSecondaryContainer,
    onSecondaryContainer = KilnLightOnSecondaryContainer,
    tertiary = KilnLightTertiary,
    onTertiary = KilnLightOnTertiary,
    tertiaryContainer = KilnLightTertiaryContainer,
    onTertiaryContainer = KilnLightOnTertiaryContainer,
    error = KilnLightError,
    onError = KilnLightOnError,
    errorContainer = KilnLightErrorContainer,
    onErrorContainer = KilnLightOnErrorContainer,
    background = KilnLightBackground,
    onBackground = KilnLightOnBackground,
    surface = KilnLightSurface,
    onSurface = KilnLightOnSurface,
    surfaceVariant = KilnLightSurfaceVariant,
    onSurfaceVariant = KilnLightOnSurfaceVariant,
    outline = KilnLightOutline,
)

private val DarkColorScheme = darkColorScheme(
    primary = KilnDarkPrimary,
    onPrimary = KilnDarkOnPrimary,
    primaryContainer = KilnDarkPrimaryContainer,
    onPrimaryContainer = KilnDarkOnPrimaryContainer,
    secondary = KilnDarkSecondary,
    onSecondary = KilnDarkOnSecondary,
    secondaryContainer = KilnDarkSecondaryContainer,
    onSecondaryContainer = KilnDarkOnSecondaryContainer,
    tertiary = KilnDarkTertiary,
    onTertiary = KilnDarkOnTertiary,
    tertiaryContainer = KilnDarkTertiaryContainer,
    onTertiaryContainer = KilnDarkOnTertiaryContainer,
    error = KilnDarkError,
    onError = KilnDarkOnError,
    errorContainer = KilnDarkErrorContainer,
    onErrorContainer = KilnDarkOnErrorContainer,
    background = KilnDarkBackground,
    onBackground = KilnDarkOnBackground,
    surface = KilnDarkSurface,
    onSurface = KilnDarkOnSurface,
    surfaceVariant = KilnDarkSurfaceVariant,
    onSurfaceVariant = KilnDarkOnSurfaceVariant,
    outline = KilnDarkOutline,
)

private val MonokaiColorScheme = darkColorScheme(
    primary = MonokaiPrimary,
    onPrimary = MonokaiOnPrimary,
    primaryContainer = MonokaiPrimaryContainer,
    onPrimaryContainer = MonokaiOnPrimaryContainer,
    secondary = MonokaiSecondary,
    onSecondary = MonokaiOnSecondary,
    secondaryContainer = MonokaiSecondaryContainer,
    onSecondaryContainer = MonokaiOnSecondaryContainer,
    tertiary = MonokaiTertiary,
    onTertiary = MonokaiOnTertiary,
    tertiaryContainer = MonokaiTertiaryContainer,
    onTertiaryContainer = MonokaiOnTertiaryContainer,
    error = MonokaiError,
    onError = MonokaiOnError,
    errorContainer = MonokaiErrorContainer,
    onErrorContainer = MonokaiOnErrorContainer,
    background = MonokaiBackground,
    onBackground = MonokaiOnBackground,
    surface = MonokaiSurface,
    onSurface = MonokaiOnSurface,
    surfaceVariant = MonokaiSurfaceVariant,
    onSurfaceVariant = MonokaiOnSurfaceVariant,
    outline = MonokaiOutline,
)

private val OceanColorScheme = darkColorScheme(
    primary = OceanPrimary,
    onPrimary = OceanOnPrimary,
    primaryContainer = OceanPrimaryContainer,
    onPrimaryContainer = OceanOnPrimaryContainer,
    secondary = OceanSecondary,
    onSecondary = OceanOnSecondary,
    secondaryContainer = OceanSecondaryContainer,
    onSecondaryContainer = OceanOnSecondaryContainer,
    tertiary = OceanTertiary,
    onTertiary = OceanOnTertiary,
    tertiaryContainer = OceanTertiaryContainer,
    onTertiaryContainer = OceanOnTertiaryContainer,
    error = OceanError,
    onError = OceanOnError,
    errorContainer = OceanErrorContainer,
    onErrorContainer = OceanOnErrorContainer,
    background = OceanBackground,
    onBackground = OceanOnBackground,
    surface = OceanSurface,
    onSurface = OceanOnSurface,
    surfaceVariant = OceanSurfaceVariant,
    onSurfaceVariant = OceanOnSurfaceVariant,
    outline = OceanOutline,
)

private fun ColorScheme.amoled(): ColorScheme = copy(
    background = AmoledBlack,
    surface = AmoledBlack,
    surfaceContainerLowest = AmoledBlack,
    surfaceContainerLow = AmoledSurfaceContainerLow,
    surfaceContainer = AmoledSurfaceContainer,
    surfaceContainerHigh = AmoledSurfaceContainerHigh,
    surfaceContainerHighest = AmoledSurfaceContainerHighest,
)

/** True when status/nav bars should use dark icons (light surfaces). */
fun KilnThemeStyle.usesLightSystemBars(systemDark: Boolean): Boolean = when (this) {
    KilnThemeStyle.LIGHT -> true

    KilnThemeStyle.SYSTEM -> !systemDark

    KilnThemeStyle.DARK,
    KilnThemeStyle.AMOLED,
    KilnThemeStyle.MONOKAI,
    KilnThemeStyle.OCEAN,
    -> false
}

/** Fixed palettes ignore Material You Dynamic Color. */
fun KilnThemeStyle.allowsDynamicColor(): Boolean = when (this) {
    KilnThemeStyle.MONOKAI,
    KilnThemeStyle.OCEAN,
    -> false

    KilnThemeStyle.SYSTEM,
    KilnThemeStyle.LIGHT,
    KilnThemeStyle.DARK,
    KilnThemeStyle.AMOLED,
    -> true
}

@Composable
fun KilnTheme(
    themeStyle: KilnThemeStyle = KilnThemeStyle.DARK,
    fontStyle: KilnFontStyle = KilnFontStyle.DEFAULT,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val amoledDark = themeStyle == KilnThemeStyle.AMOLED
    val useDynamic = dynamicColor && themeStyle.allowsDynamicColor()
    val colorScheme = when (themeStyle) {
        KilnThemeStyle.SYSTEM -> {
            if (systemDark) {
                resolveDark(useDynamic, amoled = false)
            } else {
                resolveLight(useDynamic)
            }
        }

        KilnThemeStyle.LIGHT -> resolveLight(useDynamic)

        KilnThemeStyle.DARK -> resolveDark(useDynamic, amoled = false)

        KilnThemeStyle.AMOLED -> resolveDark(useDynamic, amoled = true)

        KilnThemeStyle.MONOKAI -> MonokaiColorScheme

        KilnThemeStyle.OCEAN -> OceanColorScheme
    }

    CompositionLocalProvider(LocalAmoledDark provides amoledDark) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = kilnTypography(fontStyle),
            content = content,
        )
    }
}

/** Backward-compatible entry for previews that still pass darkTheme/amoledDark. */
@Composable
fun KilnTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    amoledDark: Boolean = false,
    content: @Composable () -> Unit,
) {
    val style = when {
        amoledDark -> KilnThemeStyle.AMOLED
        darkTheme -> KilnThemeStyle.DARK
        else -> KilnThemeStyle.LIGHT
    }
    KilnTheme(themeStyle = style, dynamicColor = dynamicColor, content = content)
}

@Composable
private fun resolveLight(dynamicColor: Boolean): ColorScheme {
    // Dynamic Color requires Android 12+ (API 31).
    if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        return dynamicLightColorScheme(LocalContext.current)
    }
    return LightColorScheme
}

@Composable
private fun resolveDark(dynamicColor: Boolean, amoled: Boolean): ColorScheme {
    // Dynamic Color requires Android 12+ (API 31).
    if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val dynamic = dynamicDarkColorScheme(LocalContext.current)
        return if (amoled) dynamic.amoled() else dynamic
    }
    return if (amoled) DarkColorScheme.amoled() else DarkColorScheme
}
