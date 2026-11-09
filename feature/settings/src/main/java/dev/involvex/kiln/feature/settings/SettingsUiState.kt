package dev.involvex.kiln.feature.settings

import dev.involvex.kiln.core.model.AppearancePrefs
import dev.involvex.kiln.core.model.AutomationPrefs
import dev.involvex.kiln.core.model.BackgroundUpdatePrefs
import dev.involvex.kiln.core.model.DevPrefs
import dev.involvex.kiln.core.model.KilnConfigImportPreview
import dev.involvex.kiln.core.model.MaintenancePrefs
import dev.involvex.kiln.core.model.MaintenanceRunRecord
import dev.involvex.kiln.core.model.PreferredInstaller

data class SettingsUiState(
    val title: String = "",
    val description: String = "",
    val backgroundUpdates: BackgroundUpdatePrefs = BackgroundUpdatePrefs(),
    val automation: AutomationPrefs = AutomationPrefs(),
    val maintenance: MaintenancePrefs = MaintenancePrefs(),
    val lastMaintenanceRun: MaintenanceRunRecord? = null,
    val appearance: AppearancePrefs = AppearancePrefs(),
    val preferredInstaller: PreferredInstaller = PreferredInstaller.SYSTEM,
    val shizukuSilentAvailable: Boolean = false,
    val devPrefs: DevPrefs = DevPrefs(),
    val checkNowQueued: Boolean = false,
    val maintenanceRunQueued: Boolean = false,
    val appVersionName: String = "",
    val appVersionCode: Long = 0L,
    val configBusy: Boolean = false,
    val configMessage: String? = null,
    val pendingImportPreview: KilnConfigImportPreview? = null,
    val pendingImportJson: String? = null,
)
