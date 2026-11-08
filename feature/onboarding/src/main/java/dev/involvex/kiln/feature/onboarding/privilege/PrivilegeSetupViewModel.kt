package dev.involvex.kiln.feature.onboarding.privilege

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.PrivilegedExecutor
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PrivilegeSetupViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val executor: PrivilegedExecutor,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PrivilegeSetupUiState())
    val uiState: StateFlow<PrivilegeSetupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            executor.observeStatus().collect { status ->
                _uiState.update { it.copy(status = status) }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            executor.refresh()
        }
    }

    fun requestPermission() {
        executor.requestPermission(REQUEST_CODE_SHIZUKU)
        refresh()
    }

    fun runSelfTest() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRunningSelfTest = true, message = null) }
            when (val result = executor.runSelfTest()) {
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(isRunningSelfTest = false, message = result.message)
                    }
                }

                is Result.Success -> {
                    val report = result.value
                    val diagnosis = buildString {
                        appendLine("Kiln privilege self-test")
                        appendLine("uid=${report.uid}")
                        appendLine("selinux=${report.selinuxContext}")
                        appendLine("androidApi=${report.androidApi}")
                        appendLine("shizukuApi=${report.shizukuApiVersion}")
                        appendLine("manager=${report.managerPackage} ${report.managerVersionName}")
                        appendLine("raw=${report.rawOutput}")
                        appendLine("matrix:")
                        report.matrix.forEach { (cap, avail) ->
                            appendLine("  $cap=$avail")
                        }
                    }
                    _uiState.update {
                        it.copy(
                            isRunningSelfTest = false,
                            message = null,
                            diagnosisText = diagnosis,
                        )
                    }
                }
            }
        }
    }

    fun copyDiagnosis() {
        val text = _uiState.value.diagnosisText ?: return
        val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("kiln-privilege", text))
        _uiState.update { it.copy(copied = true) }
    }

    fun openManagerApp(): Intent? {
        val pkg = _uiState.value.status.managerPackage ?: return null
        return appContext.packageManager.getLaunchIntentForPackage(pkg)
    }

    fun openDeveloperOptions(): Intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun shizukuInstallPage(): Intent {
        // Prefer Shizuku GitHub Releases (sideload / F-Droid users); market as fallback.
        return Intent(
            Intent.ACTION_VIEW,
            "https://github.com/RikkaApps/Shizuku/releases".toUri(),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun adbStartCommand(): String {
        val pkg = _uiState.value.status.managerPackage ?: DEFAULT_SHIZUKU_PACKAGE
        return "adb shell sh /storage/emulated/0/Android/data/$pkg/start.sh"
    }

    fun copyAdbCommand() {
        val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("kiln-adb-shizuku", adbStartCommand()))
        _uiState.update { it.copy(copied = true) }
    }

    fun clearCopied() {
        _uiState.update { it.copy(copied = false) }
    }

    companion object {
        const val REQUEST_CODE_SHIZUKU = 9401
        private const val DEFAULT_SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    }
}
