package dev.involvex.kiln.core.privilege.shizuku

import android.content.Context
import androidx.annotation.Keep
import dev.involvex.kiln.core.privilege.IKilnUserService
import dev.involvex.kiln.core.privilege.shell.ShellAllowlist
import dev.involvex.kiln.core.privilege.shell.ShellRunner
import timber.log.Timber

/**
 * Shizuku UserService — runs in a privileged process as shell (uid 2000) or root (uid 0).
 * Must keep a no-arg constructor; Context constructor is preferred on API 13+.
 */
@Keep
class KilnUserService : IKilnUserService.Stub {
    @Keep
    constructor()

    @Keep
    constructor(@Suppress("UNUSED_PARAMETER") context: Context)

    override fun destroy() {
        System.exit(0)
    }

    override fun runAllowlisted(commandId: String?, args: Array<out String>?): String {
        val id = commandId ?: return "ERR:missing_command"
        val argList = args?.toList().orEmpty()
        val argv = ShellAllowlist.buildArgv(id, argList)
            ?: return "ERR:rejected:$id"
        Timber.d("UserService allowlisted %s argc=%d", id, argv.size)
        return ShellRunner.run(argv)
    }
}
