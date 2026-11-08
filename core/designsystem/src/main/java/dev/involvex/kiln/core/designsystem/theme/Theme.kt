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

private fun ColorScheme.amoled(): ColorScheme = copy(
    background = AmoledBlack,
    surface = AmoledBlack,
    surfaceContainerLowest = AmoledBlack,
    surfaceContainerLow = AmoledSurfaceContainerLow,
    surfaceContainer = AmoledSurfaceContainer,
    surfaceContainerHigh = AmoledSurfaceContainerHigh,
    surfaceContainerHighest = AmoledSurfaceContainerHighest,
)

@Composable
fun KilnTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    amoledDark: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        // Dynamic Color requires Android 12+ (API 31).
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) {
                dynamicDarkColorScheme(context).let { if (amoledDark) it.amoled() else it }
            } else {
                dynamicLightColorScheme(context)
            }
        }

        darkTheme -> if (amoledDark) DarkColorScheme.amoled() else DarkColorScheme

        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalAmoledDark provides amoledDark) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = KilnTypography,
            content = content,
        )
    }
}
