package dev.involvex.kiln.feature.sources

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.designsystem.kilnHiltViewModel
import dev.involvex.kiln.core.designsystem.scroll.ScrollToTopEffect
import dev.involvex.kiln.core.model.WatchlistEntry

@Composable
fun SourcesWatchlistRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = kilnHiltViewModel<SourcesViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SourcesWatchlistScreen(
        watchlist = state.watchlist,
        message = state.message,
        onBack = onBack,
        onUnwatch = viewModel::unwatch,
        onClearMessage = viewModel::clearMessage,
        modifier = modifier,
    )
}

@Composable
fun SourcesWatchlistScreen(
    watchlist: List<WatchlistEntry>,
    message: String?,
    onBack: () -> Unit,
    onUnwatch: (String) -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    ScrollToTopEffect(listState)
    SourcesCategoryScaffold(
        title = stringResource(R.string.sources_hub_watchlist),
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (watchlist.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.sources_watchlist_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(watchlist, key = { it.packageName }) { entry ->
                    WatchlistCard(entry = entry, onUnwatch = onUnwatch)
                }
            }
            message?.let { msg ->
                item {
                    SourcesMessageBanner(message = msg, onClearMessage = onClearMessage)
                }
            }
        }
    }
}

@Composable
private fun WatchlistCard(entry: WatchlistEntry, onUnwatch: (String) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(entry.displayName, style = MaterialTheme.typography.titleSmall)
            Text(entry.packageName, style = MaterialTheme.typography.bodySmall)
            if (entry.hasUpdate && entry.availableVersionName != null) {
                Text(
                    text = stringResource(
                        R.string.sources_watchlist_update,
                        entry.availableVersionName.orEmpty(),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            entry.releaseNotes?.takeIf { it.isNotBlank() }?.let { notes ->
                Text(
                    text = ReleaseNotesText.preview(notes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedButton(onClick = { onUnwatch(entry.packageName) }) {
                Text(stringResource(R.string.sources_watchlist_unwatch))
            }
        }
    }
}
