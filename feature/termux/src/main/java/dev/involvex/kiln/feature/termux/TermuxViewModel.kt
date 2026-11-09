package dev.involvex.kiln.feature.termux

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.involvex.kiln.core.common.IoDispatcher
import dev.involvex.kiln.core.model.TermuxPreferences
import dev.involvex.kiln.feature.termux.run.TermuxCommandLauncher
import dev.involvex.kiln.feature.termux.run.TermuxLaunchOutcome
import dev.involvex.kiln.feature.termux.run.TermuxResultBus
import dev.involvex.kiln.feature.termux.run.TermuxRunCommandIntentBuilder
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class TermuxViewModel @Inject constructor(
    private val preferences: TermuxPreferences,
    private val launcher: TermuxCommandLauncher,
    private val resultBus: TermuxResultBus,
    private val detector: TermuxPackageDetector,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TermuxUiState())
    val uiState: StateFlow<TermuxUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.observe().collect { prefs ->
                _uiState.update {
                    it.copy(
                        prefs = prefs,
                        packageIdDraft = prefs.packageId,
                    )
                }
                refreshEnvironment()
            }
        }
        viewModelScope.launch {
            resultBus.results.collect { result ->
                val pending = _uiState.value.pendingExecutionId
                if (pending != null && result.executionId != pending) return@collect
                _uiState.update {
                    it.copy(
                        isRunning = false,
                        pendingExecutionId = null,
                        lastResult = result,
                        statusKind = TermuxStatusKind.Result,
                        startFailedDetail = null,
                    )
                }
            }
        }
    }

    fun onScreenShown(title: String) {
        _uiState.update { it.copy(title = title) }
        refreshEnvironment()
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setEnabled(enabled) }
    }

    fun setPackageIdDraft(value: String) {
        _uiState.update { it.copy(packageIdDraft = value) }
    }

    fun savePackageId() {
        viewModelScope.launch {
            preferences.setPackageId(_uiState.value.packageIdDraft)
        }
    }

    fun refreshEnvironment() {
        val packageId = _uiState.value.prefs.packageId
        val installed = launcher.isPackageInstalled(packageId)
        _uiState.update {
            it.copy(
                packageInstalled = installed,
                permissionGranted = launcher.hasRunCommandPermission(packageId),
                // A working saved package id wins over a stale "not found" banner.
                detectionFailed = if (installed) false else it.detectionFailed,
            )
        }
    }

    /** Scan installed apps for a Termux (or renamed fork) and persist the package id. */
    fun detectPackageId() {
        viewModelScope.launch {
            // Runs PackageManager across every installed app — must not block main.
            val detected = withContext(ioDispatcher) { detector.detect() }
            if (detected != null) {
                preferences.setPackageId(detected.packageId)
                _uiState.update {
                    it.copy(
                        packageIdDraft = detected.packageId,
                        detectionFailed = false,
                    )
                }
            } else {
                _uiState.update { state ->
                    state.copy(detectionFailed = !state.packageInstalled)
                }
            }
        }
    }

    fun runEchoSmokeTest() {
        val state = _uiState.value
        if (!state.prefs.enabled) {
            _uiState.update {
                it.copy(
                    statusKind = TermuxStatusKind.Disabled,
                    lastResult = null,
                    startFailedDetail = null,
                )
            }
            return
        }
        val executionId = launcher.nextExecutionId()
        val request = TermuxRunCommandIntentBuilder.echoSmokeTest(
            packageId = state.prefs.packageId,
            executionId = executionId,
        )
        _uiState.update {
            it.copy(
                isRunning = true,
                pendingExecutionId = executionId,
                lastResult = null,
                statusKind = TermuxStatusKind.Running,
                startFailedDetail = null,
            )
        }
        when (val outcome = launcher.launch(request)) {
            is TermuxLaunchOutcome.Started -> Unit

            TermuxLaunchOutcome.PackageNotInstalled -> {
                _uiState.update {
                    it.copy(
                        isRunning = false,
                        pendingExecutionId = null,
                        statusKind = TermuxStatusKind.NotInstalled,
                    )
                }
            }

            TermuxLaunchOutcome.PermissionDenied -> {
                _uiState.update {
                    it.copy(
                        isRunning = false,
                        pendingExecutionId = null,
                        statusKind = TermuxStatusKind.PermissionDenied,
                    )
                }
            }

            is TermuxLaunchOutcome.StartFailed -> {
                _uiState.update {
                    it.copy(
                        isRunning = false,
                        pendingExecutionId = null,
                        statusKind = TermuxStatusKind.StartFailed,
                        startFailedDetail = outcome.message,
                    )
                }
            }
        }
    }
}
