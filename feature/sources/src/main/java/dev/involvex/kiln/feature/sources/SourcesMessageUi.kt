package dev.involvex.kiln.feature.sources

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
internal fun SourcesMessageBanner(message: String, onClearMessage: () -> Unit) {
    Column {
        Text(
            text = formatSourcesMessage(message),
            color = MaterialTheme.colorScheme.primary,
        )
        TextButton(onClick = onClearMessage) {
            Text(stringResource(R.string.sources_dismiss))
        }
    }
}

@Composable
internal fun formatSourcesMessage(code: String): String {
    exactMessageRes(code)?.let { return stringResource(it) }
    return formatPrefixedMessage(code) ?: code
}

private fun exactMessageRes(code: String): Int? = EXACT_MESSAGE_RES[code]

private val EXACT_MESSAGE_RES = mapOf(
    "invalid_repo" to R.string.sources_msg_invalid_repo,
    "repo_added" to R.string.sources_msg_repo_added,
    "repo_uri_filled" to R.string.sources_msg_repo_uri_filled,
    "repo_http_warn" to R.string.sources_msg_repo_http_warn,
    "mirrors_saved" to R.string.sources_msg_mirrors_saved,
    "token_saved" to R.string.sources_msg_token_saved,
    "token_cleared" to R.string.sources_msg_token_cleared,
    "obtainium_empty" to R.string.sources_msg_obtainium_empty,
    "obtainium_too_short" to R.string.sources_msg_obtainium_too_short,
    "obtainium_no_apps" to R.string.sources_msg_obtainium_no_apps,
    "obtainium_file_read_failed" to R.string.sources_msg_obtainium_file_read_failed,
    "index_oom" to R.string.sources_msg_index_oom,
    "share_unrecognized" to R.string.sources_msg_share_unrecognized,
    "sync_cancelled" to R.string.sources_msg_sync_cancelled,
    "github_pat_required" to R.string.sources_msg_github_pat_required,
    "stars_empty" to R.string.sources_msg_stars_empty,
    "stars_none_selected" to R.string.sources_msg_stars_none_selected,
    "stars_unauthorized" to R.string.sources_msg_stars_unauthorized,
    "stars_rate_limited" to R.string.sources_msg_stars_rate_limited,
)

@Composable
private fun formatPrefixedMessage(code: String): String? {
    formatStarsPrefixedMessage(code)?.let { return it }
    return formatSyncPrefixedMessage(code) ?: formatMiscPrefixedMessage(code)
}

@Composable
private fun formatStarsPrefixedMessage(code: String): String? = when {
    code.startsWith("stars_added:") -> {
        val parts = code.removePrefix("stars_added:").split(':')
        val added = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val existed = parts.getOrNull(1)?.toIntOrNull() ?: 0
        stringResource(R.string.sources_msg_stars_added, added, existed)
    }

    code.startsWith("stars_failed:") ->
        stringResource(R.string.sources_msg_stars_failed, code.removePrefix("stars_failed:"))

    else -> null
}

@Composable
private fun formatMiscPrefixedMessage(code: String): String? = when {
    code.startsWith("package_page:") ->
        stringResource(R.string.sources_msg_package_page, code.removePrefix("package_page:"))

    code.startsWith("github_added:") ->
        stringResource(R.string.sources_msg_github_added, code.removePrefix("github_added:"))

    code.startsWith("github_exists:") ->
        stringResource(R.string.sources_msg_github_exists, code.removePrefix("github_exists:"))

    code.startsWith("obtainium:") ->
        stringResource(R.string.sources_msg_obtainium_imported, code.substringAfter(':'))

    else -> null
}

@Composable
private fun formatSyncPrefixedMessage(code: String): String? = when {
    code.startsWith("sync_ok:") ->
        stringResource(R.string.sources_msg_sync_ok, code.substringAfter(':'))

    code.startsWith("sync_fail:") -> {
        val parts = code.split(':', limit = 3)
        stringResource(
            R.string.sources_msg_sync_fail,
            parts.getOrNull(1) ?: "?",
            humanizeSyncDetail(parts.getOrNull(2).orEmpty()),
        )
    }

    code.startsWith("sync_partial:") -> {
        val parts = code.split(':', limit = 4)
        stringResource(
            R.string.sources_msg_sync_partial,
            parts.getOrNull(1) ?: "?",
            parts.getOrNull(2) ?: "?",
            humanizeSyncDetail(parts.getOrNull(3).orEmpty()),
        )
    }

    else -> null
}

@Composable
internal fun humanizeSyncDetail(detail: String): String = when {
    detail.contains("index_oom") -> stringResource(R.string.sources_msg_index_oom)
    else -> detail
}

@Composable
internal fun formatSyncProgress(state: SourcesUiState): String {
    val label = state.syncProgressLabel
    val display = when (label) {
        null, "" -> stringResource(R.string.sources_syncing)
        "github" -> stringResource(R.string.sources_syncing_github)
        else -> label
    }
    return if (state.syncProgressTotal > 0) {
        stringResource(
            R.string.sources_syncing_progress,
            state.syncProgressCurrent,
            state.syncProgressTotal,
            display,
        )
    } else {
        display
    }
}
