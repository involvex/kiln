package dev.involvex.kiln.feature.backup

import dev.involvex.kiln.core.model.AndroidUserInfo
import dev.involvex.kiln.core.model.BackupAppCandidate
import dev.involvex.kiln.core.model.BackupContentTier
import dev.involvex.kiln.core.model.BackupDestinationKind
import dev.involvex.kiln.core.model.BackupPrefs
import dev.involvex.kiln.core.model.BackupTreeEntry

data class BackupUiState(
    val tier: BackupContentTier = BackupContentTier.NONE,
    val prefs: BackupPrefs = BackupPrefs(),
    val treeLabel: String? = null,
    val isLoading: Boolean = false,
    val apps: List<BackupAppCandidate> = emptyList(),
    val query: String = "",
    val selectedPackage: String? = null,
    val busy: Boolean = false,
    val message: String? = null,
    val pendingBackupPackage: String? = null,
    val pendingRestore: Boolean = false,
    val pendingBackupUserConfirm: Boolean = false,
    /** Non-null → UI should launch CreateDocument with this suggested name. */
    val pendingCreateDocumentName: String? = null,
    val confirmUsers: List<AndroidUserInfo> = emptyList(),
    val selectedUserId: Int = 0,
    val passwordPrompt: BackupPasswordPrompt? = null,
    val encryptEnabled: Boolean = false,
    val passwordDraft: String = "",
    val webdavPasswordSet: Boolean = false,
    val webdavUrlDraft: String = "",
    val webdavUserDraft: String = "",
    val webdavPathDraft: String = BackupPrefs.DEFAULT_WEBDAV_PATH,
    val webdavPasswordDraft: String = "",
    val remoteArchives: List<BackupTreeEntry> = emptyList(),
    val showRemotePicker: Boolean = false,
) {
    val filteredApps: List<BackupAppCandidate>
        get() {
            val q = query.trim()
            if (q.isEmpty()) return apps
            return apps.filter {
                it.label.contains(q, ignoreCase = true) ||
                    it.packageName.contains(q, ignoreCase = true)
            }
        }

    val selectedApp: BackupAppCandidate?
        get() = apps.find { it.packageName == selectedPackage }

    val hasTree: Boolean get() = !prefs.treeUri.isNullOrBlank()

    val usesWebDav: Boolean get() = prefs.destinationKind == BackupDestinationKind.WEBDAV

    val webDavReady: Boolean
        get() = usesWebDav &&
            webdavUrlDraft.startsWith("https://", ignoreCase = true) &&
            webdavUserDraft.isNotBlank() &&
            (webdavPasswordSet || webdavPasswordDraft.isNotEmpty())
}

enum class BackupPasswordPrompt {
    BACKUP,
    RESTORE,
}
