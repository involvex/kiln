package dev.involvex.kiln.feature.installer.session

import android.content.pm.PackageInstaller

object InstallStatusMapper {
    fun messageKey(status: Int): String = when (status) {
        PackageInstaller.STATUS_SUCCESS -> "install_success"
        PackageInstaller.STATUS_PENDING_USER_ACTION -> "install_pending_user_action"
        PackageInstaller.STATUS_FAILURE_ABORTED -> "install_failure_aborted"
        PackageInstaller.STATUS_FAILURE_BLOCKED -> "install_failure_blocked"
        PackageInstaller.STATUS_FAILURE_CONFLICT -> "install_failure_conflict"
        PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> "install_failure_incompatible"
        PackageInstaller.STATUS_FAILURE_INVALID -> "install_failure_invalid"
        PackageInstaller.STATUS_FAILURE_STORAGE -> "install_failure_storage"
        PackageInstaller.STATUS_FAILURE -> "install_failure"
        else -> "install_failure_unknown"
    }

    fun isTerminalFailure(status: Int): Boolean = status != PackageInstaller.STATUS_SUCCESS &&
        status != PackageInstaller.STATUS_PENDING_USER_ACTION
}
