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
import dev.involvex.kiln.core.model.GitHubReleaseChannel
import dev.involvex.kiln.core.model.ManagedApp
import dev.involvex.kiln.core.model.WatchlistRepository
import dev.involvex.kiln.feature.sources.fdroid.FDroidRepoSync
import dev.involvex.kiln.feature.sources.fdroid.FDroidRepoUri
import dev.involvex.kiln.feature.sources.github.GitHubOwnerRepo
import dev.involvex.kiln.feature.sources.github.GitHubReleasePicker
import dev.involvex.kiln.feature.sources.github.GitHubReleasesClient
import dev.involvex.kiln.feature.sources.github.GitHubStarsClient
import dev.involvex.kiln.feature.sources.github.GitHubTokenStore
import dev.involvex.kiln.feature.sources.github.StarredApkHint
import dev.involvex.kiln.feature.sources.obtainium.ObtainiumImport
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import timber.log.Timber

@HiltViewModel
@Suppress("TooManyFunctions") // Sources hub: repos, Obtainium, PAT, watchlist, starred import.
class SourcesViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val repoDao: RepoDao,
    private val managedAppDao: ManagedAppDao,
    private val sync: FDroidRepoSync,
    private val tokenStore: GitHubTokenStore,
    private val starsClient: GitHubStarsClient,
    private val gitHubReleasesClient: GitHubReleasesClient,
    private val appUpdateRepository: AppUpdateRepository,
    private val watchlistRepository: WatchlistRepository,
    private val incomingRepoLinkStore: IncomingRepoLinkStore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SourcesUiState(githubTokenSet = tokenStore.getToken() != null))
    val uiState: StateFlow<SourcesUiState> = _uiState.asStateFlow()
    private var syncJob: Job? = null

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
            val message = withContext(ioDispatcher) { upsertGithubSource(ownerRepo) }
            _uiState.update { state ->
                state.copy(
                    message = message,
                    starredRepos = state.starredRepos.map { row ->
                        if (row.ownerRepo.equals(ownerRepo, ignoreCase = true) &&
                            message.startsWith("github_added:")
                        ) {
                            row.copy(alreadyManaged = true, selected = false)
                        } else {
                            row
                        }
                    },
                )
            }
            withContext(ioDispatcher) { appUpdateRepository.refreshUpdates() }
        }
    }

    fun loadStarredRepos() {
        if (tokenStore.getToken() == null) {
            _uiState.update { it.copy(message = "github_pat_required") }
            return
        }
        viewModelScope.launch {
            _uiState.update {
                it.copy(starredLoading = true, message = null, starredLoaded = false)
            }
            val result = withContext(ioDispatcher) { starsClient.listStarred() }
            when (result) {
                is Result.Failure -> {
                    _uiState.update {
                        it.copy(
                            starredLoading = false,
                            message = mapStarsError(result.message),
                        )
                    }
                }

                is Result.Success -> {
                    val managed = withContext(ioDispatcher) {
                        managedAppDao.getAll()
                            .mapNotNull { it.githubOwnerRepo?.lowercase() }
                            .toSet()
                    }
                    val rows = result.value.map { repo ->
                        StarredRepoUiModel(
                            ownerRepo = repo.ownerRepo,
                            description = repo.description,
                            fork = repo.fork,
                            alreadyManaged = repo.ownerRepo.lowercase() in managed,
                            selected = false,
                            apkHint = StarredApkHint.UNKNOWN,
                        )
                    }
                    _uiState.update {
                        it.copy(
                            starredRepos = rows,
                            starredLoading = false,
                            starredLoaded = true,
                            message = if (rows.isEmpty()) "stars_empty" else null,
                        )
                    }
                    if (rows.isNotEmpty()) {
                        scanStarredApkHints()
                    }
                }
            }
        }
    }

    fun toggleStarredSelection(ownerRepo: String) {
        _uiState.update { state ->
            state.copy(
                starredRepos = state.starredRepos.map { row ->
                    if (row.ownerRepo.equals(ownerRepo, ignoreCase = true) && !row.alreadyManaged) {
                        row.copy(selected = !row.selected)
                    } else {
                        row
                    }
                },
            )
        }
    }

    fun selectStarredWithApkHint() {
        _uiState.update { state ->
            state.copy(
                starredRepos = state.starredRepos.map { row ->
                    if (!row.alreadyManaged && row.apkHint == StarredApkHint.HAS_APK) {
                        row.copy(selected = true)
                    } else {
                        row
                    }
                },
            )
        }
    }

    fun clearStarredSelection() {
        _uiState.update { state ->
            state.copy(starredRepos = state.starredRepos.map { it.copy(selected = false) })
        }
    }

    fun addSelectedStarredRepos() {
        val selected = _uiState.value.starredRepos.filter { it.selected && !it.alreadyManaged }
        if (selected.isEmpty()) {
            _uiState.update { it.copy(message = "stars_none_selected") }
            return
        }
        viewModelScope.launch {
            val added = ArrayList<String>()
            val existed = ArrayList<String>()
            withContext(ioDispatcher) {
                for (row in selected) {
                    val msg = upsertGithubSource(row.ownerRepo)
                    if (msg.startsWith("github_added:")) {
                        added += row.ownerRepo
                    } else if (msg.startsWith("github_exists:")) {
                        existed += row.ownerRepo
                    }
                }
                appUpdateRepository.refreshUpdates()
            }
            val managedLower = (added + existed).map { it.lowercase() }.toSet()
            _uiState.update { state ->
                state.copy(
                    starredRepos = state.starredRepos.map { row ->
                        if (row.ownerRepo.lowercase() in managedLower) {
                            row.copy(alreadyManaged = true, selected = false)
                        } else {
                            row
                        }
                    },
                    message = "stars_added:${added.size}:${existed.size}",
                )
            }
        }
    }

    private fun scanStarredApkHints() {
        viewModelScope.launch {
            _uiState.update { it.copy(starredScanningApk = true) }
            val targets = _uiState.value.starredRepos
                .filter { it.apkHint == StarredApkHint.UNKNOWN }
                .map { it.ownerRepo }
            val hints = withContext(ioDispatcher) {
                val gate = Semaphore(APK_SCAN_CONCURRENCY)
                coroutineScope {
                    targets.map { ownerRepo ->
                        async {
                            gate.withPermit {
                                val hint = when (val r = starsClient.apkHintFor(ownerRepo)) {
                                    is Result.Success -> r.value
                                    is Result.Failure -> StarredApkHint.CHECK_FAILED
                                }
                                ownerRepo to hint
                            }
                        }
                    }.awaitAll().toMap()
                }
            }
            _uiState.update { state ->
                state.copy(
                    starredScanningApk = false,
                    starredRepos = state.starredRepos.map { row ->
                        hints[row.ownerRepo]?.let { row.copy(apkHint = it) } ?: row
                    },
                )
            }
        }
    }

    private suspend fun upsertGithubSource(ownerRepo: String): String {
        val existing = managedAppDao.getAll()
            .firstOrNull { it.githubOwnerRepo.equals(ownerRepo, ignoreCase = true) }
        if (existing != null) {
            return "github_exists:$ownerRepo"
        }
        val packageName = GitHubOwnerRepo.provisionalPackageName(ownerRepo)
        val assetRegex = Regex(".*\\.apk$")
        val channel = when (val listed = gitHubReleasesClient.listReleases(ownerRepo)) {
            is Result.Failure -> GitHubReleaseChannel.STABLE

            is Result.Success -> {
                GitHubReleasePicker.match(
                    releases = listed.value,
                    assetRegex = assetRegex,
                    preferred = GitHubReleaseChannel.STABLE,
                    fallbackToPreRelease = true,
                )?.channel ?: GitHubReleaseChannel.STABLE
            }
        }
        managedAppDao.upsert(
            ManagedAppEntity(
                packageName = packageName,
                sourceId = "github:$ownerRepo",
                confidence = "MEDIUM",
                pinned = false,
                ignoreUpdates = false,
                githubOwnerRepo = ownerRepo,
                githubAssetRegex = ".*\\.apk$",
                githubChannel = channel.name,
                // So Sync all / checkForUpdates probes this repo before any installed package is linked.
                watchOnly = true,
            ),
        )
        return "github_added:$ownerRepo"
    }

    private fun mapStarsError(raw: String): String = when {
        raw == "github_pat_required" -> "github_pat_required"
        raw.contains("HTTP 401") -> "stars_unauthorized"
        raw.contains("HTTP 403") -> "stars_rate_limited"
        else -> "stars_failed:$raw"
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
        if (_uiState.value.isSyncing) return
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSyncing = true,
                    message = null,
                    syncProgressCurrent = 0,
                    syncProgressTotal = 0,
                    syncProgressLabel = null,
                )
            }
            try {
                when (
                    val result = withContext(ioDispatcher) {
                        appUpdateRepository.checkForUpdates { progress ->
                            _uiState.update {
                                it.copy(
                                    syncProgressCurrent = progress.current,
                                    syncProgressTotal = progress.total,
                                    syncProgressLabel = progress.label,
                                )
                            }
                        }
                    }
                ) {
                    is Result.Success -> {
                        val summary = result.value
                        val detail = when {
                            summary.reposFailed == 0 -> "sync_ok:${summary.reposSynced}"
                            summary.reposSynced == 0 -> "sync_fail:${summary.reposFailed}"
                            else -> "sync_partial:${summary.reposSynced}:${summary.reposFailed}"
                        }
                        _uiState.update { it.copy(message = detail) }
                    }

                    is Result.Failure -> {
                        Timber.w("Sync all failed: %s", result.message)
                        _uiState.update { it.copy(message = "sync_fail:1:${result.message}") }
                    }
                }
            } finally {
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncProgressCurrent = 0,
                        syncProgressTotal = 0,
                        syncProgressLabel = null,
                    )
                }
            }
        }
    }

    fun cancelSyncAll() {
        syncJob?.cancel()
        syncJob = null
        _uiState.update {
            it.copy(
                isSyncing = false,
                syncProgressCurrent = 0,
                syncProgressTotal = 0,
                syncProgressLabel = null,
                message = "sync_cancelled",
            )
        }
    }

    fun refreshGithubTokenStatus() {
        _uiState.update { it.copy(githubTokenSet = tokenStore.getToken() != null) }
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

    fun moveRepoPriority(repoId: String, delta: Int) {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                val ordered = repoDao.getAll()
                val renumbered = RepoPriorityReorder.afterMove(
                    orderedIds = ordered.map { it.id },
                    moveId = repoId,
                    delta = delta,
                ) ?: return@withContext
                val byId = ordered.associateBy { it.id }
                repoDao.upsertAll(
                    renumbered.mapNotNull { (id, priority) ->
                        byId[id]?.copy(priority = priority)
                    },
                )
            }
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
        const val APK_SCAN_CONCURRENCY = 3
    }
}
