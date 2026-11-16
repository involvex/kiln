package dev.involvex.kiln.feature.settings

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.model.BackgroundUpdatePrefs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@Composable
fun SettingsUpdatesRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    SettingsCategoryScaffold(
        title = stringResource(R.string.settings_hub_updates),
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        SettingsScrollColumn(padding = padding) {
            BackgroundUpdatesBlock(
                prefs = state.backgroundUpdates,
                checkNowQueued = state.checkNowQueued,
                onEnabledChange = { enabled ->
                    onBackgroundEnabled(
                        enabled = enabled,
                        context = context,
                        launcher = notificationPermission,
                        setEnabled = viewModel::setBackgroundUpdatesEnabled,
                    )
                },
                onUnmeteredOnlyChange = viewModel::setUnmeteredOnly,
                onAutoInstallChange = viewModel::setAutoInstallMatching,
                onCooldownDaysChange = viewModel::setUpdateCooldownDays,
                onIntervalChange = viewModel::setCheckIntervalHours,
                onCheckNow = viewModel::checkNow,
                onConsumeCheckNow = viewModel::consumeCheckNowQueued,
            )
            GithubTokenSection(
                tokenSet = state.githubTokenSet,
                tokenInput = state.githubTokenInput,
                messageKey = state.configMessage,
                onTokenChange = viewModel::onGithubTokenInputChange,
                onSave = viewModel::saveGithubToken,
                onClear = viewModel::clearGithubToken,
                onConsumeMessage = viewModel::consumeConfigMessage,
            )
        }
    }
}

private fun onBackgroundEnabled(
    enabled: Boolean,
    context: Context,
    launcher: ManagedActivityResultLauncher<String, Boolean>,
    setEnabled: (Boolean) -> Unit,
) {
    setEnabled(enabled)
    if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
    if (!granted) {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

@Composable
private fun GithubTokenSection(
    tokenSet: Boolean,
    tokenInput: String,
    messageKey: String?,
    onTokenChange: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    onConsumeMessage: () -> Unit,
) {
    Text(
        text = stringResource(R.string.settings_github_pat_section),
        style = MaterialTheme.typography.titleMedium,
    )
    Text(
        text = stringResource(R.string.settings_github_pat_explain),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = if (tokenSet) {
            stringResource(R.string.settings_github_pat_set)
        } else {
            stringResource(R.string.settings_github_pat_unset)
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedTextField(
        value = tokenInput,
        onValueChange = onTokenChange,
        label = { Text(stringResource(R.string.settings_github_pat_label)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
    )
    Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.settings_github_pat_save))
    }
    TextButton(onClick = onClear) {
        Text(stringResource(R.string.settings_github_pat_clear))
    }
    if (messageKey == "github_token_saved" || messageKey == "github_token_cleared") {
        Text(
            text = when (messageKey) {
                "github_token_saved" -> stringResource(R.string.settings_github_pat_saved)
                else -> stringResource(R.string.settings_github_pat_cleared)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LaunchedEffect(messageKey) {
            delay(3_000)
            onConsumeMessage()
        }
    }
}

@Composable
private fun BackgroundUpdatesBlock(
    prefs: BackgroundUpdatePrefs,
    checkNowQueued: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onUnmeteredOnlyChange: (Boolean) -> Unit,
    onAutoInstallChange: (Boolean) -> Unit,
    onCooldownDaysChange: (Int) -> Unit,
    onIntervalChange: (Int) -> Unit,
    onCheckNow: () -> Unit,
    onConsumeCheckNow: () -> Unit,
) {
    Text(
        text = stringResource(R.string.settings_background_updates_section),
        style = MaterialTheme.typography.titleMedium,
    )
    Text(
        text = stringResource(R.string.settings_background_updates_explain),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    SettingsSwitchRow(
        title = stringResource(R.string.settings_background_enabled),
        checked = prefs.enabled,
        onCheckedChange = onEnabledChange,
    )
    Text(
        text = stringResource(R.string.settings_check_interval),
        style = MaterialTheme.typography.bodyLarge,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IntervalChip(12, prefs.checkIntervalHours, onIntervalChange, stringResource(R.string.settings_check_interval_12h))
        IntervalChip(24, prefs.checkIntervalHours, onIntervalChange, stringResource(R.string.settings_check_interval_1d))
        IntervalChip(168, prefs.checkIntervalHours, onIntervalChange, stringResource(R.string.settings_check_interval_7d))
    }
    SettingsSwitchRow(
        title = stringResource(R.string.settings_unmetered_only),
        checked = prefs.unmeteredOnly,
        onCheckedChange = onUnmeteredOnlyChange,
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
    OutlinedButton(onClick = onCheckNow, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.settings_check_now))
    }
    if (checkNowQueued) {
        Text(
            text = stringResource(R.string.settings_check_now_queued),
            style = MaterialTheme.typography.bodySmall,
        )
        LaunchedEffect(checkNowQueued) {
            delay(2_000)
            onConsumeCheckNow()
        }
    }
}

@Composable
private fun IntervalChip(hours: Int, selected: Int, onChange: (Int) -> Unit, label: String) {
    FilterChip(
        selected = selected == hours,
        onClick = { onChange(hours) },
        label = { Text(label) },
    )
}
