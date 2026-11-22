package dev.involvex.kiln.feature.sources.discovery

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.feature.sources.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallVariantSheet(
    target: InstallVariantTarget,
    actions: InstallVariantActions,
    onDismiss: () -> Unit,
    onInstalled: (message: String, isError: Boolean) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var fdroidOptions by remember { mutableStateOf<FdroidInstallOptions?>(null) }
    var githubOptions by remember { mutableStateOf<GithubInstallOptions?>(null) }

    LaunchedEffect(target) {
        loading = true
        error = null
        fdroidOptions = null
        githubOptions = null
        when (target) {
            is InstallVariantTarget.Fdroid -> when (
                val result = actions.loadFdroid(target.packageName, target.repoId)
            ) {
                is Result.Success -> fdroidOptions = result.value
                is Result.Failure -> error = result.message
            }

            is InstallVariantTarget.Github -> when (
                val result = actions.loadGithub(target.ownerRepo)
            ) {
                is Result.Success -> githubOptions = result.value
                is Result.Failure -> error = result.message
            }
        }
        loading = false
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.discovery_install_variants_title, targetDisplayName(target)),
                style = MaterialTheme.typography.titleLarge,
            )
            when {
                loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))

                error != null -> Text(
                    text = formatDiscoveryMessage(error!!),
                    color = MaterialTheme.colorScheme.error,
                )

                fdroidOptions != null -> FdroidVariantContent(
                    options = fdroidOptions!!,
                    displayName = targetDisplayName(target),
                    actions = actions,
                    onDismiss = onDismiss,
                    onInstalled = onInstalled,
                )

                githubOptions != null -> GithubVariantContent(
                    options = githubOptions!!,
                    displayName = targetDisplayName(target),
                    actions = actions,
                    onDismiss = onDismiss,
                    onInstalled = onInstalled,
                )
            }
        }
    }
}

@Composable
private fun FdroidVariantContent(
    options: FdroidInstallOptions,
    displayName: String,
    actions: InstallVariantActions,
    onDismiss: () -> Unit,
    onInstalled: (String, Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val deviceAbis = remember { Build.SUPPORTED_ABIS.toList() }
    var selectedVersionCode by remember {
        mutableStateOf(options.versions.firstOrNull()?.versionCode)
    }
    var abiChoice by remember { mutableStateOf(InstallVariantSelection.ABI_AUTO) }
    var confirming by remember { mutableStateOf(false) }
    val selected = options.versions.firstOrNull { it.versionCode == selectedVersionCode }
        ?: options.versions.firstOrNull()

    Text(stringResource(R.string.discovery_install_variants_version), style = MaterialTheme.typography.titleMedium)
    options.versions.forEach { group ->
        VersionRadioRow(
            title = group.versionName,
            subtitle = stringResource(R.string.discovery_install_variants_code, group.versionCode),
            selected = group.versionCode == selected?.versionCode,
            onClick = {
                selectedVersionCode = group.versionCode
                abiChoice = InstallVariantSelection.ABI_AUTO
            },
        )
    }
    if (selected != null) {
        AbiChipRow(
            choices = InstallVariantSelection.availableAbiChoices(selected.allNativeCodes()),
            selected = abiChoice,
            onSelect = { abiChoice = it },
        )
    }
    ConfirmRow(
        confirming = confirming,
        onCancel = onDismiss,
        onConfirm = {
            if (selected == null || confirming) return@ConfirmRow
            val variant = selected.variantForAbi(abiChoice, deviceAbis) ?: return@ConfirmRow
            confirming = true
            scope.launch {
                val result = actions.installFdroid(
                    options.packageName,
                    options.repoId,
                    selected.versionCode,
                    selected.versionName,
                    variant.apkName,
                    variant.apkSha256,
                    variant.sizeBytes,
                    selected.whatsNew,
                )
                confirming = false
                when (result) {
                    is Result.Success -> {
                        onInstalled("install_queued:$displayName", false)
                        onDismiss()
                    }

                    is Result.Failure -> onInstalled(result.message, true)
                }
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GithubVariantContent(
    options: GithubInstallOptions,
    displayName: String,
    actions: InstallVariantActions,
    onDismiss: () -> Unit,
    onInstalled: (String, Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val deviceAbis = remember { Build.SUPPORTED_ABIS.toList() }
    var channelFilter by remember { mutableStateOf(GithubChannelFilter.ALL) }
    var selectedGithubTag by remember {
        mutableStateOf(options.releases.firstOrNull()?.tagName)
    }
    var abiChoice by remember { mutableStateOf(InstallVariantSelection.ABI_AUTO) }
    var confirming by remember { mutableStateOf(false) }
    val filtered = options.releases.filter { channelFilter.matches(it) }
    val selected = filtered.firstOrNull { it.tagName == selectedGithubTag } ?: filtered.firstOrNull()

    Text(stringResource(R.string.discovery_install_variants_channel), style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GithubChannelFilter.entries.forEach { filter ->
            FilterChip(
                selected = channelFilter == filter,
                onClick = { channelFilter = filter },
                label = { Text(stringResource(filter.labelRes)) },
            )
        }
    }
    Text(stringResource(R.string.discovery_install_variants_version), style = MaterialTheme.typography.titleMedium)
    filtered.forEach { row ->
        VersionRadioRow(
            title = row.tagName,
            subtitle = buildGithubSubtitle(row),
            selected = row.tagName == selected?.tagName,
            onClick = {
                selectedGithubTag = row.tagName
                abiChoice = InstallVariantSelection.ABI_AUTO
            },
        )
    }
    if (selected != null) {
        AbiChipRow(
            choices = InstallVariantSelection.availableAbiChoicesFromAssets(selected.assets.map { it.name }),
            selected = abiChoice,
            onSelect = { abiChoice = it },
        )
    }
    ConfirmRow(
        confirming = confirming,
        onCancel = onDismiss,
        onConfirm = {
            if (selected == null || confirming) return@ConfirmRow
            val assetName = InstallVariantSelection.pickAssetName(
                selected.assets.map { it.name },
                deviceAbis,
                abiChoice,
            ) ?: return@ConfirmRow
            val asset = selected.assets.firstOrNull { it.name == assetName } ?: return@ConfirmRow
            confirming = true
            scope.launch {
                val result = actions.installGithub(
                    options.packageName,
                    options.ownerRepo,
                    selected.tagName,
                    asset.downloadUrl,
                    asset.sizeBytes,
                    selected.releaseNotes,
                )
                confirming = false
                when (result) {
                    is Result.Success -> {
                        onInstalled("install_queued:$displayName", false)
                        onDismiss()
                    }

                    is Result.Failure -> onInstalled(result.message, true)
                }
            }
        },
    )
}

private fun targetDisplayName(target: InstallVariantTarget): String = when (target) {
    is InstallVariantTarget.Fdroid -> target.displayName
    is InstallVariantTarget.Github -> target.displayName
}

@Composable
private fun buildGithubSubtitle(row: GithubReleaseVariantRow): String {
    val badges = buildList {
        if (row.latestStable) add(stringResource(R.string.discovery_install_badge_latest))
        if (row.prerelease) add(stringResource(R.string.discovery_install_badge_pre))
        if (row.nightly) add(stringResource(R.string.discovery_install_badge_nightly))
        row.releaseName?.takeIf { it.isNotBlank() && it != row.tagName }?.let { add(it) }
    }
    return badges.joinToString(" · ")
}

@Composable
private fun VersionRadioRow(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle.isNotBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AbiChipRow(choices: List<String>, selected: String, onSelect: (String) -> Unit) {
    Text(
        stringResource(R.string.discovery_install_variants_abi),
        style = MaterialTheme.typography.titleMedium,
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        choices.forEach { choice ->
            FilterChip(
                selected = selected == choice,
                onClick = { onSelect(choice) },
                label = { Text(abiLabel(choice)) },
            )
        }
    }
}

@Composable
private fun abiLabel(choice: String): String = when (choice) {
    InstallVariantSelection.ABI_AUTO -> stringResource(R.string.discovery_install_abi_auto)
    InstallVariantSelection.ABI_UNIVERSAL -> stringResource(R.string.discovery_install_abi_universal)
    else -> choice
}

@Composable
private fun ConfirmRow(confirming: Boolean, onCancel: () -> Unit, onConfirm: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f), enabled = !confirming) {
            Text(stringResource(R.string.discovery_install_variants_cancel))
        }
        Button(onClick = onConfirm, modifier = Modifier.weight(1f), enabled = !confirming) {
            if (confirming) {
                CircularProgressIndicator()
            } else {
                Text(stringResource(R.string.discovery_install_variants_confirm))
            }
        }
    }
}

private enum class GithubChannelFilter(val labelRes: Int) {
    ALL(R.string.discovery_install_filter_all),
    STABLE(R.string.discovery_install_filter_stable),
    PRE(R.string.discovery_install_filter_pre),
    NIGHTLY(R.string.discovery_install_filter_nightly),
    ;

    fun matches(row: GithubReleaseVariantRow): Boolean = when (this) {
        ALL -> true
        STABLE -> !row.prerelease && !row.nightly
        PRE -> row.prerelease && !row.nightly
        NIGHTLY -> row.nightly
    }
}
