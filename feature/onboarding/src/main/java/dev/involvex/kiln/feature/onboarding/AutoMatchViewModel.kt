package dev.involvex.kiln.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.involvex.kiln.core.common.IoDispatcher
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

data class AutoMatchUiState(
    val isLoading: Boolean = true,
    val high: List<MatchCandidate> = emptyList(),
    val medium: List<MatchCandidate> = emptyList(),
    val warning: List<MatchCandidate> = emptyList(),
    val none: List<MatchCandidate> = emptyList(),
    val selected: Set<String> = emptySet(),
    val indexEmpty: Boolean = false,
    val message: String? = null,
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
            _uiState.update { it.copy(isLoading = true) }
            val (result, indexEmpty) = withContext(ioDispatcher) {
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
                matches to indexed.isEmpty()
            }
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

    fun acceptSelected() {
        viewModelScope.launch {
            val state = _uiState.value
            val all = state.high + state.medium + state.warning
            val chosen = all.filter {
                it.packageName in state.selected && it.confidence != MatchConfidence.WARNING
            }
            withContext(ioDispatcher) {
                managedAppDao.upsertAll(
                    chosen.map {
                        ManagedAppEntity(
                            packageName = it.packageName,
                            sourceId = it.repoId.ifBlank { "manual" },
                            confidence = it.confidence.name,
                            pinned = false,
                            ignoreUpdates = false,
                            githubOwnerRepo = it.githubOwnerRepo,
                            githubAssetRegex = it.githubAssetRegex,
                            includePreReleases = false,
                        )
                    },
                )
                appUpdateRepository.refreshUpdates()
            }
            _uiState.update { it.copy(message = "accepted:${chosen.size}") }
        }
    }

    fun acceptAllSafe() {
        _uiState.update { it.copy(selected = it.high.map { c -> c.packageName }.toSet()) }
        acceptSelected()
    }
}
