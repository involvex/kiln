package dev.involvex.kiln.feature.debloat.denylist

/**
 * Hard denylist packages that must never be selectable for debloat actions (PLAN §4.10).
 * Dynamic roles (launcher/IME/dialer/SMS) are merged by [DebloatDenylistResolver].
 */
object DebloatDenylist {
    const val REASON_HARD = "hard_denylist"
    const val REASON_SELF = "kiln_self"
    const val REASON_LAUNCHER = "default_launcher"
    const val REASON_IME = "default_ime"
    const val REASON_DIALER = "default_dialer"
    const val REASON_SMS = "default_sms"

    val HARD_PACKAGES: Set<String> = setOf(
        "moe.shizuku.privileged.api",
        "af.shizuku.plus.api",
        "com.android.systemui",
        "com.android.settings",
        "com.android.phone",
        "com.android.server.telecom",
        "com.android.telephony",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.android.webview",
        "com.google.android.webview",
        "com.android.providers.settings",
        "android",
    )

    fun isHardBlocked(packageName: String): Boolean = packageName in HARD_PACKAGES
}
