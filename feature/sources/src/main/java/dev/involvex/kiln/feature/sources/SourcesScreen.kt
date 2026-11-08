package dev.involvex.kiln.feature.sources

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.designsystem.component.EmptyState
import dev.involvex.kiln.core.designsystem.component.KilnScaffold

@Composable
fun SourcesRoute(modifier: Modifier = Modifier, viewModel: SourcesViewModel = hiltViewModel()) {
    val title = stringResource(R.string.feature_sources_title)
    val description = stringResource(R.string.feature_sources_description)
    LaunchedEffect(title, description) {
        viewModel.onScreenShown(title, description)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SourcesScreen(state = state, modifier = modifier)
}

@Composable
fun SourcesScreen(state: SourcesUiState, modifier: Modifier = Modifier) {
    val title = state.title.ifBlank { stringResource(R.string.feature_sources_title) }
    val description = state.description.ifBlank {
        stringResource(R.string.feature_sources_description)
    }
    KilnScaffold(title = title, modifier = modifier) { padding ->
        EmptyState(
            title = title,
            description = description,
            modifier = Modifier.padding(padding),
        )
    }
}
