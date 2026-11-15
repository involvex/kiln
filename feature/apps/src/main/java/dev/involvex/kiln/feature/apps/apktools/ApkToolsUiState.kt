package dev.involvex.kiln.feature.apps.apktools

import android.net.Uri
import dev.involvex.kiln.core.model.ApkInspectReport
import dev.involvex.kiln.core.model.ApkSignatureCompareReport

enum class ApkToolsMode {
    INSPECT,
    COMPARE,
    RESIGN,
    EXPORT_SPLITS,
}

data class ApkToolsUiState(
    val mode: ApkToolsMode = ApkToolsMode.INSPECT,
    val isBusy: Boolean = false,
    val inspect: ApkInspectReport? = null,
    val compare: ApkSignatureCompareReport? = null,
    val comparePackageQuery: String = "",
    val resignApkUri: Uri? = null,
    val resignApkLabel: String? = null,
    val resignKeystoreUri: Uri? = null,
    val resignKeystoreLabel: String? = null,
    val resignAlias: String = "",
    val resignConfirmed: Boolean = false,
    val exportCount: Int? = null,
    val message: String? = null,
    val messageIsError: Boolean = false,
)
