package dev.involvex.kiln.feature.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

@Composable
fun SettingsToolsRoute(
    onBack: () -> Unit,
    onOpenPrivilegeSetup: () -> Unit,
    onOpenDebloat: () -> Unit,
    onOpenPermissionAudit: () -> Unit,
    onOpenBackgroundControls: () -> Unit,
    onOpenFreeze: () -> Unit,
    onOpenProcesses: () -> Unit,
    onOpenUsers: () -> Unit,
    onOpenTermux: () -> Unit,
    onOpenActivityLog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsCategoryScaffold(
        title = stringResource(R.string.settings_hub_tools),
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        SettingsScrollColumn(padding = padding) {
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
                onClick = onOpenBackgroundControls,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_background_controls))
            }
            OutlinedButton(
                onClick = onOpenFreeze,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_freeze))
            }
            OutlinedButton(
                onClick = onOpenProcesses,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_processes))
            }
            OutlinedButton(
                onClick = onOpenUsers,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_users))
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
        }
    }
}
