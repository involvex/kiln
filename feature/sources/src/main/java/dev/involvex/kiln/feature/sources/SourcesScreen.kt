package dev.involvex.kiln.feature.sources

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.designsystem.component.KilnScaffold
import dev.involvex.kiln.core.designsystem.kilnHiltViewModel
import dev.involvex.kiln.core.designsystem.scroll.ScrollToTopEffect

@Composable
fun SourcesRoute(
    onOpenRepos: () -> Unit,
    onOpenWatchlist: () -> Unit,
    onOpenStars: () -> Unit,
    onOpenImport: () -> Unit,
    onOpenAutoMatch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = kilnHiltViewModel<SourcesViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.refreshGithubTokenStatus()
    }
    SourcesHubScreen(
        state = state,
        destinations = sourcesHubDestinations(
            onOpenRepos = onOpenRepos,
            onOpenWatchlist = onOpenWatchlist,
            onOpenStars = onOpenStars,
            onOpenImport = onOpenImport,
            onOpenAutoMatch = onOpenAutoMatch,
        ),
        onSyncAll = viewModel::syncAll,
        onCancelSyncAll = viewModel::cancelSyncAll,
        onClearMessage = viewModel::clearMessage,
        modifier = modifier,
    )
}

@Composable
fun SourcesHubScreen(
    state: SourcesUiState,
    destinations: List<SourcesHubDestination>,
    onSyncAll: () -> Unit,
    onCancelSyncAll: () -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    ScrollToTopEffect(scrollState)
    KilnScaffold(title = stringResource(R.string.feature_sources_title), modifier = modifier) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.feature_sources_description),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Button(onClick = onSyncAll, enabled = !state.isSyncing, modifier = Modifier.fillMaxWidth()) {
                if (state.isSyncing) {
                    CircularProgressIndicator()
                } else {
                    Text(stringResource(R.string.sources_sync_all))
                }
            }
            if (state.isSyncing) {
                Text(
                    text = formatSyncProgress(state),
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = onCancelSyncAll) {
                    Text(stringResource(R.string.sources_cancel_sync))
                }
            }
            state.message?.let { msg ->
                SourcesMessageBanner(message = msg, onClearMessage = onClearMessage)
            }
            destinations.forEachIndexed { index, entry ->
                if (index > 0) HorizontalDivider()
                SourcesHubRow(
                    title = stringResource(entry.titleRes),
                    subtitle = stringResource(entry.subtitleRes),
                    onClick = entry.onClick,
                )
            }
        }
    }
}
