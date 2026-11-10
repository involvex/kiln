package dev.involvex.kiln.core.model

/** Kind of file the user picked for APK tools. */
enum class ApkArchiveKind {
    APK,
    APKS,
    XAPK,
    UNKNOWN,
}

data class ApkInspectReport(
    val displayName: String,
    val kind: ApkArchiveKind,
    val packageName: String?,
    val versionName: String?,
    val versionCode: Long?,
    val minSdk: Int?,
    val targetSdk: Int?,
    val permissions: List<String>,
    val signerSha256: List<String>,
    val members: List<String>,
    val baseApkMember: String?,
    val errorCode: String? = null,
)

data class ApkSignatureCompareReport(
    val leftLabel: String,
    val rightLabel: String,
    val leftSigners: List<String>,
    val rightSigners: List<String>,
    val matches: Boolean,
    val errorCode: String? = null,
)
