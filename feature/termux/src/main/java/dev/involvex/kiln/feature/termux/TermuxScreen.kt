package dev.involvex.kiln.feature.termux

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.designsystem.component.KilnScaffold
import dev.involvex.kiln.core.model.TermuxPrefs
import dev.involvex.kiln.feature.termux.run.TermuxCommandResult
import dev.involvex.kiln.feature.termux.run.TermuxRunCommandApi

@Composable
fun TermuxRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: TermuxViewModel = hiltViewModel()) {
    val title = stringResource(R.string.feature_termux_title)
    LaunchedEffect(title) {
        viewModel.onScreenShown(title)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        viewModel.refreshEnvironment()
    }
    val context = LocalContext.current
    TermuxScreen(
        state = state,
        onBack = onBack,
        onEnabledChange = viewModel::setEnabled,
        onPackageIdChange = viewModel::setPackageIdDraft,
        onSavePackageId = viewModel::savePackageId,
        onRefresh = viewModel::refreshEnvironment,
        onDetectPackage = viewModel::detectPackageId,
        onRequestPermission = {
            val packageId = state.prefs.packageId.ifBlank { TermuxPrefs.DEFAULT_TERMUX_PACKAGE_ID }
            permissionLauncher.launch(TermuxRunCommandApi.permission(packageId))
        },
        onOpenAppInfo = {
            val packageId = state.prefs.packageId.ifBlank { TermuxPrefs.DEFAULT_TERMUX_PACKAGE_ID }
            context.openAppInfo(packageId)
        },
        onRunSmokeTest = viewModel::runEchoSmokeTest,
        modifier = modifier,
    )
}

@Composable
fun TermuxScreen(
    state: TermuxUiState,
    onBack: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onPackageIdChange: (String) -> Unit,
    onSavePackageId: () -> Unit,
    onRefresh: () -> Unit,
    onDetectPackage: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onRunSmokeTest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = state.title.ifBlank { stringResource(R.string.feature_termux_title) }
    KilnScaffold(title = title, modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.termux_back))
            }
            Text(
                text = stringResource(R.string.feature_termux_description),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(R.string.termux_note_no_pm),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.termux_enable),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                )
                Switch(
                    checked = state.prefs.enabled,
                    onCheckedChange = onEnabledChange,
                )
            }
            Text(
                text = stringResource(R.string.termux_enable_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = state.packageIdDraft,
                onValueChange = onPackageIdChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.termux_package_id)) },
                supportingText = { Text(stringResource(R.string.termux_package_id_hint)) },
            )
            OutlinedButton(
                onClick = onSavePackageId,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.termux_save_package_id))
            }
            OutlinedButton(
                onClick = onDetectPackage,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.termux_detect_package))
            }
            if (state.detectionFailed) {
                Text(
                    text = stringResource(R.string.termux_detect_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Text(
                text = stringResource(R.string.termux_setup_section),
                style = MaterialTheme.typography.titleMedium,
            )
            ChecklistLine(
                ok = state.packageInstalled,
                text = stringResource(
                    if (state.packageInstalled) {
                        R.string.termux_check_installed_ok
                    } else {
                        R.string.termux_check_installed_missing
                    },
                    state.prefs.packageId,
                ),
            )
            ChecklistLine(
                ok = state.permissionGranted,
                text = stringResource(
                    if (state.permissionGranted) {
                        R.string.termux_check_permission_ok
                    } else {
                        R.string.termux_check_permission_missing
                    },
                ),
            )
            if (!state.permissionGranted) {
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.termux_request_permission))
                }
                OutlinedButton(
                    onClick = onOpenAppInfo,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.termux_open_app_info))
                }
            }
            val packageId = state.prefs.packageId.ifBlank { TermuxPrefs.DEFAULT_TERMUX_PACKAGE_ID }
            if (packageId != TermuxPrefs.DEFAULT_TERMUX_PACKAGE_ID) {
                Text(
                    text = stringResource(R.string.termux_fork_permission_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.termux_check_allow_external),
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.termux_refresh_checks))
            }
            Button(
                onClick = onRunSmokeTest,
                enabled = state.prefs.enabled && !state.isRunning,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.termux_run_echo))
            }
            if (state.isRunning) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.termux_running))
                }
            }
            StatusBlock(state = state)
            state.lastResult?.let { ResultBlock(it) }
        }
    }
}

@Composable
private fun ChecklistLine(ok: Boolean, text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (ok) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.error
        },
    )
}

@Composable
private fun StatusBlock(state: TermuxUiState) {
    val message = when (state.statusKind) {
        TermuxStatusKind.None -> null

        TermuxStatusKind.Disabled -> stringResource(R.string.termux_status_disabled)

        TermuxStatusKind.NotInstalled -> stringResource(R.string.termux_status_not_installed)

        TermuxStatusKind.PermissionDenied -> stringResource(R.string.termux_status_permission)

        TermuxStatusKind.StartFailed -> stringResource(
            R.string.termux_status_start_failed,
            state.startFailedDetail.orEmpty(),
        )

        TermuxStatusKind.Running -> stringResource(R.string.termux_running)

        TermuxStatusKind.Result -> null
    } ?: return
    val isError = state.statusKind != TermuxStatusKind.Running
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = if (isError) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurface
        },
    )
}

@Composable
private fun ResultBlock(result: TermuxCommandResult) {
    Text(
        text = stringResource(R.string.termux_result_section),
        style = MaterialTheme.typography.titleMedium,
    )
    Text(
        text = stringResource(
            R.string.termux_result_summary,
            result.exitCode?.toString() ?: "—",
            result.err?.toString() ?: "—",
        ),
        style = MaterialTheme.typography.bodyMedium,
    )
    if (result.stdout.isNotBlank()) {
        Text(
            text = stringResource(R.string.termux_result_stdout, result.stdout.trim()),
            style = MaterialTheme.typography.bodySmall,
        )
    }
    if (result.stderr.isNotBlank()) {
        Text(
            text = stringResource(R.string.termux_result_stderr, result.stderr.trim()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
    if (result.errmsg.isNotBlank()) {
        Text(
            text = stringResource(R.string.termux_result_errmsg, result.errmsg.trim()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

/**
 * Opens the system App Info screen for [packageId].
 *
 * Renamed Termux forks declare `com.involvex.termux_app.permission.RUN_COMMAND`
 * under their own package id, which Kiln cannot declare in its manifest, so
 * the `RequestPermission` launcher cannot grant it. The user grants it once in
 * App Info → Permissions; this button short-circuits that.
 */
private fun Context.openAppInfo(packageId: String) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageId, null)
    }
    runCatching { startActivity(intent) }
}
