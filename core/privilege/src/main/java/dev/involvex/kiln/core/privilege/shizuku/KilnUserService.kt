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
        return when (id) {
            // Full dumpsys meminfo can take minutes and ANR the app over Binder; snapshot only.
            ShellAllowlist.DUMPSYS_MEMINFO -> ShellRunner.runMeminfoSnapshot(argv)

            ShellAllowlist.DUMPSYS_BATTERY -> ShellRunner.runBatterySnapshot(argv)

            // Full dumpsys netstats can be multi-MB; truncate in-process.
            ShellAllowlist.DUMPSYS_NETSTATS -> ShellRunner.runNetstatsSnapshot(argv)

            // cpuinfo is heavy; always bound (do not use unbounded ShellRunner.run).
            ShellAllowlist.DUMPSYS_CPUINFO -> ShellRunner.runCpuinfoSnapshot(argv)

            ShellAllowlist.DUMPSYS_THERMALSERVICE -> ShellRunner.runThermalSnapshot(argv)

            ShellAllowlist.TOP_ONCE -> ShellRunner.runTopSnapshot(argv)

            else -> ShellRunner.run(argv)
        }
    }
}
