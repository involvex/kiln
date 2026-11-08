package dev.involvex.kiln.feature.sources.discovery

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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.designsystem.component.KilnScaffold
import dev.involvex.kiln.feature.sources.R

@Composable
fun DiscoveryRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: DiscoveryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DiscoveryScreen(
        state = state,
        onBack = onBack,
        onQueryChange = viewModel::setQuery,
        onCategory = viewModel::setCategory,
        onRefreshCatalog = viewModel::refreshCatalog,
        onInstall = viewModel::install,
        onClearMessage = viewModel::clearMessage,
        modifier = modifier,
    )
}

@Composable
@Suppress("LongMethod")
fun DiscoveryScreen(
    state: DiscoveryUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onCategory: (String) -> Unit,
    onRefreshCatalog: () -> Unit,
    onInstall: (DiscoveryAppRow) -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KilnScaffold(title = stringResource(R.string.discovery_title), modifier = modifier) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.discovery_back))
                }
            }
            item {
                Text(
                    text = stringResource(R.string.discovery_description),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            item {
                Text(
                    text = stringResource(R.string.discovery_catalog_count, state.packageCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                Button(
                    onClick = onRefreshCatalog,
                    enabled = !state.isRefreshing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.discovery_refresh_catalog))
                }
            }
            if (state.isRefreshing) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.discovery_refreshing))
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.discovery_search)) },
                )
            }
            item {
                CategoryChips(
                    categories = state.categories,
                    selected = state.selectedCategory,
                    onSelect = onCategory,
                )
            }
            state.message?.let { msg ->
                item {
                    Text(
                        text = formatDiscoveryMessage(msg),
                        color = if (state.messageIsError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                    TextButton(onClick = onClearMessage) {
                        Text(stringResource(R.string.sources_dismiss))
                    }
                }
            }
            if (state.isSearching && state.results.isEmpty()) {
                item { CircularProgressIndicator() }
            }
            items(state.results, key = { "${it.repoId}:${it.packageName}" }) { row ->
                DiscoveryAppCard(row = row, onInstall = { onInstall(row) })
            }
            if (!state.isSearching && state.results.isEmpty() && state.packageCount == 0) {
                item {
                    Text(
                        text = stringResource(R.string.discovery_empty_catalog),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChips(categories: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selected.isEmpty(),
            onClick = { onSelect("") },
            label = { Text(stringResource(R.string.discovery_category_all)) },
        )
        categories.forEach { category ->
            FilterChip(
                selected = selected == category,
                onClick = { onSelect(category) },
                label = { Text(category) },
            )
        }
    }
}

@Composable
private fun DiscoveryAppCard(row: DiscoveryAppRow, onInstall: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = row.name, style = MaterialTheme.typography.titleMedium)
        Text(
            text = row.packageName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        row.summary?.takeIf { it.isNotBlank() }?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium)
        }
        val placeholder = stringResource(R.string.discovery_placeholder)
        Text(
            text = stringResource(
                R.string.discovery_app_meta,
                row.repoName,
                row.versionName ?: placeholder,
                row.categories.joinToString(", ").ifBlank { placeholder },
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(onClick = onInstall, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.discovery_install))
        }
    }
}

@Composable
private fun formatDiscoveryMessage(code: String): String = when {
    code.startsWith("catalog_ok:") ->
        stringResource(R.string.discovery_msg_catalog_ok, code.substringAfter(':'))

    code.startsWith("install_queued:") ->
        stringResource(R.string.discovery_msg_install_queued, code.substringAfter(':'))

    code == "no_fdroid_repos" -> stringResource(R.string.discovery_msg_no_repos)

    code == "repo_missing" -> stringResource(R.string.discovery_msg_repo_missing)

    code == "release_missing" -> stringResource(R.string.discovery_msg_release_missing)

    code == "apk_url_invalid" -> stringResource(R.string.discovery_msg_apk_url)

    code == "index_oom" -> stringResource(R.string.sources_msg_index_oom)

    else -> code
}
