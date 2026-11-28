package dev.involvex.kiln.core.privilege.root

import com.topjohnwu.superuser.Shell
import dev.involvex.kiln.core.common.IoDispatcher
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.model.RootPreferences
import dev.involvex.kiln.core.privilege.shell.ShellAllowlist
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Magisk / libsu path for allowlisted shell when the user opts in (PLAN §5).
 * Never concatenates free-text user input — argv comes only from [ShellAllowlist].
 */
@Singleton
class RootExecutor @Inject constructor(
    private val rootPreferences: RootPreferences,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    init {
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(SHELL_TIMEOUT_SECONDS),
        )
    }

    fun isOptInEnabled(): Boolean = rootPreferences.isEnabled()

    /**
     * Cached Magisk grant without prompting. Null/false → treat as not granted.
     */
    fun isGrantedCached(): Boolean = runCatching {
        Shell.isAppGrantedRoot() == true
    }.getOrDefault(false)

    fun canExecute(): Boolean = isOptInEnabled() && isGrantedCached()

    /**
     * Requests a root shell when opt-in is on (may show Magisk prompt). Runs on [ioDispatcher].
     */
    suspend fun ensureRoot(): Boolean = withContext(ioDispatcher) {
        if (!rootPreferences.isEnabled()) return@withContext false
        runCatching {
            val shell = Shell.getShell()
            shell.isRoot
        }.onFailure { Timber.w(it, "Root shell request failed") }
            .getOrDefault(false)
    }

    /**
     * Runs an allowlisted command via libsu. Tokens are validated (no spaces / metacharacters),
     * so joining into one libsu command line does not introduce shell injection.
     */
    suspend fun runAllowlisted(commandId: String, args: List<String>): Result<String> = withContext(ioDispatcher) {
        if (!rootPreferences.isEnabled()) {
            return@withContext Result.failure("root_opt_in_disabled")
        }
        val argv = ShellAllowlist.buildArgv(commandId, args)
            ?: return@withContext Result.failure("command_rejected")
        if (!ensureRoot()) {
            return@withContext Result.failure("root_not_granted")
        }
        val line = argv.joinToString(" ")
        val exec = Shell.cmd(line).exec()
        if (exec.isSuccess) {
            Result.success(exec.out.joinToString("\n").ifBlank { "OK" })
        } else {
            val detail = (exec.err + exec.out).joinToString("\n").trim()
            Result.success("ERR:exit=${exec.code}:$detail")
        }
    }

    private companion object {
        const val SHELL_TIMEOUT_SECONDS = 120L
    }
}
