package dev.involvex.kiln.feature.apps

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
fun AppsRoute(modifier: Modifier = Modifier, viewModel: AppsViewModel = hiltViewModel()) {
    val title = stringResource(R.string.feature_apps_title)
    val description = stringResource(R.string.feature_apps_description)
    LaunchedEffect(title, description) {
        viewModel.onScreenShown(title, description)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AppsScreen(
        state = state,
        modifier = modifier,
    )
}

@Composable
fun AppsScreen(state: AppsUiState, modifier: Modifier = Modifier) {
    val title = state.title.ifBlank { stringResource(R.string.feature_apps_title) }
    val description = state.description.ifBlank {
        stringResource(R.string.feature_apps_description)
    }
    KilnScaffold(
        title = title,
        modifier = modifier,
    ) { padding ->
        EmptyState(
            title = title,
            description = description,
            modifier = Modifier.padding(padding),
        )
    }
}
