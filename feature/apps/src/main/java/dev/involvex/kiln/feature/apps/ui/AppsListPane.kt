package dev.involvex.kiln.feature.apps.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.involvex.kiln.core.designsystem.component.EmptyState
import dev.involvex.kiln.core.model.AppListFilter
import dev.involvex.kiln.core.model.AppSortField
import dev.involvex.kiln.core.model.InstalledAppSummary
import dev.involvex.kiln.feature.apps.AppsUiState
import dev.involvex.kiln.feature.apps.R
import dev.involvex.kiln.feature.apps.UpdateBadgeUi
import dev.involvex.kiln.feature.apps.icon.PackageIconModel
import dev.involvex.kiln.feature.apps.util.formatApkSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsListPane(
    state: AppsUiState,
    onQueryChange: (String) -> Unit,
    onFilterChange: (AppListFilter) -> Unit,
    onSortFieldChange: (AppSortField) -> Unit,
    onRefresh: () -> Unit,
    onCheckUpdates: () -> Unit,
    onDismissUpdateCheckMessage: () -> Unit,
    onCancelBatch: () -> Unit,
    onOpenDetail: (String) -> Unit,
    onLongPress: (String) -> Unit,
    onToggleSelection: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var filtersExpanded by rememberSaveable { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxSize()) {
        if (state.batchExportRunning) {
            BatchProgressBar(
                current = state.batchProgressCurrent,
                total = state.batchProgressTotal,
                onCancel = onCancelBatch,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.feature_apps_search_hint)) },
            )
            IconButton(onClick = { filtersExpanded = !filtersExpanded }) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = stringResource(R.string.apps_filters_menu),
                )
            }
        }
        TextButton(
            onClick = onCheckUpdates,
            enabled = !state.isCheckingUpdates,
            modifier = Modifier.padding(horizontal = 12.dp),
        ) {
            Text(
                if (state.isCheckingUpdates) {
                    stringResource(R.string.feature_apps_checking_updates)
                } else {
                    stringResource(R.string.feature_apps_check_updates)
                },
            )
        }
        state.updateCheckMessage?.let { code ->
            Text(
                text = formatUpdateCheckMessage(code),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            TextButton(onClick = onDismissUpdateCheckMessage, modifier = Modifier.padding(horizontal = 8.dp)) {
                Text(stringResource(R.string.feature_apps_dismiss))
            }
        }
        if (filtersExpanded) {
            FilterRow(filter = state.filter, onFilterChange = onFilterChange)
            SortRow(sortField = state.sortField, onSortFieldChange = onSortFieldChange)
        }
        PullToRefreshBox(
            isRefreshing = state.isLoading || state.isCheckingUpdates,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            AppsListBody(
                state = state,
                onOpenDetail = onOpenDetail,
                onLongPress = onLongPress,
                onToggleSelection = onToggleSelection,
            )
        }
    }
}

@Composable
private fun BatchProgressBar(current: Int, total: Int, onCancel: () -> Unit) {
    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = stringResource(R.string.feature_apps_batch_running, current, total))
        TextButton(onClick = onCancel) {
            Text(stringResource(R.string.feature_apps_cancel_batch))
        }
    }
}

@Composable
private fun FilterRow(filter: AppListFilter, onFilterChange: (AppListFilter) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = filter == AppListFilter.USER,
            onClick = { onFilterChange(AppListFilter.USER) },
            label = { Text(stringResource(R.string.feature_apps_filter_user)) },
        )
        FilterChip(
            selected = filter == AppListFilter.SYSTEM,
            onClick = { onFilterChange(AppListFilter.SYSTEM) },
            label = { Text(stringResource(R.string.feature_apps_filter_system)) },
        )
        FilterChip(
            selected = filter == AppListFilter.ALL,
            onClick = { onFilterChange(AppListFilter.ALL) },
            label = { Text(stringResource(R.string.feature_apps_filter_all)) },
        )
        FilterChip(
            selected = filter == AppListFilter.UPDATES,
            onClick = { onFilterChange(AppListFilter.UPDATES) },
            label = { Text(stringResource(R.string.feature_apps_filter_updates)) },
        )
    }
}

@Composable
private fun formatUpdateCheckMessage(code: String): String = when {
    code.startsWith("updates_error:") ->
        stringResource(R.string.feature_apps_update_check_error, code.removePrefix("updates_error:"))

    code.startsWith("updates:") -> {
        val parts = code.split(':')
        // Resource uses %d — Formatter rejects String args (IllegalFormatConversionException).
        stringResource(
            R.string.feature_apps_update_check_ok,
            parts.getOrNull(1)?.toIntOrNull() ?: 0,
            parts.getOrNull(2)?.toIntOrNull() ?: 0,
            parts.getOrNull(3)?.toIntOrNull() ?: 0,
        )
    }

    else -> code
}

@Composable
private fun SortRow(sortField: AppSortField, onSortFieldChange: (AppSortField) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SortChip(AppSortField.NAME, sortField, onSortFieldChange, stringResource(R.string.feature_apps_sort_name))
        SortChip(AppSortField.SIZE, sortField, onSortFieldChange, stringResource(R.string.feature_apps_sort_size))
        SortChip(AppSortField.INSTALL_DATE, sortField, onSortFieldChange, stringResource(R.string.feature_apps_sort_install))
        SortChip(AppSortField.UPDATE_DATE, sortField, onSortFieldChange, stringResource(R.string.feature_apps_sort_update))
    }
}

@Composable
private fun SortChip(field: AppSortField, selected: AppSortField, onSelect: (AppSortField) -> Unit, label: String) {
    AssistChip(
        onClick = { onSelect(field) },
        label = { Text(label) },
        leadingIcon = if (selected == field) {
            { Text("•") }
        } else {
            null
        },
    )
}

@Composable
private fun AppsListBody(
    state: AppsUiState,
    onOpenDetail: (String) -> Unit,
    onLongPress: (String) -> Unit,
    onToggleSelection: (String) -> Unit,
) {
    when {
        state.errorMessage != null && state.visibleApps.isEmpty() -> {
            EmptyState(
                title = stringResource(R.string.feature_apps_error_title),
                description = state.errorMessage,
            )
        }

        state.visibleApps.isEmpty() && !state.isLoading -> {
            EmptyState(
                title = stringResource(R.string.feature_apps_empty_title),
                description = stringResource(R.string.feature_apps_empty_description),
            )
        }

        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                items(items = state.visibleApps, key = { it.packageName }) { app ->
                    AppListItem(
                        app = app,
                        updateBadge = state.updateBadges[app.packageName],
                        selected = app.packageName in state.selectedPackages,
                        selectionMode = state.selectionMode,
                        onClick = {
                            if (state.selectionMode) {
                                onToggleSelection(app.packageName)
                            } else {
                                onOpenDetail(app.packageName)
                            }
                        },
                        onLongClick = { onLongPress(app.packageName) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppListItem(
    app: InstalledAppSummary,
    updateBadge: UpdateBadgeUi?,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AsyncImage(
            model = PackageIconModel(app.packageName, app.lastUpdateTime),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val version = app.versionName ?: app.versionCode.toString()
            val size = formatApkSize(app.apkSizeBytes)
                ?: stringResource(R.string.feature_apps_size_unknown)
            Text(text = "$version · $size", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (app.isSystem) {
                    Text(
                        text = stringResource(R.string.feature_apps_badge_system),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (app.hasSplits) {
                    Text(
                        text = stringResource(R.string.feature_apps_badge_split),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                if (updateBadge != null) {
                    Text(
                        text = if (updateBadge.signatureWarning) {
                            stringResource(R.string.feature_apps_update_badge_warning, updateBadge.versionLabel)
                        } else {
                            stringResource(R.string.feature_apps_update_badge, updateBadge.versionLabel)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (updateBadge.signatureWarning) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.secondary
                        },
                    )
                }
            }
        }
        if (selectionMode && selected) {
            Text("✓", color = MaterialTheme.colorScheme.primary)
        }
    }
}
