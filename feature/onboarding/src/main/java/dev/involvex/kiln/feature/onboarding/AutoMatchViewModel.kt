package dev.involvex.kiln.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.involvex.kiln.core.common.IoDispatcher
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.database.IndexDao
import dev.involvex.kiln.core.database.ManagedAppDao
import dev.involvex.kiln.core.database.ManagedAppEntity
import dev.involvex.kiln.core.database.RepoDao
import dev.involvex.kiln.core.model.AppUpdateRepository
import dev.involvex.kiln.core.model.InstalledAppsReader
import dev.involvex.kiln.core.model.MatchConfidence
import dev.involvex.kiln.feature.onboarding.github.GitHubMap
import dev.involvex.kiln.feature.onboarding.match.AutoMatchConfidenceFilter
import dev.involvex.kiln.feature.onboarding.match.AutoMatchEngine
import dev.involvex.kiln.feature.onboarding.match.MatchCandidate
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LinkableRepo(val id: String, val name: String)

data class AutoMatchUiState(
    val isLoading: Boolean = true,
    val high: List<MatchCandidate> = emptyList(),
    val medium: List<MatchCandidate> = emptyList(),
    val warning: List<MatchCandidate> = emptyList(),
    val none: List<MatchCandidate> = emptyList(),
    val selected: Set<String> = emptySet(),
    val indexEmpty: Boolean = false,
    val enabledRepos: List<LinkableRepo> = emptyList(),
    /** Distinct GitHub owner/repo values already saved (for one-tap link chips). */
    val savedGithubRepos: List<String> = emptyList(),
    /** One-shot snackbar count after a successful link; UI clears via [AutoMatchViewModel.consumeSnackbar]. */
    val snackbarLinkedCount: Int? = null,
    /** One-shot snackbar after stop-tracking. */
    val snackbarUnlinked: Boolean = false,
    /** String-resource key: `bad_github` | `link_failed` | `unlink_failed` | raw message. */
    val messageErrorKey: String? = null,
    val linkTarget: MatchCandidate? = null,
    val warningConfirmTarget: MatchCandidate? = null,
    val unlinkConfirmTarget: MatchCandidate? = null,
    val searchQuery: String = "",
    val confidenceFilter: AutoMatchConfidenceFilter = AutoMatchConfidenceFilter.ALL,
    val showLinked: Boolean = false,
)

@HiltViewModel
@Suppress("TooManyFunctions") // Match hub: filters, accept/link/unlink, and rematch share one VM.
class AutoMatchViewModel @Inject constructor(
    private val installedAppsReader: InstalledAppsReader,
    private val indexDao: IndexDao,
    private val repoDao: RepoDao,
    private val managedAppDao: ManagedAppDao,
    private val githubMap: GitHubMap,
    private val appUpdateRepository: AppUpdateRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AutoMatchUiState())
    val uiState: StateFlow<AutoMatchUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { rematch(announceCount = null) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setConfidenceFilter(filter: AutoMatchConfidenceFilter) {
        _uiState.update { it.copy(confidenceFilter = filter) }
    }

    fun setShowLinked(show: Boolean) {
        _uiState.update { it.copy(showLinked = show) }
    }

    fun consumeSnackbar() {
        _uiState.update { it.copy(snackbarLinkedCount = null, snackbarUnlinked = false) }
    }

    fun toggle(packageName: String) {
        _uiState.update { state ->
            val next = state.selected.toMutableSet()
            if (!next.add(packageName)) next.remove(packageName)
            state.copy(selected = next)
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(messageErrorKey = null) }
    }

    fun openLinkDialog(candidate: MatchCandidate) {
        _uiState.update { it.copy(linkTarget = candidate) }
    }

    fun dismissLinkDialog() {
        _uiState.update { it.copy(linkTarget = null) }
    }

    fun requestWarningAccept(candidate: MatchCandidate) {
        _uiState.update { it.copy(warningConfirmTarget = candidate) }
    }

    fun dismissWarningConfirm() {
        _uiState.update { it.copy(warningConfirmTarget = null) }
    }

    fun requestUnlink(candidate: MatchCandidate) {
        _uiState.update { it.copy(unlinkConfirmTarget = candidate) }
    }

    fun dismissUnlinkConfirm() {
        _uiState.update { it.copy(unlinkConfirmTarget = null) }
    }

    fun confirmUnlink() {
        val target = _uiState.value.unlinkConfirmTarget ?: return
        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                appUpdateRepository.unlinkManagedPackage(target.packageName)
            }
            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            unlinkConfirmTarget = null,
                            messageErrorKey = null,
                            snackbarUnlinked = true,
                        )
                    }
                    rematch(announceCount = null)
                }

                is Result.Failure -> {
                    _uiState.update {
                        it.copy(
                            unlinkConfirmTarget = null,
                            messageErrorKey = result.message.ifBlank { "unlink_failed" },
                        )
                    }
                }
            }
        }
    }

    fun confirmWarningAccept() {
        val target = _uiState.value.warningConfirmTarget ?: return
        viewModelScope.launch {
            val count = persist(listOf(target), forcePinned = true)
            _uiState.update { it.copy(warningConfirmTarget = null) }
            rematch(announceCount = count)
        }
    }

    fun linkToFdroidRepo(packageName: String, repoId: String) {
        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                appUpdateRepository.linkManagedSource(
                    packageName = packageName,
                    sourceId = repoId,
                    confidence = MatchConfidence.MEDIUM,
                    pinned = true,
                )
            }
            finishLink(result)
        }
    }

    fun linkToGithub(packageName: String, ownerRepo: String) {
        val normalized = ownerRepo.trim().trim('/')
        if (!normalized.contains('/') || normalized.count { it == '/' } != 1) {
            _uiState.update { it.copy(messageErrorKey = "bad_github") }
            return
        }
        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                appUpdateRepository.linkManagedSource(
                    packageName = packageName,
                    sourceId = "github:$normalized",
                    confidence = MatchConfidence.MEDIUM,
                    githubOwnerRepo = normalized,
                    githubAssetRegex = ".*\\.apk$",
                    pinned = true,
                )
            }
            finishLink(result)
        }
    }

    fun acceptSelected() {
        viewModelScope.launch {
            val state = _uiState.value
            val chosen = (state.high + state.medium).filter {
                it.packageName in state.selected && !it.alreadyLinked
            }
            val count = persist(chosen, forcePinned = false)
            rematch(announceCount = count)
        }
    }

    fun acceptAllSafe() {
        val highPending = _uiState.value.high.filter { !it.alreadyLinked }.map { it.packageName }.toSet()
        _uiState.update { it.copy(selected = highPending) }
        acceptSelected()
    }

    /**
     * @return number of apps written, or 0 when [chosen] was empty.
     */
    private suspend fun persist(chosen: List<MatchCandidate>, forcePinned: Boolean): Int {
        if (chosen.isEmpty()) return 0
        withContext(ioDispatcher) {
            managedAppDao.upsertAll(
                chosen.map {
                    ManagedAppEntity(
                        packageName = it.packageName,
                        sourceId = it.repoId.ifBlank { "manual" },
                        confidence = it.confidence.name,
                        pinned = forcePinned || it.confidence == MatchConfidence.WARNING,
                        ignoreUpdates = false,
                        githubOwnerRepo = it.githubOwnerRepo,
                        githubAssetRegex = it.githubAssetRegex,
                        githubChannel = "STABLE",
                    )
                },
            )
            appUpdateRepository.refreshUpdates()
        }
        _uiState.update {
            it.copy(
                messageErrorKey = null,
                linkTarget = null,
            )
        }
        return chosen.size
    }

    private suspend fun finishLink(result: Result<Unit>) {
        when (result) {
            is Result.Success -> {
                _uiState.update {
                    it.copy(
                        linkTarget = null,
                        messageErrorKey = null,
                    )
                }
                rematch(announceCount = 1)
            }

            is Result.Failure -> {
                _uiState.update {
                    it.copy(messageErrorKey = result.message.ifBlank { "link_failed" })
                }
            }
        }
    }

    private suspend fun rematch(announceCount: Int?) {
        _uiState.update {
            it.copy(
                isLoading = true,
                messageErrorKey = if (announceCount != null) null else it.messageErrorKey,
            )
        }
        val snapshot = withContext(ioDispatcher) {
            val installed = installedAppsReader.userApps()
            val indexed = indexDao.allPackages()
            val repos = repoDao.getAll()
            val managedRows = managedAppDao.getAll()
            val managedByPackage = managedRows.associateBy { it.packageName }
            val repoNames = repos.associate { it.id to it.name }
            val matches = AutoMatchEngine.match(
                installed = installed,
                indexed = indexed,
                repoPriority = repos.associate { it.id to it.priority },
                repoNames = repoNames,
                githubMap = githubMap,
            ).map { candidate ->
                val managed = managedByPackage[candidate.packageName]
                if (managed == null) {
                    candidate
                } else {
                    candidate.copy(
                        alreadyLinked = true,
                        trackedSourceLabel = trackedLabelFor(managed, repoNames),
                    )
                }
            }
            MatchSnapshot(
                matches = matches,
                indexEmpty = indexed.isEmpty(),
                enabledRepos = repos.filter { it.enabled }.sortedBy { it.priority }
                    .map { LinkableRepo(it.id, it.name) },
                savedGithubRepos = managedRows
                    .mapNotNull { it.githubOwnerRepo?.takeIf { repo -> repo.contains('/') } }
                    .distinctBy { it.lowercase() }
                    .sortedBy { it.lowercase() },
            )
        }
        val high = snapshot.matches.filter { it.confidence == MatchConfidence.HIGH }
        val pendingHigh = high.filter { !it.alreadyLinked }
        _uiState.update {
            it.copy(
                isLoading = false,
                high = high,
                medium = snapshot.matches.filter { c -> c.confidence == MatchConfidence.MEDIUM },
                warning = snapshot.matches.filter { c -> c.confidence == MatchConfidence.WARNING },
                none = snapshot.matches.filter { c -> c.confidence == MatchConfidence.NONE },
                selected = pendingHigh.map { c -> c.packageName }.toSet(),
                indexEmpty = snapshot.indexEmpty,
                enabledRepos = snapshot.enabledRepos,
                savedGithubRepos = snapshot.savedGithubRepos,
                snackbarLinkedCount = announceCount?.takeIf { count -> count > 0 },
            )
        }
    }

    private fun trackedLabelFor(managed: ManagedAppEntity, repoNames: Map<String, String>): String {
        managed.githubOwnerRepo?.takeIf { it.contains('/') }?.let { return "GitHub $it" }
        val sourceId = managed.sourceId
        if (sourceId.isBlank() || sourceId == "manual") return ""
        if (sourceId.startsWith("github:")) {
            return "GitHub ${sourceId.removePrefix("github:")}"
        }
        return repoNames[sourceId] ?: sourceId
    }

    private data class MatchSnapshot(
        val matches: List<MatchCandidate>,
        val indexEmpty: Boolean,
        val enabledRepos: List<LinkableRepo>,
        val savedGithubRepos: List<String>,
    )
}
