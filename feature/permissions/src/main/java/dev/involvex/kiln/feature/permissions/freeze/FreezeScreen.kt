package dev.involvex.kiln.feature.permissions.freeze

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.designsystem.component.KilnScaffold
import dev.involvex.kiln.core.designsystem.kilnHiltViewModel
import dev.involvex.kiln.core.model.FreezeApp
import dev.involvex.kiln.core.model.FreezeListFilter
import dev.involvex.kiln.feature.permissions.R

@Composable
fun FreezeRoute(onBack: () -> Unit, onOpenPrivilegeSetup: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = kilnHiltViewModel<FreezeViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    FreezeScreen(
        state = state,
        onBack = onBack,
        onOpenPrivilegeSetup = onOpenPrivilegeSetup,
        onRefresh = viewModel::refresh,
        onQueryChange = viewModel::setQuery,
        onIncludeSystemChange = viewModel::setIncludeSystem,
        onFreezeFilterChange = viewModel::setFreezeFilter,
        onRequestFreeze = viewModel::requestFreeze,
        onRequestUnfreeze = viewModel::requestUnfreeze,
        onSelectPendingUser = viewModel::selectPendingUser,
        onConfirmFreeze = viewModel::confirmFreeze,
        onDismissFreezeConfirm = viewModel::dismissFreezeConfirm,
        onClearMessage = viewModel::clearMessage,
        modifier = modifier,
    )
}

@Composable
@Suppress("LongParameterList")
fun FreezeScreen(
    state: FreezeUiState,
    onBack: () -> Unit,
    onOpenPrivilegeSetup: () -> Unit,
    onRefresh: () -> Unit,
    onQueryChange: (String) -> Unit,
    onIncludeSystemChange: (Boolean) -> Unit,
    onFreezeFilterChange: (FreezeListFilter) -> Unit,
    onRequestFreeze: (String) -> Unit,
    onRequestUnfreeze: (String) -> Unit,
    onSelectPendingUser: (Int) -> Unit,
    onConfirmFreeze: () -> Unit,
    onDismissFreezeConfirm: () -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KilnScaffold(title = stringResource(R.string.freeze_title), modifier = modifier) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.permissions_back))
            }
            Text(
                stringResource(R.string.freeze_explain),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!state.canMutate) {
                Text(
                    stringResource(R.string.freeze_need_shizuku),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(onClick = onOpenPrivilegeSetup, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.freeze_open_setup))
                }
            }
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.freeze_search)) },
            )
            FilterBars(
                state = state,
                onIncludeSystemChange = onIncludeSystemChange,
                onFreezeFilterChange = onFreezeFilterChange,
                onRefresh = onRefresh,
            )
            if (state.message != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatFreezeMessage(state.message),
                        modifier = Modifier.weight(1f),
                        color = if (state.messageIsError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                    TextButton(onClick = onClearMessage) {
                        Text(stringResource(R.string.permissions_dismiss))
                    }
                }
            }
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else if (state.filteredApps.isEmpty()) {
                Text(
                    stringResource(R.string.freeze_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.filteredApps, key = { it.packageName }) { app ->
                        FreezeRow(
                            app = app,
                            canMutate = state.canMutate,
                            busy = state.mutatingPackage == app.packageName,
                            onToggle = { wantFrozen ->
                                if (wantFrozen) {
                                    onRequestFreeze(app.packageName)
                                } else {
                                    onRequestUnfreeze(app.packageName)
                                }
                            },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }

    state.pendingFreezeAction?.let { pending ->
        val label = state.apps.find { it.packageName == pending.packageName }?.label ?: pending.packageName
        AlertDialog(
            onDismissRequest = onDismissFreezeConfirm,
            title = {
                Text(
                    stringResource(
                        if (pending.freeze) R.string.freeze_confirm_title else R.string.freeze_unfreeze_confirm_title,
                    ),
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(
                            if (pending.freeze) R.string.freeze_confirm_body else R.string.freeze_unfreeze_confirm_body,
                            label,
                        ),
                    )
                    if (pending.users.size >= 2) {
                        Text(
                            stringResource(R.string.freeze_dual_user_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        pending.users.forEach { user ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                RadioButton(
                                    selected = pending.selectedUserId == user.id,
                                    onClick = { onSelectPendingUser(user.id) },
                                )
                                Text(
                                    text = stringResource(
                                        R.string.freeze_dual_user_option,
                                        user.name,
                                        user.id,
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onConfirmFreeze) {
                    Text(
                        stringResource(
                            if (pending.freeze) R.string.freeze_confirm_action else R.string.freeze_unfreeze_confirm_action,
                        ),
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissFreezeConfirm) {
                    Text(stringResource(R.string.freeze_confirm_cancel))
                }
            },
        )
    }
}

@Composable
private fun FilterBars(
    state: FreezeUiState,
    onIncludeSystemChange: (Boolean) -> Unit,
    onFreezeFilterChange: (FreezeListFilter) -> Unit,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = !state.includeSystem,
            onClick = { onIncludeSystemChange(false) },
            label = { Text(stringResource(R.string.freeze_filter_user)) },
        )
        FilterChip(
            selected = state.includeSystem,
            onClick = { onIncludeSystemChange(true) },
            label = { Text(stringResource(R.string.freeze_filter_all_apps)) },
        )
        TextButton(onClick = onRefresh) {
            Text(stringResource(R.string.freeze_refresh))
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = state.freezeFilter == FreezeListFilter.ALL,
            onClick = { onFreezeFilterChange(FreezeListFilter.ALL) },
            label = { Text(stringResource(R.string.freeze_filter_all)) },
        )
        FilterChip(
            selected = state.freezeFilter == FreezeListFilter.FROZEN,
            onClick = { onFreezeFilterChange(FreezeListFilter.FROZEN) },
            label = { Text(stringResource(R.string.freeze_filter_frozen)) },
        )
        FilterChip(
            selected = state.freezeFilter == FreezeListFilter.ACTIVE,
            onClick = { onFreezeFilterChange(FreezeListFilter.ACTIVE) },
            label = { Text(stringResource(R.string.freeze_filter_active)) },
        )
    }
}

@Composable
private fun FreezeRow(app: FreezeApp, canMutate: Boolean, busy: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PackageIcon(packageName = app.packageName)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                app.label,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                app.packageName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (app.isFrozen) {
                    stringResource(R.string.freeze_status_frozen)
                } else {
                    stringResource(R.string.freeze_status_active)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (busy) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        } else {
            Switch(
                checked = app.isFrozen,
                onCheckedChange = onToggle,
                enabled = canMutate,
            )
        }
    }
}

@Composable
private fun PackageIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(packageName) {
        runCatching {
            val drawable: Drawable = context.packageManager.getApplicationIcon(packageName)
            drawable.toBitmap().asImageBitmap()
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = modifier.size(40.dp))
    } else {
        Box(
            modifier = modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
        )
    }
}

@Composable
private fun formatFreezeMessage(code: String): String {
    val id = when (code) {
        "capability_missing:DEBLOAT" -> R.string.freeze_msg_capability
        "invalid_package" -> R.string.freeze_msg_invalid_package
        "user_service_unavailable" -> R.string.freeze_msg_service
        else -> null
    }
    return if (id != null) stringResource(id) else code
}
