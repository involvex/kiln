package dev.involvex.kiln.feature.termux.run

import android.app.PendingIntent
import android.content.Intent

/**
 * Package-scoped extras for a RUN_COMMAND Intent, testable without Android.
 */
data class TermuxRunCommandSpec(
    val packageId: String,
    val action: String,
    val serviceClassName: String,
    val commandPath: String,
    val arguments: List<String>,
    val workdir: String,
    val background: Boolean,
    val sessionAction: String?,
    val label: String?,
)

/**
 * Builds a Termux RUN_COMMAND service Intent from a request.
 * Pure key/extra assembly — no Termux library dependency.
 */
object TermuxRunCommandIntentBuilder {
    fun spec(request: TermuxRunCommandRequest): TermuxRunCommandSpec {
        require(request.packageId.isNotBlank()) { "packageId must not be blank" }
        require(request.commandPath.isNotBlank()) { "commandPath must not be blank" }
        val packageId = request.packageId.trim()
        return TermuxRunCommandSpec(
            packageId = packageId,
            action = TermuxRunCommandApi.action(packageId),
            serviceClassName = TermuxRunCommandApi.serviceClassName(packageId),
            commandPath = request.commandPath,
            arguments = request.arguments,
            workdir = request.workdir ?: TermuxPaths.homeDir(packageId),
            background = request.background,
            sessionAction = request.sessionAction,
            label = request.label,
        )
    }

    fun build(request: TermuxRunCommandRequest, resultPendingIntent: PendingIntent?): Intent {
        val described = spec(request)
        val packageId = described.packageId
        return Intent().apply {
            setClassName(packageId, described.serviceClassName)
            action = described.action
            putExtra(TermuxRunCommandApi.extraCommandPath(packageId), described.commandPath)
            if (described.arguments.isNotEmpty()) {
                putExtra(
                    TermuxRunCommandApi.extraArguments(packageId),
                    described.arguments.toTypedArray(),
                )
            }
            putExtra(TermuxRunCommandApi.extraWorkdir(packageId), described.workdir)
            putExtra(TermuxRunCommandApi.extraBackground(packageId), described.background)
            described.sessionAction?.let {
                putExtra(TermuxRunCommandApi.extraSessionAction(packageId), it)
            }
            described.label?.let {
                putExtra(TermuxRunCommandApi.extraCommandLabel(packageId), it)
            }
            if (resultPendingIntent != null) {
                putExtra(TermuxRunCommandApi.extraPendingIntent(packageId), resultPendingIntent)
            }
        }
    }

    /** Smoke-test: `echo kiln-termux-ok` in Termux `$PREFIX/bin/echo`. */
    fun echoSmokeTest(packageId: String, executionId: Int): TermuxRunCommandRequest = TermuxRunCommandRequest(
        packageId = packageId,
        commandPath = TermuxPaths.executable(packageId, "echo"),
        arguments = listOf("kiln-termux-ok"),
        workdir = TermuxPaths.homeDir(packageId),
        background = true,
        label = "Kiln Termux smoke test",
        executionId = executionId,
    )
}
