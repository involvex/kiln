package dev.involvex.kiln.feature.onboarding.privilege

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.involvex.kiln.core.designsystem.component.KilnScaffold
import dev.involvex.kiln.core.model.CapabilityAvailability
import dev.involvex.kiln.core.model.PrivilegeCapability
import dev.involvex.kiln.core.model.SelfTestReport
import dev.involvex.kiln.core.model.ShizukuSetupPhase
import dev.involvex.kiln.feature.onboarding.R
import kotlinx.coroutines.delay

@Composable
fun PrivilegeSetupRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: PrivilegeSetupViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    if (state.copied) {
        LaunchedEffect(state.copied) {
            delay(1500)
            viewModel.clearCopied()
        }
    }
    PrivilegeSetupScreen(
        state = state,
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onRequestPermission = viewModel::requestPermission,
        onRunSelfTest = viewModel::runSelfTest,
        onCopyDiagnosis = viewModel::copyDiagnosis,
        onCopyAdb = viewModel::copyAdbCommand,
        openManager = viewModel::openManagerApp,
        openDeveloperOptions = viewModel::openDeveloperOptions,
        openInstallPage = viewModel::shizukuInstallPage,
        adbCommand = viewModel.adbStartCommand(),
        modifier = modifier,
    )
}

@Composable
fun PrivilegeSetupScreen(
    state: PrivilegeSetupUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onRequestPermission: () -> Unit,
    onRunSelfTest: () -> Unit,
    onCopyDiagnosis: () -> Unit,
    onCopyAdb: () -> Unit,
    openManager: () -> Intent?,
    openDeveloperOptions: () -> Intent,
    openInstallPage: () -> Intent,
    adbCommand: String,
    modifier: Modifier = Modifier,
) {
    KilnScaffold(
        title = stringResource(R.string.privilege_setup_title),
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues(16.dp)),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PrivilegeSetupHeader(state = state)
            PrivilegePhaseActions(
                state = state,
                onRequestPermission = onRequestPermission,
                onRunSelfTest = onRunSelfTest,
                onCopyDiagnosis = onCopyDiagnosis,
                onCopyAdb = onCopyAdb,
                openManager = openManager,
                openDeveloperOptions = openDeveloperOptions,
                openInstallPage = openInstallPage,
                adbCommand = adbCommand,
            )
            OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.privilege_refresh))
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.onboarding_back))
            }
        }
    }
}

@Composable
private fun PrivilegeSetupHeader(state: PrivilegeSetupUiState) {
    Text(
        text = stringResource(R.string.privilege_setup_intro),
        style = MaterialTheme.typography.bodyMedium,
    )
    Text(
        text = phaseLabel(state.phase),
        style = MaterialTheme.typography.titleMedium,
    )
    state.status.managerLabel?.let { label ->
        Text(
            text = stringResource(R.string.privilege_manager_detected, label),
            style = MaterialTheme.typography.bodySmall,
        )
    }
    state.message?.let {
        Text(text = it, color = MaterialTheme.colorScheme.error)
    }
    if (state.copied) {
        Text(text = stringResource(R.string.privilege_copied))
    }
}

@Composable
private fun PrivilegePhaseActions(
    state: PrivilegeSetupUiState,
    onRequestPermission: () -> Unit,
    onRunSelfTest: () -> Unit,
    onCopyDiagnosis: () -> Unit,
    onCopyAdb: () -> Unit,
    openManager: () -> Intent?,
    openDeveloperOptions: () -> Intent,
    openInstallPage: () -> Intent,
    adbCommand: String,
) {
    when (state.phase) {
        ShizukuSetupPhase.NOT_INSTALLED -> PhaseNotInstalled(openInstallPage)

        ShizukuSetupPhase.NOT_RUNNING -> PhaseNotRunning(
            openManager = openManager,
            openDeveloperOptions = openDeveloperOptions,
            onCopyAdb = onCopyAdb,
            adbCommand = adbCommand,
        )

        ShizukuSetupPhase.NO_PERMISSION -> PhaseNoPermission(
            onRequestPermission = onRequestPermission,
            openManager = openManager,
        )

        ShizukuSetupPhase.READY -> PhaseReady(
            state = state,
            onRunSelfTest = onRunSelfTest,
            onCopyDiagnosis = onCopyDiagnosis,
        )
    }
}

@Composable
private fun PhaseNotInstalled(openInstallPage: () -> Intent) {
    val context = LocalContext.current
    Text(stringResource(R.string.privilege_phase_a_body))
    Button(
        onClick = { context.startActivity(openInstallPage()) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.privilege_install_shizuku))
    }
}

@Composable
private fun PhaseNotRunning(openManager: () -> Intent?, openDeveloperOptions: () -> Intent, onCopyAdb: () -> Unit, adbCommand: String) {
    val context = LocalContext.current
    Text(stringResource(R.string.privilege_phase_b_body))
    Text(stringResource(R.string.privilege_hyperos_hint))
    Button(
        onClick = { openManager()?.let { context.startActivity(it) } },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.privilege_open_manager))
    }
    OutlinedButton(
        onClick = { context.startActivity(openDeveloperOptions()) },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.privilege_open_developer))
    }
    Text(
        text = stringResource(R.string.privilege_adb_hint, adbCommand),
        style = MaterialTheme.typography.bodySmall,
    )
    OutlinedButton(onClick = onCopyAdb, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.privilege_copy_adb))
    }
}

@Composable
private fun PhaseNoPermission(onRequestPermission: () -> Unit, openManager: () -> Intent?) {
    val context = LocalContext.current
    Text(stringResource(R.string.privilege_phase_c_body))
    Button(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.privilege_request_permission))
    }
    OutlinedButton(
        onClick = { openManager()?.let { context.startActivity(it) } },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.privilege_open_manager))
    }
}

@Composable
private fun PhaseReady(state: PrivilegeSetupUiState, onRunSelfTest: () -> Unit, onCopyDiagnosis: () -> Unit) {
    Text(stringResource(R.string.privilege_phase_d_body))
    Button(
        onClick = onRunSelfTest,
        enabled = !state.isRunningSelfTest,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.privilege_run_self_test))
    }
    if (state.isRunningSelfTest) {
        CircularProgressIndicator()
    }
    state.status.selfTest?.let { report ->
        SelfTestResults(report = report, onCopyDiagnosis = onCopyDiagnosis)
    }
}

@Composable
private fun SelfTestResults(report: SelfTestReport, onCopyDiagnosis: () -> Unit) {
    Text(
        text = stringResource(
            R.string.privilege_self_test_summary,
            report.uid,
            report.selinuxContext ?: "—",
            report.androidApi,
            report.shizukuApiVersion ?: -1,
        ),
        style = MaterialTheme.typography.bodySmall,
    )
    Text(
        text = stringResource(R.string.privilege_matrix_title),
        style = MaterialTheme.typography.titleSmall,
    )
    PrivilegeCapability.entries.forEach { cap ->
        val avail = report.matrix[cap] ?: CapabilityAvailability.UNAVAILABLE
        Text(
            text = "${cap.name}: ${availabilityLabel(avail)}",
            style = MaterialTheme.typography.bodySmall,
        )
    }
    OutlinedButton(onClick = onCopyDiagnosis, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.privilege_copy_diagnosis))
    }
}

@Composable
private fun phaseLabel(phase: ShizukuSetupPhase): String = when (phase) {
    ShizukuSetupPhase.NOT_INSTALLED -> stringResource(R.string.privilege_phase_a)
    ShizukuSetupPhase.NOT_RUNNING -> stringResource(R.string.privilege_phase_b)
    ShizukuSetupPhase.NO_PERMISSION -> stringResource(R.string.privilege_phase_c)
    ShizukuSetupPhase.READY -> stringResource(R.string.privilege_phase_d)
}

@Composable
private fun availabilityLabel(avail: CapabilityAvailability): String = when (avail) {
    CapabilityAvailability.AVAILABLE -> stringResource(R.string.privilege_avail_yes)
    CapabilityAvailability.RESTRICTED -> stringResource(R.string.privilege_avail_restricted)
    CapabilityAvailability.UNAVAILABLE -> stringResource(R.string.privilege_avail_no)
}
