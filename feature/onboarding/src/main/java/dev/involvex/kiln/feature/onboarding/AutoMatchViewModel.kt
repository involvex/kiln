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
    /** Count of links just accepted; UI maps to localized string. */
    val messageAcceptedCount: Int? = null,
    /** String-resource key: `bad_github` | `link_failed` | raw message. */
    val messageErrorKey: String? = null,
    val linkTarget: MatchCandidate? = null,
    val warningConfirmTarget: MatchCandidate? = null,
)

@HiltViewModel
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
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    messageAcceptedCount = null,
                    messageErrorKey = null,
                )
            }
            val snapshot = withContext(ioDispatcher) {
                val installed = installedAppsReader.userApps()
                val indexed = indexDao.allPackages()
                val repos = repoDao.getAll()
                val matches = AutoMatchEngine.match(
                    installed = installed,
                    indexed = indexed,
                    repoPriority = repos.associate { it.id to it.priority },
                    repoNames = repos.associate { it.id to it.name },
                    githubMap = githubMap,
                )
                Triple(
                    matches,
                    indexed.isEmpty(),
                    repos.filter { it.enabled }.sortedBy { it.priority }
                        .map { LinkableRepo(it.id, it.name) },
                )
            }
            val (result, indexEmpty, enabledRepos) = snapshot
            val high = result.filter { it.confidence == MatchConfidence.HIGH }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    high = high,
                    medium = result.filter { c -> c.confidence == MatchConfidence.MEDIUM },
                    warning = result.filter { c -> c.confidence == MatchConfidence.WARNING },
                    none = result.filter { c -> c.confidence == MatchConfidence.NONE },
                    selected = high.map { c -> c.packageName }.toSet(),
                    indexEmpty = indexEmpty,
                    enabledRepos = enabledRepos,
                )
            }
        }
    }

    fun toggle(packageName: String) {
        _uiState.update { state ->
            val next = state.selected.toMutableSet()
            if (!next.add(packageName)) next.remove(packageName)
            state.copy(selected = next)
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(messageAcceptedCount = null, messageErrorKey = null) }
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

    fun confirmWarningAccept() {
        val target = _uiState.value.warningConfirmTarget ?: return
        viewModelScope.launch {
            persist(listOf(target), forcePinned = true)
            _uiState.update { it.copy(warningConfirmTarget = null) }
            refresh()
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
            val chosen = (state.high + state.medium).filter { it.packageName in state.selected }
            persist(chosen, forcePinned = false)
        }
    }

    fun acceptAllSafe() {
        _uiState.update { it.copy(selected = it.high.map { c -> c.packageName }.toSet()) }
        acceptSelected()
    }

    private suspend fun persist(chosen: List<MatchCandidate>, forcePinned: Boolean) {
        if (chosen.isEmpty()) {
            _uiState.update { it.copy(messageAcceptedCount = 0) }
            return
        }
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
                messageAcceptedCount = chosen.size,
                messageErrorKey = null,
                linkTarget = null,
            )
        }
    }

    private suspend fun finishLink(result: Result<Unit>) {
        when (result) {
            is Result.Success -> {
                _uiState.update {
                    it.copy(
                        linkTarget = null,
                        messageAcceptedCount = 1,
                        messageErrorKey = null,
                    )
                }
                refresh()
            }

            is Result.Failure -> {
                _uiState.update {
                    it.copy(messageErrorKey = result.message.ifBlank { "link_failed" })
                }
            }
        }
    }
}
