package dev.involvex.kiln.feature.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.designsystem.component.KilnScaffold
import dev.involvex.kiln.core.model.BackgroundUpdatePrefs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@Composable
fun SettingsRoute(
    onOpenPrivilegeSetup: () -> Unit,
    onOpenDebloat: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val title = stringResource(R.string.feature_settings_title)
    val description = stringResource(R.string.feature_settings_description)
    LaunchedEffect(title, description) {
        viewModel.onScreenShown(title, description)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Settings switch already written; OS may still deny notifications. */ }

    SettingsScreen(
        state = state,
        onOpenPrivilegeSetup = onOpenPrivilegeSetup,
        onOpenDebloat = onOpenDebloat,
        onBackgroundEnabledChange = { enabled ->
            viewModel.setBackgroundUpdatesEnabled(enabled)
            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) {
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        },
        onUnmeteredOnlyChange = viewModel::setUnmeteredOnly,
        onAutoInstallChange = viewModel::setAutoInstallMatching,
        onCooldownDaysChange = viewModel::setUpdateCooldownDays,
        onCheckNow = viewModel::checkNow,
        onConsumeCheckNow = viewModel::consumeCheckNowQueued,
        modifier = modifier,
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onOpenPrivilegeSetup: () -> Unit,
    onOpenDebloat: () -> Unit,
    onBackgroundEnabledChange: (Boolean) -> Unit,
    onUnmeteredOnlyChange: (Boolean) -> Unit,
    onAutoInstallChange: (Boolean) -> Unit,
    onCooldownDaysChange: (Int) -> Unit,
    onCheckNow: () -> Unit,
    onConsumeCheckNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = state.title.ifBlank { stringResource(R.string.feature_settings_title) }
    val description = state.description.ifBlank {
        stringResource(R.string.feature_settings_description)
    }
    KilnScaffold(title = title, modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = description, style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = onOpenPrivilegeSetup,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_privilege_setup))
            }
            OutlinedButton(
                onClick = onOpenDebloat,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_debloat))
            }
            HorizontalDivider()
            Text(
                text = stringResource(R.string.settings_background_updates_section),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.settings_background_updates_explain),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BackgroundUpdateToggles(
                prefs = state.backgroundUpdates,
                onEnabledChange = onBackgroundEnabledChange,
                onUnmeteredOnlyChange = onUnmeteredOnlyChange,
                onAutoInstallChange = onAutoInstallChange,
                onCooldownDaysChange = onCooldownDaysChange,
            )
            OutlinedButton(
                onClick = onCheckNow,
                modifier = Modifier.fillMaxWidth(),
                enabled = true,
            ) {
                Text(stringResource(R.string.settings_check_now))
            }
            if (state.checkNowQueued) {
                Text(
                    text = stringResource(R.string.settings_check_now_queued),
                    style = MaterialTheme.typography.bodySmall,
                )
                LaunchedEffect(state.checkNowQueued) {
                    delay(2_000)
                    onConsumeCheckNow()
                }
            }
        }
    }
}

@Composable
private fun BackgroundUpdateToggles(
    prefs: BackgroundUpdatePrefs,
    onEnabledChange: (Boolean) -> Unit,
    onUnmeteredOnlyChange: (Boolean) -> Unit,
    onAutoInstallChange: (Boolean) -> Unit,
    onCooldownDaysChange: (Int) -> Unit,
) {
    SettingsSwitchRow(
        title = stringResource(R.string.settings_background_enabled),
        checked = prefs.enabled,
        onCheckedChange = onEnabledChange,
    )
    SettingsSwitchRow(
        title = stringResource(R.string.settings_unmetered_only),
        checked = prefs.unmeteredOnly,
        onCheckedChange = onUnmeteredOnlyChange,
        enabled = prefs.enabled,
    )
    SettingsSwitchRow(
        title = stringResource(R.string.settings_auto_install_matching),
        checked = prefs.autoInstallMatching,
        onCheckedChange = onAutoInstallChange,
        enabled = prefs.enabled,
    )
    Text(
        text = stringResource(R.string.settings_auto_install_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = stringResource(R.string.settings_update_cooldown),
        style = MaterialTheme.typography.bodyLarge,
    )
    Text(
        text = stringResource(R.string.settings_update_cooldown_value, prefs.updateCooldownDays),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Slider(
        value = prefs.updateCooldownDays.toFloat(),
        onValueChange = { onCooldownDaysChange(it.roundToInt()) },
        valueRange = 0f..14f,
        steps = 13,
        enabled = prefs.enabled && prefs.autoInstallMatching,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        text = stringResource(R.string.settings_update_cooldown_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SettingsSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}
