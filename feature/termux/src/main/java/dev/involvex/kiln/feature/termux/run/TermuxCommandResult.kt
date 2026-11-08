package dev.involvex.kiln.feature.termux.run

import android.content.Intent
import android.os.Bundle

data class TermuxCommandResult(
    val executionId: Int,
    val stdout: String,
    val stderr: String,
    val exitCode: Int?,
    val err: Int?,
    val errmsg: String,
    val stdoutOriginalLength: String?,
    val stderrOriginalLength: String?,
) {
    val isPluginOk: Boolean
        get() = err == null || err == -1 // Activity.RESULT_OK

    companion object {
        fun fromIntent(intent: Intent): TermuxCommandResult? {
            val executionId = intent.getIntExtra(TermuxRunCommandApi.EXTRA_EXECUTION_ID, -1)
            if (executionId < 0) return null
            val bundle: Bundle = intent.getBundleExtra(TermuxRunCommandApi.EXTRA_PLUGIN_RESULT_BUNDLE)
                ?: return TermuxCommandResult(
                    executionId = executionId,
                    stdout = "",
                    stderr = "",
                    exitCode = null,
                    err = null,
                    errmsg = "missing_result_bundle",
                    stdoutOriginalLength = null,
                    stderrOriginalLength = null,
                )
            return TermuxCommandResult(
                executionId = executionId,
                stdout = bundle.getString(TermuxRunCommandApi.EXTRA_STDOUT).orEmpty(),
                stderr = bundle.getString(TermuxRunCommandApi.EXTRA_STDERR).orEmpty(),
                exitCode = if (bundle.containsKey(TermuxRunCommandApi.EXTRA_EXIT_CODE)) {
                    bundle.getInt(TermuxRunCommandApi.EXTRA_EXIT_CODE)
                } else {
                    null
                },
                err = if (bundle.containsKey(TermuxRunCommandApi.EXTRA_ERR)) {
                    bundle.getInt(TermuxRunCommandApi.EXTRA_ERR)
                } else {
                    null
                },
                errmsg = bundle.getString(TermuxRunCommandApi.EXTRA_ERRMSG).orEmpty(),
                stdoutOriginalLength = bundle.getString(TermuxRunCommandApi.EXTRA_STDOUT_ORIGINAL_LENGTH),
                stderrOriginalLength = bundle.getString(TermuxRunCommandApi.EXTRA_STDERR_ORIGINAL_LENGTH),
            )
        }
    }
}
