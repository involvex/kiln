package dev.involvex.kiln.feature.backup

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
fun BackupRoute(modifier: Modifier = Modifier, viewModel: BackupViewModel = hiltViewModel()) {
    val title = stringResource(R.string.feature_backup_title)
    val description = stringResource(R.string.feature_backup_description)
    LaunchedEffect(title, description) {
        viewModel.onScreenShown(title, description)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BackupScreen(state = state, modifier = modifier)
}

@Composable
fun BackupScreen(state: BackupUiState, modifier: Modifier = Modifier) {
    val title = state.title.ifBlank { stringResource(R.string.feature_backup_title) }
    val description = state.description.ifBlank {
        stringResource(R.string.feature_backup_description)
    }
    KilnScaffold(title = title, modifier = modifier) { padding ->
        EmptyState(
            title = title,
            description = description,
            modifier = Modifier.padding(padding),
        )
    }
}
