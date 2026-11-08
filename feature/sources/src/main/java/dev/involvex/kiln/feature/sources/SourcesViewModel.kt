package dev.involvex.kiln.feature.sources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.involvex.kiln.core.common.IoDispatcher
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.common.normalizeFingerprint
import dev.involvex.kiln.core.database.ManagedAppDao
import dev.involvex.kiln.core.database.ManagedAppEntity
import dev.involvex.kiln.core.database.RepoDao
import dev.involvex.kiln.core.database.RepoEntity
import dev.involvex.kiln.core.model.AppUpdateRepository
import dev.involvex.kiln.feature.sources.fdroid.FDroidRepoSync
import dev.involvex.kiln.feature.sources.github.GitHubTokenStore
import dev.involvex.kiln.feature.sources.obtainium.ObtainiumImport
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class SourcesViewModel @Inject constructor(
    private val repoDao: RepoDao,
    private val managedAppDao: ManagedAppDao,
    private val sync: FDroidRepoSync,
    private val tokenStore: GitHubTokenStore,
    private val appUpdateRepository: AppUpdateRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SourcesUiState(githubTokenSet = tokenStore.getToken() != null))
    val uiState: StateFlow<SourcesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repoDao.observeAll().collect { repos ->
                _uiState.update { state ->
                    state.copy(
                        repos = repos.map { it.toUi() },
                        githubTokenSet = tokenStore.getToken() != null,
                    )
                }
            }
        }
    }

    fun onAddFieldsChange(name: String? = null, address: String? = null, fingerprint: String? = null) {
        _uiState.update {
            it.copy(
                addName = name ?: it.addName,
                addAddress = address ?: it.addAddress,
                addFingerprint = fingerprint ?: it.addFingerprint,
            )
        }
    }

    fun onObtainiumTextChange(text: String) {
        _uiState.update { it.copy(obtainiumText = text) }
    }

    fun onGithubTokenInputChange(text: String) {
        _uiState.update { it.copy(githubTokenInput = text) }
    }

    fun addRepo() {
        val state = _uiState.value
        val name = state.addName.trim()
        val address = state.addAddress.trim().trimEnd('/')
        val fp = normalizeFingerprint(state.addFingerprint.trim())
        if (name.isEmpty() || address.isEmpty() || fp.length < MIN_FINGERPRINT_HEX_LENGTH) {
            _uiState.update { it.copy(message = "invalid_repo") }
            return
        }
        viewModelScope.launch {
            withContext(ioDispatcher) {
                val id = name.lowercase().replace(Regex("[^a-z0-9]+"), "-")
                repoDao.upsert(
                    RepoEntity(
                        id = id,
                        name = name,
                        address = address,
                        fingerprint = fp,
                        kind = "FDROID",
                        enabled = true,
                        lastSyncedAtEpochMs = null,
                        priority = 10,
                    ),
                )
            }
            _uiState.update {
                it.copy(addName = "", addAddress = "", addFingerprint = "", message = "repo_added")
            }
        }
    }

    fun syncAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, message = null) }
            val repos = withContext(ioDispatcher) { repoDao.getAll().filter { it.enabled } }
            var ok = 0
            var fail = 0
            for (repo in repos) {
                when (withContext(ioDispatcher) { sync.sync(repo) }) {
                    is Result.Success -> ok++
                    is Result.Failure -> fail++
                }
            }
            withContext(ioDispatcher) { appUpdateRepository.refreshUpdates() }
            _uiState.update {
                it.copy(isSyncing = false, message = "sync:$ok:$fail")
            }
        }
    }

    fun saveGithubToken() {
        val token = _uiState.value.githubTokenInput
        tokenStore.setToken(token.ifBlank { null })
        _uiState.update {
            it.copy(
                githubTokenInput = "",
                githubTokenSet = tokenStore.getToken() != null,
                message = "token_saved",
            )
        }
    }

    fun clearGithubToken() {
        tokenStore.setToken(null)
        _uiState.update { it.copy(githubTokenSet = false, message = "token_cleared") }
    }

    fun importObtainium() {
        val text = _uiState.value.obtainiumText
        viewModelScope.launch {
            when (val parsed = ObtainiumImport.parse(text)) {
                is Result.Failure -> _uiState.update { it.copy(message = parsed.message) }

                is Result.Success -> {
                    withContext(ioDispatcher) {
                        managedAppDao.upsertAll(
                            parsed.value.map {
                                ManagedAppEntity(
                                    packageName = it.packageName,
                                    sourceId = it.sourceId,
                                    confidence = it.confidence.name,
                                    pinned = it.pinned,
                                    ignoreUpdates = it.ignoreUpdates,
                                    githubOwnerRepo = it.githubOwnerRepo,
                                    githubAssetRegex = it.githubAssetRegex,
                                    includePreReleases = it.includePreReleases,
                                )
                            },
                        )
                        appUpdateRepository.refreshUpdates()
                    }
                    _uiState.update {
                        it.copy(obtainiumText = "", message = "obtainium:${parsed.value.size}")
                    }
                }
            }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun RepoEntity.toUi(): RepoUiModel = RepoUiModel(
        id = id,
        name = name,
        address = address,
        fingerprint = fingerprint,
        enabled = enabled,
        lastSyncedLabel = lastSyncedAtEpochMs?.let {
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it))
        },
        priority = priority,
    )

    private companion object {
        const val MIN_FINGERPRINT_HEX_LENGTH = 32
    }
}
