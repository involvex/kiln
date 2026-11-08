package dev.involvex.kiln.feature.installer

import dev.involvex.kiln.core.model.PreferredInstaller

/** Pure policy: whether InstallWorker should attempt silent Shizuku install. */
object InstallCommitPolicy {
    fun useSilentInstall(preferred: PreferredInstaller, hasSilentCapability: Boolean): Boolean =
        preferred == PreferredInstaller.SHIZUKU && hasSilentCapability
}
