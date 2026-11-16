package dev.involvex.kiln.feature.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.designsystem.kilnHiltViewModel
import dev.involvex.kiln.core.designsystem.theme.allowsDynamicColor
import dev.involvex.kiln.core.model.AppearancePrefs
import dev.involvex.kiln.core.model.KilnFontStyle
import dev.involvex.kiln.core.model.KilnLanguage
import dev.involvex.kiln.core.model.KilnThemeStyle

@Composable
fun SettingsAppearanceRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = kilnHiltViewModel<SettingsViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsCategoryScaffold(
        title = stringResource(R.string.settings_hub_appearance),
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        SettingsScrollColumn(padding = padding) {
            AppearanceSection(
                prefs = state.appearance,
                onThemeChange = viewModel::setTheme,
                onLanguageChange = viewModel::setLanguage,
                onFontStyleChange = viewModel::setFontStyle,
                onDynamicColorChange = viewModel::setDynamicColor,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AppearanceSection(
    prefs: AppearancePrefs,
    onThemeChange: (KilnThemeStyle) -> Unit,
    onLanguageChange: (KilnLanguage) -> Unit,
    onFontStyleChange: (KilnFontStyle) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
) {
    Text(
        text = stringResource(R.string.settings_theme),
        style = MaterialTheme.typography.bodyLarge,
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ThemeChip(KilnThemeStyle.SYSTEM, prefs.theme, onThemeChange, stringResource(R.string.settings_theme_system))
        ThemeChip(KilnThemeStyle.LIGHT, prefs.theme, onThemeChange, stringResource(R.string.settings_theme_light))
        ThemeChip(KilnThemeStyle.DARK, prefs.theme, onThemeChange, stringResource(R.string.settings_theme_dark))
        ThemeChip(KilnThemeStyle.AMOLED, prefs.theme, onThemeChange, stringResource(R.string.settings_theme_amoled))
        ThemeChip(KilnThemeStyle.MONOKAI, prefs.theme, onThemeChange, stringResource(R.string.settings_theme_monokai))
        ThemeChip(KilnThemeStyle.OCEAN, prefs.theme, onThemeChange, stringResource(R.string.settings_theme_ocean))
        ThemeChip(KilnThemeStyle.HACKER, prefs.theme, onThemeChange, stringResource(R.string.settings_theme_hacker))
    }
    val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val dynamicAllowed = prefs.theme.allowsDynamicColor()
    SettingsSwitchRow(
        title = stringResource(R.string.settings_dynamic_color),
        checked = prefs.dynamicColor && dynamicAllowed,
        onCheckedChange = onDynamicColorChange,
        enabled = dynamicSupported && dynamicAllowed,
    )
    Text(
        text = stringResource(R.string.settings_dynamic_color_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = stringResource(R.string.settings_language),
        style = MaterialTheme.typography.bodyLarge,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LanguageChip(KilnLanguage.SYSTEM, prefs.language, onLanguageChange, stringResource(R.string.settings_language_system))
        LanguageChip(KilnLanguage.ENGLISH, prefs.language, onLanguageChange, stringResource(R.string.settings_language_english))
        LanguageChip(KilnLanguage.GERMAN, prefs.language, onLanguageChange, stringResource(R.string.settings_language_german))
    }
    Text(
        text = stringResource(R.string.settings_font_style),
        style = MaterialTheme.typography.bodyLarge,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FontChip(KilnFontStyle.DEFAULT, prefs.fontStyle, onFontStyleChange, stringResource(R.string.settings_font_default))
        FontChip(KilnFontStyle.ROUNDED_DISPLAY, prefs.fontStyle, onFontStyleChange, stringResource(R.string.settings_font_rounded))
    }
}

@Composable
private fun FontChip(value: KilnFontStyle, selected: KilnFontStyle, onChange: (KilnFontStyle) -> Unit, label: String) {
    FilterChip(
        selected = selected == value,
        onClick = { onChange(value) },
        label = { Text(label) },
    )
}

@Composable
private fun ThemeChip(value: KilnThemeStyle, selected: KilnThemeStyle, onChange: (KilnThemeStyle) -> Unit, label: String) {
    FilterChip(
        selected = selected == value,
        onClick = { onChange(value) },
        label = { Text(label) },
    )
}

@Composable
private fun LanguageChip(value: KilnLanguage, selected: KilnLanguage, onChange: (KilnLanguage) -> Unit, label: String) {
    FilterChip(
        selected = selected == value,
        onClick = { onChange(value) },
        label = { Text(label) },
    )
}
