package dev.involvex.kiln.automation

import android.content.Intent
import dev.involvex.kiln.core.model.AutomationApi

sealed interface AutomationCommand {
    data object CheckUpdates : AutomationCommand

    data class InstallUpdate(val packageName: String) : AutomationCommand

    data class InstallFromUrl(
        val packageName: String,
        val url: String,
        val versionName: String,
        val versionCode: Long,
        val sha256: String?,
    ) : AutomationCommand

    data class CleanCache(val packageName: String?) : AutomationCommand
}

/** Bundle of Intent extras without Android framework coupling (unit-testable). */
data class AutomationExtras(
    val packageName: String? = null,
    val url: String? = null,
    val versionName: String? = null,
    val versionCode: Long = 0L,
    val sha256: String? = null,
)

object AutomationCommandParser {
    fun parse(intent: Intent): Result<AutomationCommand> = parse(
        action = intent.action,
        extras = AutomationExtras(
            packageName = intent.getStringExtra(AutomationApi.EXTRA_PACKAGE_NAME),
            url = intent.getStringExtra(AutomationApi.EXTRA_URL),
            versionName = intent.getStringExtra(AutomationApi.EXTRA_VERSION_NAME),
            versionCode = intent.getLongExtra(AutomationApi.EXTRA_VERSION_CODE, 0L),
            sha256 = intent.getStringExtra(AutomationApi.EXTRA_SHA256),
        ),
    )

    fun parse(action: String?, extras: AutomationExtras): Result<AutomationCommand> = when (action) {
        AutomationApi.ACTION_CHECK_UPDATES -> Result.success(AutomationCommand.CheckUpdates)

        AutomationApi.ACTION_INSTALL_UPDATE -> {
            val pkg = extras.packageName?.trim().orEmpty()
            if (pkg.isEmpty()) {
                Result.failure(IllegalArgumentException("missing_package"))
            } else {
                Result.success(AutomationCommand.InstallUpdate(pkg))
            }
        }

        AutomationApi.ACTION_INSTALL_FROM_URL -> parseInstallFromUrl(extras)

        AutomationApi.ACTION_CLEAN_CACHE -> {
            val pkg = extras.packageName?.trim()?.takeIf { it.isNotEmpty() }
            Result.success(AutomationCommand.CleanCache(pkg))
        }

        else -> Result.failure(IllegalArgumentException("unknown_action"))
    }

    private fun parseInstallFromUrl(extras: AutomationExtras): Result<AutomationCommand> {
        val pkg = extras.packageName?.trim().orEmpty()
        val url = extras.url?.trim().orEmpty()
        if (pkg.isEmpty()) return Result.failure(IllegalArgumentException("missing_package"))
        if (!isHttpsUrl(url)) return Result.failure(IllegalArgumentException("url_must_be_https"))
        val versionName = extras.versionName?.trim().orEmpty().ifBlank { "unknown" }
        val versionCode = extras.versionCode.coerceAtLeast(0L)
        val sha256 = extras.sha256?.trim()?.takeIf { it.isNotEmpty() }
        return Result.success(
            AutomationCommand.InstallFromUrl(
                packageName = pkg,
                url = url,
                versionName = versionName,
                versionCode = versionCode,
                sha256 = sha256,
            ),
        )
    }

    /** HTTPS-only gate without Android Uri stubs (unit-test friendly). */
    fun isHttpsUrl(raw: String): Boolean {
        if (!raw.startsWith("https://", ignoreCase = true)) return false
        val afterScheme = raw.substring(8)
        if (afterScheme.isBlank() || afterScheme.startsWith("/")) return false
        val host = afterScheme.substringBefore('/').substringBefore('?').substringBefore('#')
        return host.isNotBlank() && host.contains('.')
    }
}
