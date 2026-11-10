package dev.involvex.kiln.feature.apps.apktools

import dev.involvex.kiln.core.model.ApkInspectReport
import dev.involvex.kiln.core.model.ApkSignatureCompareReport

enum class ApkToolsMode {
    INSPECT,
    COMPARE,
}

data class ApkToolsUiState(
    val mode: ApkToolsMode = ApkToolsMode.INSPECT,
    val isBusy: Boolean = false,
    val inspect: ApkInspectReport? = null,
    val compare: ApkSignatureCompareReport? = null,
    val comparePackageQuery: String = "",
    val message: String? = null,
    val messageIsError: Boolean = false,
)
