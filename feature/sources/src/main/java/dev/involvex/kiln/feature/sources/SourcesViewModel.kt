package dev.involvex.kiln.feature.sources

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.involvex.kiln.core.common.IoDispatcher
import dev.involvex.kiln.core.common.RepoMirrorUrls
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.common.normalizeFingerprint
import dev.involvex.kiln.core.database.ManagedAppDao
import dev.involvex.kiln.core.database.ManagedAppEntity
import dev.involvex.kiln.core.database.RepoDao
import dev.involvex.kiln.core.database.RepoEntity
import dev.involvex.kiln.core.model.AppUpdateRepository
import dev.involvex.kiln.core.model.ManagedApp
import dev.involvex.kiln.core.model.WatchlistRepository
import dev.involvex.kiln.feature.sources.fdroid.FDroidRepoSync
import dev.involvex.kiln.feature.sources.fdroid.FDroidRepoUri
import dev.involvex.kiln.feature.sources.github.GitHubOwnerRepo
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
import timber.log.Timber

@HiltViewModel
class SourcesViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val repoDao: RepoDao,
    private val managedAppDao: ManagedAppDao,
    private val sync: FDroidRepoSync,
    private val tokenStore: GitHubTokenStore,
    private val appUpdateRepository: AppUpdateRepository,
    private val watchlistRepository: WatchlistRepository,
    private val incomingRepoLinkStore: IncomingRepoLinkStore,
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
        viewModelScope.launch {
            watchlistRepository.observeWatchlist().collect { entries ->
                _uiState.update { it.copy(watchlist = entries) }
            }
        }
        viewModelScope.launch {
            incomingRepoLinkStore.pending.collect { uri ->
                if (uri != null) {
                    incomingRepoLinkStore.consume()
                    applyIncomingUri(uri)
                }
            }
        }
    }

    fun unwatch(packageName: String) {
        viewModelScope.launch {
            when (val result = withContext(ioDispatcher) { watchlistRepository.unwatchPackage(packageName) }) {
                is Result.Success -> Unit
                is Result.Failure -> _uiState.update { it.copy(message = result.message) }
            }
        }
    }

    fun onAddFieldsChange(name: String? = null, address: String? = null, fingerprint: String? = null) {
        if (address != null) {
            val parsed = FDroidRepoUri.parseRepo(address)
            if (parsed != null) {
                _uiState.update {
                    it.copy(
                        addName = name?.takeIf { n -> n.isNotBlank() } ?: parsed.suggestedName,
                        addAddress = parsed.address,
                        addFingerprint = fingerprint?.takeIf { f -> f.isNotBlank() }
                            ?: parsed.fingerprint.orEmpty(),
                        message = if (parsed.usesHttp) "repo_http_warn" else it.message,
                    )
                }
                return
            }
            val pkg = FDroidRepoUri.parsePackagePage(address)
            if (pkg != null) {
                _uiState.update {
                    it.copy(
                        addAddress = address,
                        message = "package_page:${pkg.packageName}",
                    )
                }
                return
            }
        }
        _uiState.update {
            it.copy(
                addName = name ?: it.addName,
                addAddress = address ?: it.addAddress,
                addFingerprint = fingerprint ?: it.addFingerprint,
            )
        }
    }

    fun applyIncomingUri(raw: String) {
        val parsed = FDroidRepoUri.parseRepo(raw)
        if (parsed != null) {
            _uiState.update {
                it.copy(
                    addName = parsed.suggestedName,
                    addAddress = parsed.address,
                    addFingerprint = parsed.fingerprint.orEmpty(),
                    message = if (parsed.usesHttp) "repo_http_warn" else "repo_uri_filled",
                )
            }
            return
        }
        val pkg = FDroidRepoUri.parsePackagePage(raw)
        if (pkg != null) {
            _uiState.update { it.copy(message = "package_page:${pkg.packageName}") }
            return
        }
        val ownerRepo = ObtainiumImport.githubOwnerRepoFromUrl(raw)
        if (ownerRepo != null) {
            addGithubSource(ownerRepo)
            return
        }
        _uiState.update { it.copy(message = "share_unrecognized") }
    }

    fun addGithubSource(ownerRepo: String) {
        viewModelScope.launch {
            val message = withContext(ioDispatcher) {
                val existing = managedAppDao.getAll()
                    .firstOrNull { it.githubOwnerRepo.equals(ownerRepo, ignoreCase = true) }
                if (existing != null) {
                    return@withContext "github_exists:$ownerRepo"
                }
                val packageName = GitHubOwnerRepo.provisionalPackageName(ownerRepo)
                managedAppDao.upsert(
                    ManagedAppEntity(
                        packageName = packageName,
                        sourceId = "github:$ownerRepo",
                        confidence = "MEDIUM",
                        pinned = false,
                        ignoreUpdates = false,
                        githubOwnerRepo = ownerRepo,
                        githubAssetRegex = ".*\\.apk$",
                        githubChannel = "STABLE",
                    ),
                )
                "github_added:$ownerRepo"
            }
            _uiState.update { it.copy(message = message) }
            withContext(ioDispatcher) { appUpdateRepository.refreshUpdates() }
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
        if (!address.startsWith("https://")) {
            _uiState.update { it.copy(message = "repo_http_warn") }
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
            val errors = mutableListOf<String>()
            for (repo in repos) {
                when (val result = withContext(ioDispatcher) { sync.sync(repo) }) {
                    is Result.Success -> ok++

                    is Result.Failure -> {
                        fail++
                        errors += "${repo.name}: ${result.message}"
                        Timber.w("Sync failed for %s: %s", repo.id, result.message)
                    }
                }
            }
            withContext(ioDispatcher) { appUpdateRepository.refreshUpdates() }
            val detail = when {
                fail == 0 -> "sync_ok:$ok"
                ok == 0 -> "sync_fail:$fail:${errors.firstOrNull().orEmpty()}"
                else -> "sync_partial:$ok:$fail:${errors.firstOrNull().orEmpty()}"
            }
            _uiState.update {
                it.copy(isSyncing = false, message = detail)
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
        applyObtainiumParse(ObtainiumImport.parse(_uiState.value.obtainiumText))
    }

    fun importObtainiumFromUri(uri: Uri) {
        viewModelScope.launch {
            val text = withContext(ioDispatcher) {
                runCatching {
                    appContext.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)
                        ?.use { it.readText() }
                }.getOrElse { error ->
                    Timber.w(error, "Failed reading Obtainium export")
                    null
                }
            }
            if (text.isNullOrBlank()) {
                _uiState.update { it.copy(message = "obtainium_file_read_failed") }
                return@launch
            }
            applyObtainiumParse(ObtainiumImport.parse(text))
        }
    }

    private fun applyObtainiumParse(parsed: Result<List<ManagedApp>>) {
        viewModelScope.launch {
            when (parsed) {
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
                                    githubChannel = it.githubChannel.name,
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

    fun saveRepoMirrors(repoId: String, mirrorsRaw: String) {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                val existing = repoDao.getAll().firstOrNull { it.id == repoId } ?: return@withContext
                val csv = RepoMirrorUrls.parseMirrorsInput(mirrorsRaw)
                repoDao.upsert(existing.copy(mirrorsCsv = csv))
            }
            _uiState.update { it.copy(message = "mirrors_saved") }
        }
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
        mirrorsCsv = mirrorsCsv,
        mirrorCount = RepoMirrorUrls.mirrorsList(mirrorsCsv).size,
    )

    private companion object {
        const val MIN_FINGERPRINT_HEX_LENGTH = 32
    }
}
