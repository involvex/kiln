package dev.involvex.kiln.feature.sources

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.designsystem.kilnHiltViewModel

@Composable
fun SourcesImportRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = kilnHiltViewModel<SourcesViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SourcesImportScreen(
        obtainiumText = state.obtainiumText,
        message = state.message,
        onBack = onBack,
        onObtainiumTextChange = viewModel::onObtainiumTextChange,
        onImportObtainium = viewModel::importObtainium,
        onImportObtainiumUri = viewModel::importObtainiumFromUri,
        onClearMessage = viewModel::clearMessage,
        modifier = modifier,
    )
}

@Composable
fun SourcesImportScreen(
    obtainiumText: String,
    message: String?,
    onBack: () -> Unit,
    onObtainiumTextChange: (String) -> Unit,
    onImportObtainium: () -> Unit,
    onImportObtainiumUri: (Uri) -> Unit,
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onImportObtainiumUri(uri)
    }
    SourcesCategoryScaffold(
        title = stringResource(R.string.sources_hub_import),
        onBack = onBack,
        modifier = modifier,
    ) { padding ->
        SourcesScrollColumn(padding = padding) {
            Text(
                stringResource(R.string.sources_obtainium_hint),
                style = MaterialTheme.typography.bodySmall,
            )
            Button(
                onClick = { picker.launch(arrayOf("application/json", "text/*", "*/*")) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.sources_obtainium_pick_file))
            }
            OutlinedTextField(
                value = obtainiumText,
                onValueChange = onObtainiumTextChange,
                label = { Text(stringResource(R.string.sources_obtainium_paste)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
            )
            Button(onClick = onImportObtainium, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.sources_obtainium_import))
            }
            message?.let { msg ->
                SourcesMessageBanner(message = msg, onClearMessage = onClearMessage)
            }
        }
    }
}
