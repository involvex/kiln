package dev.involvex.kiln.feature.termux.run

/**
 * Public RUN_COMMAND Intent contract from the Termux wiki / documented API
 * (https://github.com/termux/termux-app/wiki/RUN-COMMAND-Intent).
 *
 * Values mirror TermuxConstants for a given [packageId] (= TERMUX_PACKAGE_NAME).
 * Hardcoded from the public wiki examples — no Termux source is copied.
 */
object TermuxRunCommandApi {
    const val SERVICE_CLASS_SUFFIX: String = ".app.RunCommandService"

    fun action(packageId: String): String = "$packageId.RUN_COMMAND"

    fun serviceClassName(packageId: String): String = "$packageId$SERVICE_CLASS_SUFFIX"

    fun permission(packageId: String): String = "$packageId.permission.RUN_COMMAND"

    fun extraCommandPath(packageId: String): String = "$packageId.RUN_COMMAND_PATH"

    fun extraArguments(packageId: String): String = "$packageId.RUN_COMMAND_ARGUMENTS"

    fun extraWorkdir(packageId: String): String = "$packageId.RUN_COMMAND_WORKDIR"

    fun extraBackground(packageId: String): String = "$packageId.RUN_COMMAND_BACKGROUND"

    fun extraSessionAction(packageId: String): String = "$packageId.RUN_COMMAND_SESSION_ACTION"

    fun extraCommandLabel(packageId: String): String = "$packageId.RUN_COMMAND_COMMAND_LABEL"

    fun extraPendingIntent(packageId: String): String = "$packageId.RUN_COMMAND_PENDING_INTENT"

    /** Result bundle key inside the PendingIntent extras (not package-prefixed). */
    const val EXTRA_PLUGIN_RESULT_BUNDLE: String = "result"

    const val EXTRA_STDOUT: String = "stdout"
    const val EXTRA_STDERR: String = "stderr"
    const val EXTRA_EXIT_CODE: String = "exitCode"
    const val EXTRA_ERR: String = "err"
    const val EXTRA_ERRMSG: String = "errmsg"
    const val EXTRA_STDOUT_ORIGINAL_LENGTH: String = "stdout_original_length"
    const val EXTRA_STDERR_ORIGINAL_LENGTH: String = "stderr_original_length"

    /** Kiln-owned extra on the result PendingIntent to correlate runs. */
    const val EXTRA_EXECUTION_ID: String = "dev.involvex.kiln.termux.EXECUTION_ID"
}
