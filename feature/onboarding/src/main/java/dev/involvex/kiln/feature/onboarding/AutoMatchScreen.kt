package dev.involvex.kiln.feature.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.designsystem.component.KilnScaffold
import dev.involvex.kiln.core.model.MatchConfidence
import dev.involvex.kiln.feature.onboarding.match.MatchCandidate

@Composable
fun AutoMatchRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: AutoMatchViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AutoMatchScreen(
        state = state,
        onBack = onBack,
        onToggle = viewModel::toggle,
        onAcceptSelected = viewModel::acceptSelected,
        onAcceptAllSafe = viewModel::acceptAllSafe,
        onRefresh = viewModel::refresh,
        modifier = modifier,
    )
}

@Composable
fun AutoMatchScreen(
    state: AutoMatchUiState,
    onBack: () -> Unit,
    onToggle: (String) -> Unit,
    onAcceptSelected: () -> Unit,
    onAcceptAllSafe: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KilnScaffold(
        title = stringResource(R.string.onboarding_auto_match_title),
        modifier = modifier,
    ) { padding ->
        if (state.isLoading) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
            }
            return@KilnScaffold
        }
        val titleHigh = stringResource(R.string.onboarding_group_high)
        val titleMedium = stringResource(R.string.onboarding_group_medium)
        val titleWarning = stringResource(R.string.onboarding_group_warning)
        val titleNone = stringResource(R.string.onboarding_group_none)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Button(onClick = onBack) { Text(stringResource(R.string.onboarding_back)) }
            }
            item {
                Text(stringResource(R.string.onboarding_auto_match_body), style = MaterialTheme.typography.bodyMedium)
            }
            if (state.indexEmpty) {
                item {
                    Text(
                        text = stringResource(R.string.onboarding_index_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onAcceptAllSafe) {
                        Text(stringResource(R.string.onboarding_accept_all_safe))
                    }
                    Button(onClick = onAcceptSelected) {
                        Text(stringResource(R.string.onboarding_accept_selected))
                    }
                    Button(onClick = onRefresh) {
                        Text(stringResource(R.string.onboarding_refresh))
                    }
                }
            }
            section(titleHigh, state.high, state.selected, onToggle)
            section(titleMedium, state.medium, state.selected, onToggle)
            section(titleWarning, state.warning, state.selected, onToggle, enabled = false)
            section(titleNone, state.none, state.selected, onToggle, enabled = false)
            state.message?.let { msg ->
                item { Text(msg, color = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    title: String,
    items: List<MatchCandidate>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    enabled: Boolean = true,
) {
    if (items.isEmpty()) return
    item {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
    }
    items(items, key = { it.packageName + it.repoId }) { candidate ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled && candidate.confidence != MatchConfidence.WARNING) {
                    onToggle(candidate.packageName)
                }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = candidate.packageName in selected,
                onCheckedChange = { if (enabled) onToggle(candidate.packageName) },
                enabled = enabled && candidate.confidence != MatchConfidence.WARNING,
            )
            Column(Modifier.weight(1f)) {
                Text(candidate.label, style = MaterialTheme.typography.bodyLarge)
                Text(candidate.packageName, style = MaterialTheme.typography.bodySmall)
                if (candidate.sourceLabel.isNotBlank()) {
                    Text(candidate.sourceLabel, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
