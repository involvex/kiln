package dev.involvex.kiln.feature.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.designsystem.component.KilnScaffold
import dev.involvex.kiln.core.designsystem.scroll.ScrollToTopEffect
import dev.involvex.kiln.core.designsystem.theme.allowsDynamicColor
import dev.involvex.kiln.core.model.AppearancePrefs
import dev.involvex.kiln.core.model.AutomationPrefs
import dev.involvex.kiln.core.model.BackgroundUpdatePrefs
import dev.involvex.kiln.core.model.DevPrefs
import dev.involvex.kiln.core.model.KilnConfigFilenames
import dev.involvex.kiln.core.model.KilnConfigImportPreview
import dev.involvex.kiln.core.model.KilnFontStyle
import dev.involvex.kiln.core.model.KilnLanguage
import dev.involvex.kiln.core.model.KilnThemeStyle
import dev.involvex.kiln.core.model.MaintenancePrefs
import dev.involvex.kiln.core.model.MaintenanceRunRecord
import dev.involvex.kiln.core.model.NetworkProxyPrefs
import dev.involvex.kiln.core.model.NetworkProxyType
import dev.involvex.kiln.core.model.PreferredInstaller
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

@Composable
fun SettingsRoute(
    onOpenPrivilegeSetup: () -> Unit,
    onOpenDebloat: () -> Unit,
    onOpenPermissionAudit: () -> Unit,
    onOpenTermux: () -> Unit,
    onOpenActivityLog: () -> Unit,
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
    val exportConfig = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(KilnConfigFilenames.MIME_JSON),
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportConfigToUri(uri, context.contentResolver)
        }
    }
    val importConfig = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.previewImportFromUri(uri, context.contentResolver)
        }
    }

    SettingsScreen(
        state = state,
        onOpenPrivilegeSetup = onOpenPrivilegeSetup,
        onOpenDebloat = onOpenDebloat,
        onOpenPermissionAudit = onOpenPermissionAudit,
        onOpenTermux = onOpenTermux,
        onOpenActivityLog = onOpenActivityLog,
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
        onProxyTypeChange = viewModel::setProxyType,
        onProxyHostChange = viewModel::setProxyHost,
        onProxyPortChange = viewModel::setProxyPort,
        onProxyUsernameChange = viewModel::setProxyUsername,
        onProxyPasswordChange = viewModel::setProxyPassword,
        onOrbotPresetChange = viewModel::setOrbotPreset,
        onAutomationEnabledChange = viewModel::setAutomationEnabled,
        onMaintenanceEnabledChange = viewModel::setMaintenanceEnabled,
        onMaintenanceTrimChange = viewModel::setMaintenanceTrimCaches,
        onMaintenanceDownloadsChange = viewModel::setMaintenancePruneDownloads,
        onMaintenanceRollbackChange = viewModel::setMaintenancePruneRollback,
        onMaintenanceBackupTempChange = viewModel::setMaintenancePruneBackupTemp,
        onRunMaintenanceNow = viewModel::runMaintenanceNow,
        onConsumeMaintenanceRunQueued = viewModel::consumeMaintenanceRunQueued,
        onExportConfig = { exportConfig.launch(viewModel.suggestedConfigFileName()) },
        onImportConfig = {
            importConfig.launch(arrayOf(KilnConfigFilenames.MIME_JSON, "application/octet-stream", "text/*", "*/*"))
        },
        onConfirmImport = viewModel::confirmPendingImport,
        onDismissImport = viewModel::dismissImportPreview,
        onConsumeConfigMessage = viewModel::consumeConfigMessage,
        onThemeChange = viewModel::setTheme,
        onLanguageChange = viewModel::setLanguage,
        onFontStyleChange = viewModel::setFontStyle,
        onDynamicColorChange = viewModel::setDynamicColor,
        onPreferredInstallerChange = viewModel::setPreferredInstaller,
        onDebugLogChange = viewModel::setDebugLogEnabled,
        onCheckNow = viewModel::checkNow,
        onConsumeCheckNow = viewModel::consumeCheckNowQueued,
        modifier = modifier,
    )
}

@Composable
@Suppress("LongMethod", "LongParameterList")
fun SettingsScreen(
    state: SettingsUiState,
    onOpenPrivilegeSetup: () -> Unit,
    onOpenDebloat: () -> Unit,
    onOpenPermissionAudit: () -> Unit,
    onOpenTermux: () -> Unit,
    onOpenActivityLog: () -> Unit,
    onBackgroundEnabledChange: (Boolean) -> Unit,
    onUnmeteredOnlyChange: (Boolean) -> Unit,
    onAutoInstallChange: (Boolean) -> Unit,
    onCooldownDaysChange: (Int) -> Unit,
    onProxyTypeChange: (NetworkProxyType) -> Unit,
    onProxyHostChange: (String) -> Unit,
    onProxyPortChange: (Int) -> Unit,
    onProxyUsernameChange: (String) -> Unit,
    onProxyPasswordChange: (String) -> Unit,
    onOrbotPresetChange: (Boolean) -> Unit,
    onAutomationEnabledChange: (Boolean) -> Unit,
    onMaintenanceEnabledChange: (Boolean) -> Unit,
    onMaintenanceTrimChange: (Boolean) -> Unit,
    onMaintenanceDownloadsChange: (Boolean) -> Unit,
    onMaintenanceRollbackChange: (Boolean) -> Unit,
    onMaintenanceBackupTempChange: (Boolean) -> Unit,
    onRunMaintenanceNow: () -> Unit,
    onConsumeMaintenanceRunQueued: () -> Unit,
    onExportConfig: () -> Unit,
    onImportConfig: () -> Unit,
    onConfirmImport: () -> Unit,
    onDismissImport: () -> Unit,
    onConsumeConfigMessage: () -> Unit,
    onThemeChange: (KilnThemeStyle) -> Unit,
    onLanguageChange: (KilnLanguage) -> Unit,
    onFontStyleChange: (KilnFontStyle) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onPreferredInstallerChange: (PreferredInstaller) -> Unit,
    onDebugLogChange: (Boolean) -> Unit,
    onCheckNow: () -> Unit,
    onConsumeCheckNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = state.title.ifBlank { stringResource(R.string.feature_settings_title) }
    val description = state.description.ifBlank {
        stringResource(R.string.feature_settings_description)
    }
    val scrollState = rememberScrollState()
    ScrollToTopEffect(scrollState)
    KilnScaffold(title = title, modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
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
            OutlinedButton(
                onClick = onOpenPermissionAudit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_permission_audit))
            }
            OutlinedButton(
                onClick = onOpenTermux,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_termux))
            }
            OutlinedButton(
                onClick = onOpenActivityLog,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_activity_log))
            }
            HorizontalDivider()
            AppearanceSection(
                prefs = state.appearance,
                onThemeChange = onThemeChange,
                onLanguageChange = onLanguageChange,
                onFontStyleChange = onFontStyleChange,
                onDynamicColorChange = onDynamicColorChange,
            )
            HorizontalDivider()
            InstallerSection(
                preferred = state.preferredInstaller,
                shizukuSilentAvailable = state.shizukuSilentAvailable,
                onPreferredChange = onPreferredInstallerChange,
            )
            HorizontalDivider()
            NetworkProxySection(
                prefs = state.networkProxy,
                onTypeChange = onProxyTypeChange,
                onHostChange = onProxyHostChange,
                onPortChange = onProxyPortChange,
                onUsernameChange = onProxyUsernameChange,
                onPasswordChange = onProxyPasswordChange,
                onOrbotChange = onOrbotPresetChange,
            )
            HorizontalDivider()
            DevSection(
                prefs = state.devPrefs,
                onDebugLogChange = onDebugLogChange,
            )
            HorizontalDivider()
            BackgroundUpdatesBlock(
                prefs = state.backgroundUpdates,
                checkNowQueued = state.checkNowQueued,
                onEnabledChange = onBackgroundEnabledChange,
                onUnmeteredOnlyChange = onUnmeteredOnlyChange,
                onAutoInstallChange = onAutoInstallChange,
                onCooldownDaysChange = onCooldownDaysChange,
                onCheckNow = onCheckNow,
                onConsumeCheckNow = onConsumeCheckNow,
            )
            HorizontalDivider()
            AutomationSection(
                prefs = state.automation,
                onEnabledChange = onAutomationEnabledChange,
            )
            HorizontalDivider()
            MaintenanceSection(
                prefs = state.maintenance,
                lastRun = state.lastMaintenanceRun,
                runQueued = state.maintenanceRunQueued,
                onEnabledChange = onMaintenanceEnabledChange,
                onTrimChange = onMaintenanceTrimChange,
                onDownloadsChange = onMaintenanceDownloadsChange,
                onRollbackChange = onMaintenanceRollbackChange,
                onBackupTempChange = onMaintenanceBackupTempChange,
                onRunNow = onRunMaintenanceNow,
                onConsumeRunQueued = onConsumeMaintenanceRunQueued,
            )
            HorizontalDivider()
            KilnConfigSection(
                busy = state.configBusy,
                messageKey = state.configMessage,
                onExport = onExportConfig,
                onImport = onImportConfig,
                onConsumeMessage = onConsumeConfigMessage,
            )
            HorizontalDivider()
            AboutSection(
                versionName = state.appVersionName,
                versionCode = state.appVersionCode,
            )
        }
    }
    state.pendingImportPreview?.let { preview ->
        ImportConfirmDialog(
            preview = preview,
            onConfirm = onConfirmImport,
            onDismiss = onDismissImport,
        )
    }
}

@Composable
private fun AboutSection(versionName: String, versionCode: Long) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.settings_about_section),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(
                R.string.settings_about_version,
                versionName.ifBlank { "—" },
                versionCode,
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = stringResource(R.string.settings_about_license),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val sourceLabel = stringResource(R.string.settings_about_source)
        OutlinedButton(
            onClick = { openHttps(context, URL_SOURCE) },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = sourceLabel },
        ) {
            Text(sourceLabel)
        }
        val privacyLabel = stringResource(R.string.settings_about_privacy)
        OutlinedButton(
            onClick = { openHttps(context, URL_PRIVACY) },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = privacyLabel },
        ) {
            Text(privacyLabel)
        }
        val supportLabel = stringResource(R.string.settings_about_support)
        OutlinedButton(
            onClick = { openHttps(context, URL_SUPPORTING) },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = supportLabel },
        ) {
            Text(supportLabel)
        }
        val licenseLabel = stringResource(R.string.settings_about_license_link)
        OutlinedButton(
            onClick = { openHttps(context, URL_LICENSE) },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = licenseLabel },
        ) {
            Text(licenseLabel)
        }
    }
}

private fun openHttps(context: android.content.Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    runCatching { context.startActivity(intent) }
}

private const val URL_SOURCE: String = "https://github.com/involvex/kiln"
private const val URL_PRIVACY: String =
    "https://github.com/involvex/kiln/blob/main/docs/PRIVACY.md"
private const val URL_SUPPORTING: String =
    "https://github.com/involvex/kiln/blob/main/docs/SUPPORTING.md"
private const val URL_LICENSE: String =
    "https://github.com/involvex/kiln/blob/main/LICENSE"

@Composable
private fun KilnConfigSection(
    busy: Boolean,
    messageKey: String?,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onConsumeMessage: () -> Unit,
) {
    Text(
        text = stringResource(R.string.settings_config_section),
        style = MaterialTheme.typography.titleMedium,
    )
    Text(
        text = stringResource(R.string.settings_config_explain),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedButton(
        onClick = onExport,
        enabled = !busy,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.settings_config_export))
    }
    OutlinedButton(
        onClick = onImport,
        enabled = !busy,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.settings_config_import))
    }
    if (busy) {
        Text(
            text = stringResource(R.string.settings_config_busy),
            style = MaterialTheme.typography.bodySmall,
        )
    }
    messageKey?.let { key ->
        Text(
            text = configMessageText(key),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LaunchedEffect(key) {
            delay(4_000)
            onConsumeMessage()
        }
    }
}

@Composable
private fun configMessageText(key: String): String = when {
    key == "config_export_ok" -> stringResource(R.string.settings_config_export_ok)

    key == "config_export_write_failed" -> stringResource(R.string.settings_config_export_failed)

    key == "config_import_read_failed" -> stringResource(R.string.settings_config_import_read_failed)

    key.startsWith("config_export_failed:") -> stringResource(R.string.settings_config_export_failed)

    key.startsWith("config_import_failed:") -> stringResource(R.string.settings_config_import_failed)

    key.startsWith("config_import_ok:") -> {
        val parts = key.removePrefix("config_import_ok:").split(":")
        stringResource(
            R.string.settings_config_import_ok,
            parts.getOrNull(0) ?: "0",
            parts.getOrNull(1) ?: "0",
            parts.getOrNull(2) ?: "0",
        )
    }

    else -> key
}

@Composable
private fun ImportConfirmDialog(preview: KilnConfigImportPreview, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val prefsLine = if (preview.prefsSections.isEmpty()) {
        stringResource(R.string.settings_config_import_prefs_none)
    } else {
        stringResource(
            R.string.settings_config_import_prefs,
            preview.prefsSections.joinToString(", "),
        )
    }
    val patHint = if (preview.githubPatWasConfigured) {
        stringResource(R.string.settings_config_import_pat_hint)
    } else {
        ""
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_config_import_confirm_title)) },
        text = {
            Text(
                stringResource(
                    R.string.settings_config_import_confirm_body,
                    preview.reposNew,
                    preview.reposUpdated,
                    preview.managedAppsUpserted,
                    prefsLine,
                    patHint,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.settings_config_import_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_config_import_cancel))
            }
        },
    )
}

@Composable
private fun AutomationSection(prefs: AutomationPrefs, onEnabledChange: (Boolean) -> Unit) {
    Text(
        text = stringResource(R.string.settings_automation_section),
        style = MaterialTheme.typography.titleMedium,
    )
    Text(
        text = stringResource(R.string.settings_automation_explain),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    SettingsSwitchRow(
        title = stringResource(R.string.settings_automation_enabled),
        checked = prefs.enabled,
        onCheckedChange = onEnabledChange,
    )
}

@Composable
private fun MaintenanceSection(
    prefs: MaintenancePrefs,
    lastRun: MaintenanceRunRecord?,
    runQueued: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onTrimChange: (Boolean) -> Unit,
    onDownloadsChange: (Boolean) -> Unit,
    onRollbackChange: (Boolean) -> Unit,
    onBackupTempChange: (Boolean) -> Unit,
    onRunNow: () -> Unit,
    onConsumeRunQueued: () -> Unit,
) {
    Text(
        text = stringResource(R.string.settings_maintenance_section),
        style = MaterialTheme.typography.titleMedium,
    )
    Text(
        text = stringResource(R.string.settings_maintenance_explain),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    SettingsSwitchRow(
        title = stringResource(R.string.settings_maintenance_enabled),
        checked = prefs.enabled,
        onCheckedChange = onEnabledChange,
    )
    if (prefs.enabled) {
        SettingsSwitchRow(
            title = stringResource(R.string.settings_maintenance_trim),
            checked = prefs.trimCaches,
            onCheckedChange = onTrimChange,
        )
        SettingsSwitchRow(
            title = stringResource(R.string.settings_maintenance_downloads),
            checked = prefs.pruneDownloads,
            onCheckedChange = onDownloadsChange,
        )
        SettingsSwitchRow(
            title = stringResource(R.string.settings_maintenance_rollback),
            checked = prefs.pruneRollback,
            onCheckedChange = onRollbackChange,
        )
        SettingsSwitchRow(
            title = stringResource(R.string.settings_maintenance_backup_temp),
            checked = prefs.pruneBackupTemp,
            onCheckedChange = onBackupTempChange,
        )
    }
    OutlinedButton(
        onClick = onRunNow,
        enabled = prefs.enabled,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.settings_maintenance_run_now))
    }
    if (runQueued) {
        Text(
            text = stringResource(R.string.settings_maintenance_run_queued),
            style = MaterialTheme.typography.bodySmall,
        )
        LaunchedEffect(runQueued) {
            delay(2_500)
            onConsumeRunQueued()
        }
    }
    Text(
        text = lastRunSummary(lastRun),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun lastRunSummary(lastRun: MaintenanceRunRecord?): String {
    if (lastRun == null) {
        return stringResource(R.string.settings_maintenance_last_run_none)
    }
    val whenText = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        .format(Date(lastRun.finishedAtEpochMs))
    val status = if (lastRun.ok) {
        stringResource(R.string.settings_maintenance_last_run_ok)
    } else {
        stringResource(R.string.settings_maintenance_last_run_partial)
    }
    val lines = lastRun.tasks.joinToString("; ") { "${it.taskId}=${it.status}" }
    return stringResource(R.string.settings_maintenance_last_run, whenText, status, lines)
}

@Composable
private fun BackgroundUpdatesBlock(
    prefs: BackgroundUpdatePrefs,
    checkNowQueued: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onUnmeteredOnlyChange: (Boolean) -> Unit,
    onAutoInstallChange: (Boolean) -> Unit,
    onCooldownDaysChange: (Int) -> Unit,
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
    BackgroundUpdateToggles(
        prefs = prefs,
        onEnabledChange = onEnabledChange,
        onUnmeteredOnlyChange = onUnmeteredOnlyChange,
        onAutoInstallChange = onAutoInstallChange,
        onCooldownDaysChange = onCooldownDaysChange,
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
private fun NetworkProxySection(
    prefs: NetworkProxyPrefs,
    onTypeChange: (NetworkProxyType) -> Unit,
    onHostChange: (String) -> Unit,
    onPortChange: (Int) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onOrbotChange: (Boolean) -> Unit,
) {
    Text(
        text = stringResource(R.string.settings_network_section),
        style = MaterialTheme.typography.titleMedium,
    )
    Text(
        text = stringResource(R.string.settings_network_explain),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    SettingsSwitchRow(
        title = stringResource(R.string.settings_network_orbot),
        checked = prefs.orbotPreset,
        onCheckedChange = onOrbotChange,
    )
    if (!prefs.orbotPreset) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = prefs.type == NetworkProxyType.NONE,
                onClick = { onTypeChange(NetworkProxyType.NONE) },
                label = { Text(stringResource(R.string.settings_network_type_none)) },
            )
            FilterChip(
                selected = prefs.type == NetworkProxyType.HTTP,
                onClick = { onTypeChange(NetworkProxyType.HTTP) },
                label = { Text(stringResource(R.string.settings_network_type_http)) },
            )
            FilterChip(
                selected = prefs.type == NetworkProxyType.SOCKS,
                onClick = { onTypeChange(NetworkProxyType.SOCKS) },
                label = { Text(stringResource(R.string.settings_network_type_socks)) },
            )
        }
        if (prefs.type != NetworkProxyType.NONE) {
            OutlinedTextField(
                value = prefs.host,
                onValueChange = onHostChange,
                label = { Text(stringResource(R.string.settings_network_host)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = if (prefs.port == 0) "" else prefs.port.toString(),
                onValueChange = { raw -> onPortChange(raw.filter { it.isDigit() }.toIntOrNull() ?: 0) },
                label = { Text(stringResource(R.string.settings_network_port)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = prefs.username,
                onValueChange = onUsernameChange,
                label = { Text(stringResource(R.string.settings_network_username)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = prefs.password,
                onValueChange = onPasswordChange,
                label = { Text(stringResource(R.string.settings_network_password)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
            )
        }
    } else {
        Text(
            text = stringResource(R.string.settings_network_orbot_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun InstallerSection(
    preferred: PreferredInstaller,
    shizukuSilentAvailable: Boolean,
    onPreferredChange: (PreferredInstaller) -> Unit,
) {
    Text(
        text = stringResource(R.string.settings_installer_section),
        style = MaterialTheme.typography.titleMedium,
    )
    Text(
        text = stringResource(R.string.settings_installer_explain),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = preferred == PreferredInstaller.SYSTEM,
            onClick = { onPreferredChange(PreferredInstaller.SYSTEM) },
            label = { Text(stringResource(R.string.settings_installer_system)) },
        )
        FilterChip(
            selected = preferred == PreferredInstaller.SHIZUKU,
            onClick = { onPreferredChange(PreferredInstaller.SHIZUKU) },
            label = { Text(stringResource(R.string.settings_installer_shizuku)) },
        )
    }
    if (preferred == PreferredInstaller.SHIZUKU && !shizukuSilentAvailable) {
        Text(
            text = stringResource(R.string.settings_installer_shizuku_unavailable),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun DevSection(prefs: DevPrefs, onDebugLogChange: (Boolean) -> Unit) {
    Text(
        text = stringResource(R.string.settings_dev_section),
        style = MaterialTheme.typography.titleMedium,
    )
    SettingsSwitchRow(
        title = stringResource(R.string.settings_dev_debug_log),
        checked = prefs.debugLogEnabled,
        onCheckedChange = onDebugLogChange,
    )
    Text(
        text = stringResource(R.string.settings_dev_debug_log_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AppearanceSection(
    prefs: AppearancePrefs,
    onThemeChange: (KilnThemeStyle) -> Unit,
    onLanguageChange: (KilnLanguage) -> Unit,
    onFontStyleChange: (KilnFontStyle) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
) {
    Text(
        text = stringResource(R.string.settings_appearance_section),
        style = MaterialTheme.typography.titleMedium,
    )
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
    // Always interactive: configure network constraint before enabling checks.
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
