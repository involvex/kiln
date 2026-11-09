package dev.involvex.kiln.feature.settings.activity

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.designsystem.component.KilnScaffold
import dev.involvex.kiln.core.model.ActivityLogEvent
import dev.involvex.kiln.core.model.ActivityLogType
import dev.involvex.kiln.feature.settings.R
import java.text.DateFormat
import java.util.Date

@Composable
fun ActivityLogRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: ActivityLogViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ActivityLogScreen(
        state = state,
        onBack = onBack,
        onQueryChange = viewModel::setQuery,
        onTypeFilter = viewModel::setTypeFilter,
        onRequestClear = viewModel::requestClear,
        onDismissClear = viewModel::dismissClear,
        onConfirmClear = viewModel::confirmClear,
        modifier = modifier,
    )
}

@Composable
fun ActivityLogScreen(
    state: ActivityLogUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onTypeFilter: (ActivityLogType?) -> Unit,
    onRequestClear: () -> Unit,
    onDismissClear: () -> Unit,
    onConfirmClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.confirmClear) {
        AlertDialog(
            onDismissRequest = onDismissClear,
            title = { Text(stringResource(R.string.settings_activity_log_clear_title)) },
            text = { Text(stringResource(R.string.settings_activity_log_clear_body)) },
            confirmButton = {
                TextButton(onClick = onConfirmClear, enabled = !state.clearing) {
                    Text(stringResource(R.string.settings_activity_log_clear_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissClear) {
                    Text(stringResource(R.string.settings_activity_log_clear_cancel))
                }
            },
        )
    }

    KilnScaffold(
        title = stringResource(R.string.settings_activity_log_title),
        modifier = modifier,
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.settings_activity_log_back),
                )
            }
        },
        actions = {
            TextButton(onClick = onRequestClear, enabled = state.events.isNotEmpty() && !state.clearing) {
                Text(stringResource(R.string.settings_activity_log_clear))
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                singleLine = true,
                label = { Text(stringResource(R.string.settings_activity_log_search)) },
            )
            TypeFilterRow(
                selected = state.typeFilter,
                onSelect = onTypeFilter,
            )
            if (state.events.isEmpty()) {
                Text(
                    text = stringResource(R.string.settings_activity_log_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.events, key = { it.id }) { event ->
                        ActivityLogRow(event = event)
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeFilterRow(selected: ActivityLogType?, onSelect: (ActivityLogType?) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.settings_activity_log_filter_all)) },
        )
        ActivityLogType.entries.forEach { type ->
            FilterChip(
                selected = selected == type,
                onClick = { onSelect(if (selected == type) null else type) },
                label = { Text(typeChipLabel(type)) },
            )
        }
    }
}

@Composable
private fun ActivityLogRow(event: ActivityLogEvent) {
    val time = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        .format(Date(event.epochMs))
    val status = if (event.success) {
        stringResource(R.string.settings_activity_log_ok)
    } else {
        stringResource(R.string.settings_activity_log_failed)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = "$time · ${event.type.name} · $status",
            style = MaterialTheme.typography.labelMedium,
        )
        Text(
            text = event.action,
            style = MaterialTheme.typography.titleSmall,
        )
        event.packageName?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall)
        }
        val meta = buildString {
            event.versionName?.let { append(it) }
            event.versionCode?.let {
                if (isNotEmpty()) append(" (") else append("(")
                append(it)
                append(')')
            }
            event.detailKey?.let {
                if (isNotEmpty()) append(" · ")
                append(it)
            }
        }
        if (meta.isNotEmpty()) {
            Text(text = meta, style = MaterialTheme.typography.bodySmall)
        }
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun typeChipLabel(type: ActivityLogType): String = stringResource(
    when (type) {
        ActivityLogType.INSTALL -> R.string.settings_activity_log_type_install
        ActivityLogType.BACKUP -> R.string.settings_activity_log_type_backup
        ActivityLogType.RESTORE -> R.string.settings_activity_log_type_restore
        ActivityLogType.CACHE -> R.string.settings_activity_log_type_cache
        ActivityLogType.MAINTENANCE -> R.string.settings_activity_log_type_maintenance
        ActivityLogType.DEBLOAT -> R.string.settings_activity_log_type_debloat
        ActivityLogType.PERMISSION -> R.string.settings_activity_log_type_permission
        ActivityLogType.SHELL -> R.string.settings_activity_log_type_shell
        ActivityLogType.TERMUX -> R.string.settings_activity_log_type_termux
        ActivityLogType.AUTOMATION -> R.string.settings_activity_log_type_automation
    },
)
