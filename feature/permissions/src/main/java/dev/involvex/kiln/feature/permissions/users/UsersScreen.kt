package dev.involvex.kiln.feature.permissions.users

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.designsystem.component.KilnScaffold
import dev.involvex.kiln.core.designsystem.kilnHiltViewModel
import dev.involvex.kiln.core.model.AndroidUserInfo
import dev.involvex.kiln.feature.permissions.R

@Composable
fun UsersRoute(onBack: () -> Unit, onOpenPrivilegeSetup: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel = kilnHiltViewModel<UsersViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    UsersScreen(
        state = state,
        onBack = onBack,
        onOpenPrivilegeSetup = onOpenPrivilegeSetup,
        onRefresh = viewModel::refresh,
        modifier = modifier,
    )
}

@Composable
fun UsersScreen(
    state: UsersUiState,
    onBack: () -> Unit,
    onOpenPrivilegeSetup: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KilnScaffold(title = stringResource(R.string.users_title), modifier = modifier) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.permissions_back))
            }
            Text(
                stringResource(R.string.users_explain),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.users_current, state.currentUserId),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (!state.canList) {
                Text(
                    stringResource(R.string.users_need_shizuku),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(onClick = onOpenPrivilegeSetup, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.users_open_setup))
                }
            } else {
                OutlinedButton(onClick = onRefresh, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.users_refresh))
                }
            }
            if (!state.errorKey.isNullOrBlank()) {
                Text(
                    stringResource(R.string.users_error, state.errorKey.orEmpty()),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            if (state.canList && state.users.isEmpty() && !state.isLoading) {
                Text(
                    stringResource(R.string.users_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.canList && state.users.size == 1 && state.users.firstOrNull()?.id == 0) {
                Text(
                    stringResource(R.string.users_only_owner),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.users, key = { it.id }) { user ->
                    UserCard(user = user, isCurrent = user.id == state.currentUserId)
                }
            }
        }
    }
}

@Composable
private fun UserCard(user: AndroidUserInfo, isCurrent: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.users_row_title, user.id, user.name),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = when {
                    isCurrent && user.running -> stringResource(R.string.users_row_current_running)
                    isCurrent -> stringResource(R.string.users_row_current)
                    user.running -> stringResource(R.string.users_row_running)
                    else -> stringResource(R.string.users_row_stopped)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
