package dev.involvex.kiln.feature.settings

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.common.IoDispatcher
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.ApkRollbackCache
import dev.involvex.kiln.core.model.AppearancePreferences
import dev.involvex.kiln.core.model.AutomationPreferences
import dev.involvex.kiln.core.model.BackgroundUpdatePreferences
import dev.involvex.kiln.core.model.BackgroundUpdateScheduler
import dev.involvex.kiln.core.model.DebugLogController
import dev.involvex.kiln.core.model.DevPreferences
import dev.involvex.kiln.core.model.ExodusPrivacyPreferences
import dev.involvex.kiln.core.model.GitHubTokenRepository
import dev.involvex.kiln.core.model.InstallerPreferences
import dev.involvex.kiln.core.model.KilnConfigFilenames
import dev.involvex.kiln.core.model.KilnConfigRepository
import dev.involvex.kiln.core.model.KilnFontStyle
import dev.involvex.kiln.core.model.KilnLanguage
import dev.involvex.kiln.core.model.KilnThemeStyle
import dev.involvex.kiln.core.model.MaintenancePreferences
import dev.involvex.kiln.core.model.MaintenanceRunStore
import dev.involvex.kiln.core.model.MaintenanceScheduler
import dev.involvex.kiln.core.model.NetworkProxyPreferences
import dev.involvex.kiln.core.model.NetworkProxyType
import dev.involvex.kiln.core.model.PreferredInstaller
import dev.involvex.kiln.core.model.PrivilegeCapability
import dev.involvex.kiln.core.model.PrivilegedExecutor
import java.io.IOException
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@HiltViewModel
@Suppress("TooManyFunctions") // Settings aggregates many independent preference setters.
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: BackgroundUpdatePreferences,
    private val networkProxyPreferences: NetworkProxyPreferences,
    private val exodusPrivacyPreferences: ExodusPrivacyPreferences,
    private val automationPreferences: AutomationPreferences,
    private val maintenancePreferences: MaintenancePreferences,
    private val maintenanceRunStore: MaintenanceRunStore,
    private val appearancePreferences: AppearancePreferences,
    private val installerPreferences: InstallerPreferences,
    private val devPreferences: DevPreferences,
    private val debugLogController: DebugLogController,
    private val privilegedExecutor: PrivilegedExecutor,
    private val scheduler: BackgroundUpdateScheduler,
    private val maintenanceScheduler: MaintenanceScheduler,
    private val kilnConfigRepository: KilnConfigRepository,
    private val gitHubTokenRepository: GitHubTokenRepository,
    private val apkRollbackCache: ApkRollbackCache,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                appVersionName = readVersionName(),
                appVersionCode = readVersionCode(),
                githubTokenSet = gitHubTokenRepository.isTokenSet(),
            )
        }
        viewModelScope.launch {
            preferences.observe().collect { prefs ->
                _uiState.update { it.copy(backgroundUpdates = prefs) }
                scheduler.apply(prefs)
            }
        }
        viewModelScope.launch {
            networkProxyPreferences.observe().collect { prefs ->
                _uiState.update { it.copy(networkProxy = prefs) }
            }
        }
        viewModelScope.launch {
            exodusPrivacyPreferences.observe().collect { prefs ->
                _uiState.update {
                    it.copy(
                        exodusPrivacy = prefs,
                        exodusBaseUrlInput = prefs.apiBaseUrl,
                    )
                }
            }
        }
        viewModelScope.launch {
            automationPreferences.observe().collect { prefs ->
                _uiState.update { it.copy(automation = prefs) }
            }
        }
        viewModelScope.launch {
            maintenancePreferences.observe().collect { prefs ->
                _uiState.update { it.copy(maintenance = prefs) }
                maintenanceScheduler.apply(prefs)
            }
        }
        viewModelScope.launch {
            maintenanceRunStore.observe().collect { runs ->
                _uiState.update { it.copy(lastMaintenanceRun = runs.firstOrNull()) }
            }
        }
        viewModelScope.launch {
            appearancePreferences.observe().collect { prefs ->
                _uiState.update { it.copy(appearance = prefs) }
            }
        }
        viewModelScope.launch {
            installerPreferences.observe().collect { preferred ->
                _uiState.update { it.copy(preferredInstaller = preferred) }
            }
        }
        viewModelScope.launch {
            devPreferences.observe().collect { prefs ->
                _uiState.update { it.copy(devPrefs = prefs) }
                debugLogController.apply(prefs.debugLogEnabled)
            }
        }
        viewModelScope.launch {
            privilegedExecutor.observeStatus().collect { status ->
                _uiState.update {
                    it.copy(
                        shizukuSilentAvailable = PrivilegeCapability.SILENT_INSTALL in status.capabilities,
                    )
                }
            }
        }
    }

    fun onScreenShown(title: String, description: String) {
        _uiState.update { it.copy(title = title, description = description) }
    }

    fun setBackgroundUpdatesEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setEnabled(enabled) }
    }

    fun setAutomationEnabled(enabled: Boolean) {
        viewModelScope.launch { automationPreferences.setEnabled(enabled) }
    }

    fun setMaintenanceEnabled(enabled: Boolean) {
        viewModelScope.launch { maintenancePreferences.setEnabled(enabled) }
    }

    fun setMaintenanceTrimCaches(enabled: Boolean) {
        viewModelScope.launch { maintenancePreferences.setTrimCaches(enabled) }
    }

    fun setMaintenancePruneDownloads(enabled: Boolean) {
        viewModelScope.launch { maintenancePreferences.setPruneDownloads(enabled) }
    }

    fun setMaintenancePruneRollback(enabled: Boolean) {
        viewModelScope.launch { maintenancePreferences.setPruneRollback(enabled) }
    }

    fun setMaintenancePruneBackupTemp(enabled: Boolean) {
        viewModelScope.launch { maintenancePreferences.setPruneBackupTemp(enabled) }
    }

    fun runMaintenanceNow() {
        maintenanceScheduler.enqueueImmediate()
        _uiState.update { it.copy(maintenanceRunQueued = true) }
    }

    fun consumeMaintenanceRunQueued() {
        _uiState.update { it.copy(maintenanceRunQueued = false) }
    }

    fun setUnmeteredOnly(unmeteredOnly: Boolean) {
        viewModelScope.launch { preferences.setUnmeteredOnly(unmeteredOnly) }
    }

    fun setProxyType(type: NetworkProxyType) {
        viewModelScope.launch { networkProxyPreferences.setType(type) }
    }

    fun setProxyHost(host: String) {
        viewModelScope.launch { networkProxyPreferences.setHost(host) }
    }

    fun setProxyPort(port: Int) {
        viewModelScope.launch { networkProxyPreferences.setPort(port) }
    }

    fun setProxyUsername(username: String) {
        viewModelScope.launch { networkProxyPreferences.setUsername(username) }
    }

    fun setProxyPassword(password: String) {
        viewModelScope.launch { networkProxyPreferences.setPassword(password) }
    }

    fun setOrbotPreset(enabled: Boolean) {
        viewModelScope.launch { networkProxyPreferences.setOrbotPreset(enabled) }
    }

    fun onExodusBaseUrlInputChange(value: String) {
        _uiState.update { it.copy(exodusBaseUrlInput = value) }
    }

    fun onExodusTokenInputChange(value: String) {
        _uiState.update { it.copy(exodusTokenInput = value) }
    }

    fun saveExodusApi() {
        viewModelScope.launch {
            val url = _uiState.value.exodusBaseUrlInput.trim()
            if (url.isNotEmpty() && !url.startsWith("https://")) {
                _uiState.update { it.copy(configMessage = "exodus_https_required") }
                return@launch
            }
            exodusPrivacyPreferences.setApiBaseUrl(url)
            val token = _uiState.value.exodusTokenInput
            if (token.isNotBlank()) {
                exodusPrivacyPreferences.setToken(token)
                _uiState.update { it.copy(exodusTokenInput = "") }
            }
            _uiState.update { it.copy(configMessage = "exodus_saved") }
        }
    }

    fun clearExodusToken() {
        viewModelScope.launch {
            exodusPrivacyPreferences.setToken(null)
            _uiState.update { it.copy(exodusTokenInput = "", configMessage = "exodus_token_cleared") }
        }
    }

    fun setAutoInstallMatching(autoInstall: Boolean) {
        viewModelScope.launch { preferences.setAutoInstallMatching(autoInstall) }
    }

    fun setUpdateCooldownDays(days: Int) {
        viewModelScope.launch { preferences.setUpdateCooldownDays(days) }
    }

    fun setRollbackKeepCount(count: Int) {
        viewModelScope.launch {
            preferences.setRollbackKeepCount(count)
            apkRollbackCache.retrimAll(count)
        }
    }

    fun setCheckIntervalHours(hours: Int) {
        viewModelScope.launch { preferences.setCheckIntervalHours(hours) }
    }

    fun onGithubTokenInputChange(value: String) {
        _uiState.update { it.copy(githubTokenInput = value) }
    }

    fun saveGithubToken() {
        val token = _uiState.value.githubTokenInput
        gitHubTokenRepository.saveToken(token)
        _uiState.update {
            it.copy(
                githubTokenInput = "",
                githubTokenSet = gitHubTokenRepository.isTokenSet(),
                configMessage = "github_token_saved",
            )
        }
    }

    fun clearGithubToken() {
        gitHubTokenRepository.clearToken()
        _uiState.update {
            it.copy(
                githubTokenInput = "",
                githubTokenSet = false,
                configMessage = "github_token_cleared",
            )
        }
    }

    fun setTheme(theme: KilnThemeStyle) {
        viewModelScope.launch { appearancePreferences.setTheme(theme) }
    }

    fun setLanguage(language: KilnLanguage) {
        viewModelScope.launch { appearancePreferences.setLanguage(language) }
    }

    fun setFontStyle(fontStyle: KilnFontStyle) {
        viewModelScope.launch { appearancePreferences.setFontStyle(fontStyle) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { appearancePreferences.setDynamicColor(enabled) }
    }

    fun setPreferredInstaller(preferred: PreferredInstaller) {
        viewModelScope.launch { installerPreferences.setPreferred(preferred) }
    }

    fun setDebugLogEnabled(enabled: Boolean) {
        viewModelScope.launch { devPreferences.setDebugLogEnabled(enabled) }
    }

    fun checkNow() {
        scheduler.enqueueImmediate()
        _uiState.update { it.copy(checkNowQueued = true) }
    }

    fun consumeCheckNowQueued() {
        _uiState.update { it.copy(checkNowQueued = false) }
    }

    fun suggestedConfigFileName(): String = KilnConfigFilenames.suggested()

    fun exportConfigToUri(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            _uiState.update { it.copy(configBusy = true, configMessage = null) }
            when (
                val built = kilnConfigRepository.exportJson(
                    versionName = _uiState.value.appVersionName,
                    versionCode = _uiState.value.appVersionCode,
                )
            ) {
                is Result.Failure -> {
                    deleteUriQuietly(contentResolver, uri)
                    _uiState.update {
                        it.copy(configBusy = false, configMessage = "config_export_failed:${built.message}")
                    }
                }

                is Result.Success -> {
                    val write = withContext(ioDispatcher) {
                        runCatching {
                            contentResolver.openOutputStream(uri)?.use { out ->
                                out.write(built.value.toByteArray(StandardCharsets.UTF_8))
                            } ?: error("openOutputStream_null")
                        }
                    }
                    if (write.isFailure) {
                        deleteUriQuietly(contentResolver, uri)
                        Timber.w(write.exceptionOrNull(), "Kiln-Config export write failed")
                        _uiState.update {
                            it.copy(configBusy = false, configMessage = "config_export_write_failed")
                        }
                    } else {
                        _uiState.update {
                            it.copy(configBusy = false, configMessage = "config_export_ok")
                        }
                    }
                }
            }
        }
    }

    fun previewImportFromUri(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            _uiState.update { it.copy(configBusy = true, configMessage = null) }
            val text = withContext(ioDispatcher) {
                runCatching {
                    contentResolver.openInputStream(uri)?.use { input ->
                        input.readBytes().toString(StandardCharsets.UTF_8)
                    }
                }.getOrNull()
            }
            if (text.isNullOrBlank()) {
                _uiState.update {
                    it.copy(configBusy = false, configMessage = "config_import_read_failed")
                }
                return@launch
            }
            when (val preview = kilnConfigRepository.previewImport(text)) {
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(configBusy = false, configMessage = "config_import_failed:${preview.message}")
                    }
                }

                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            configBusy = false,
                            pendingImportPreview = preview.value,
                            pendingImportJson = text,
                        )
                    }
                }
            }
        }
    }

    fun confirmPendingImport() {
        val json = _uiState.value.pendingImportJson ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    configBusy = true,
                    pendingImportPreview = null,
                    pendingImportJson = null,
                    configMessage = null,
                )
            }
            when (val result = kilnConfigRepository.importMerge(json)) {
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(configBusy = false, configMessage = "config_import_failed:${result.message}")
                    }
                }

                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            configBusy = false,
                            configMessage = "config_import_ok:${result.value.reposUpserted}:" +
                                "${result.value.managedAppsUpserted}:${result.value.prefsSectionsApplied}",
                        )
                    }
                }
            }
        }
    }

    fun dismissImportPreview() {
        _uiState.update {
            it.copy(pendingImportPreview = null, pendingImportJson = null)
        }
    }

    fun consumeConfigMessage() {
        _uiState.update { it.copy(configMessage = null) }
    }

    private fun deleteUriQuietly(contentResolver: ContentResolver, uri: Uri) {
        runCatching {
            contentResolver.delete(uri, null, null)
        }.onFailure { error ->
            if (error !is IOException) {
                Timber.w(error, "Could not delete failed Kiln-Config export URI")
            }
        }
    }

    private fun readVersionName(): String = try {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+: typed flags overload; PackageInfoFlags required.
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        info.versionName.orEmpty()
    } catch (_: PackageManager.NameNotFoundException) {
        ""
    }

    private fun readVersionCode(): Long = try {
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        // API 28+: longVersionCode; older devices use deprecated versionCode.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
    } catch (_: PackageManager.NameNotFoundException) {
        0L
    }
}
