package dev.involvex.kiln.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.R
import dev.involvex.kiln.feature.apps.apktools.ApkToolsRoute
import dev.involvex.kiln.feature.apps.apktools.IncomingApkInstallStore
import dev.involvex.kiln.feature.backup.BackupRoute
import dev.involvex.kiln.feature.storage.StorageRoute

private const val TAB_APK_TOOLS = 2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageRoute(
    modifier: Modifier = Modifier,
    onOpenPrivilegeSetup: () -> Unit = {},
    incomingApkInstallStore: IncomingApkInstallStore? = null,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    if (incomingApkInstallStore != null) {
        val pendingUri by incomingApkInstallStore.pending.collectAsStateWithLifecycle()
        LaunchedEffect(pendingUri) {
            if (pendingUri != null) {
                selectedTab = TAB_APK_TOOLS
            }
        }
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.nav_manage)) })
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.manage_tab_storage)) },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.manage_tab_backup)) },
                )
                Tab(
                    selected = selectedTab == TAB_APK_TOOLS,
                    onClick = { selectedTab = TAB_APK_TOOLS },
                    text = { Text(stringResource(R.string.manage_tab_apk_tools)) },
                )
            }
            when (selectedTab) {
                0 -> StorageRoute(
                    showTopBar = false,
                    onOpenPrivilegeSetup = onOpenPrivilegeSetup,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )

                1 -> BackupRoute(
                    showTopBar = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )

                else -> ApkToolsRoute(
                    showTopBar = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
        }
    }
}
