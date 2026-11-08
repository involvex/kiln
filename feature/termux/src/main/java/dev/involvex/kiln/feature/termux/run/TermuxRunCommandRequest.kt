package dev.involvex.kiln.feature.termux.run

/**
 * Immutable description of a command to run via Termux RUN_COMMAND.
 *
 * [commandPath] should be absolute under Termux `$PREFIX`, or `$PREFIX/…` / `~/…`
 * as accepted by Termux ≥ documented wiki versions.
 */
data class TermuxRunCommandRequest(
    val packageId: String,
    val commandPath: String,
    val arguments: List<String> = emptyList(),
    val workdir: String? = null,
    val background: Boolean = true,
    val sessionAction: String? = null,
    val label: String? = null,
    val executionId: Int,
)
