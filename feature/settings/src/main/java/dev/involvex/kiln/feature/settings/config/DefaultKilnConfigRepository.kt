package dev.involvex.kiln.feature.settings.config

import dev.involvex.kiln.core.common.IoDispatcher
import dev.involvex.kiln.core.common.Result
import dev.involvex.kiln.core.database.ManagedAppDao
import dev.involvex.kiln.core.database.ManagedAppEntity
import dev.involvex.kiln.core.database.RepoDao
import dev.involvex.kiln.core.database.RepoEntity
import dev.involvex.kiln.core.model.AppearancePreferences
import dev.involvex.kiln.core.model.AutomationPreferences
import dev.involvex.kiln.core.model.BackgroundUpdatePreferences
import dev.involvex.kiln.core.model.GitHubAuthHeaderProvider
import dev.involvex.kiln.core.model.InstallerPreferences
import dev.involvex.kiln.core.model.KilnConfigImportPreview
import dev.involvex.kiln.core.model.KilnConfigImportResult
import dev.involvex.kiln.core.model.KilnConfigRepository
import dev.involvex.kiln.core.model.KilnFontStyle
import dev.involvex.kiln.core.model.KilnLanguage
import dev.involvex.kiln.core.model.KilnThemeStyle
import dev.involvex.kiln.core.model.MaintenancePreferences
import dev.involvex.kiln.core.model.PreferredInstaller
import dev.involvex.kiln.core.model.TermuxPreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class DefaultKilnConfigRepository @Inject constructor(
    private val repoDao: RepoDao,
    private val managedAppDao: ManagedAppDao,
    private val backgroundUpdatePreferences: BackgroundUpdatePreferences,
    private val automationPreferences: AutomationPreferences,
    private val maintenancePreferences: MaintenancePreferences,
    private val appearancePreferences: AppearancePreferences,
    private val installerPreferences: InstallerPreferences,
    private val termuxPreferences: TermuxPreferences,
    private val gitHubAuth: GitHubAuthHeaderProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : KilnConfigRepository {
    override suspend fun exportJson(versionName: String, versionCode: Long, exportedAtEpochMs: Long): Result<String> =
        withContext(ioDispatcher) {
            val doc = buildDocument(versionName, versionCode, exportedAtEpochMs)
            val encoded = KilnConfigCodec.encode(doc)
            if (KilnConfigCodec.containsForbiddenSecretKeys(encoded)) {
                return@withContext Result.failure("kiln_config_secret_leak_guard")
            }
            Result.success(encoded)
        }

    override suspend fun previewImport(json: String): Result<KilnConfigImportPreview> = withContext(ioDispatcher) {
        when (val decoded = KilnConfigCodec.decode(json)) {
            is Result.Failure -> decoded

            is Result.Success -> {
                val doc = decoded.value
                val existingIds = repoDao.getAll().map { it.id }.toSet()
                val (neu, updated) = KilnConfigMerge.countRepoChanges(existingIds, doc.repos)
                Result.success(
                    KilnConfigImportPreview(
                        schemaVersion = doc.schemaVersion,
                        reposNew = neu,
                        reposUpdated = updated,
                        managedAppsUpserted = doc.managedApps.count { it.packageName.isNotBlank() },
                        prefsSections = KilnConfigMerge.prefsSectionsPresent(doc.preferences),
                        githubPatWasConfigured = doc.hints.githubPatWasConfigured,
                    ),
                )
            }
        }
    }

    override suspend fun importMerge(json: String): Result<KilnConfigImportResult> = withContext(ioDispatcher) {
        when (val decoded = KilnConfigCodec.decode(json)) {
            is Result.Failure -> decoded

            is Result.Success -> {
                val doc = decoded.value
                val repos = doc.repos.mapNotNull { it.toEntityOrNull() }
                if (repos.isNotEmpty()) {
                    repoDao.upsertAll(repos)
                }
                val apps = doc.managedApps.mapNotNull { it.toEntityOrNull() }
                if (apps.isNotEmpty()) {
                    managedAppDao.upsertAll(apps)
                }
                val prefsApplied = applyPreferences(doc.preferences)
                Result.success(
                    KilnConfigImportResult(
                        reposUpserted = repos.size,
                        managedAppsUpserted = apps.size,
                        prefsSectionsApplied = prefsApplied,
                    ),
                )
            }
        }
    }

    private suspend fun buildDocument(versionName: String, versionCode: Long, exportedAtEpochMs: Long): KilnConfigV1 {
        val bg = backgroundUpdatePreferences.current()
        val auto = automationPreferences.current()
        val maint = maintenancePreferences.current()
        val appearance = appearancePreferences.current()
        val installer = installerPreferences.current()
        val termux = termuxPreferences.current()
        // hasToken() only — never getToken() / never serialize the secret.
        val patConfigured = gitHubAuth.hasToken()
        return KilnConfigV1(
            schemaVersion = KilnConfigCodec.SCHEMA_VERSION,
            exportedAtEpochMs = exportedAtEpochMs,
            kilnVersionName = versionName,
            kilnVersionCode = versionCode,
            repos = repoDao.getAll().map { repo ->
                KilnConfigRepoV1(
                    id = repo.id,
                    name = repo.name,
                    address = repo.address,
                    fingerprint = repo.fingerprint,
                    kind = repo.kind,
                    enabled = repo.enabled,
                    priority = repo.priority,
                )
            },
            managedApps = managedAppDao.getAll().map { app ->
                KilnConfigManagedAppV1(
                    packageName = app.packageName,
                    sourceId = app.sourceId,
                    confidence = app.confidence,
                    pinned = app.pinned,
                    ignoreUpdates = app.ignoreUpdates,
                    githubOwnerRepo = app.githubOwnerRepo,
                    githubAssetRegex = app.githubAssetRegex,
                    githubChannel = app.githubChannel,
                    watchOnly = app.watchOnly,
                )
            },
            preferences = KilnConfigPreferencesV1(
                backgroundUpdates = KilnConfigBackgroundUpdatesV1(
                    enabled = bg.enabled,
                    unmeteredOnly = bg.unmeteredOnly,
                    autoInstallMatching = bg.autoInstallMatching,
                    updateCooldownDays = bg.updateCooldownDays,
                ),
                automation = KilnConfigAutomationV1(enabled = auto.enabled),
                maintenance = KilnConfigMaintenanceV1(
                    enabled = maint.enabled,
                    trimCaches = maint.trimCaches,
                    pruneDownloads = maint.pruneDownloads,
                    pruneRollback = maint.pruneRollback,
                    pruneBackupTemp = maint.pruneBackupTemp,
                ),
                appearance = KilnConfigAppearanceV1(
                    theme = appearance.theme.name,
                    language = appearance.language.name,
                    fontStyle = appearance.fontStyle.name,
                    dynamicColor = appearance.dynamicColor,
                ),
                installer = KilnConfigInstallerV1(preferred = installer.name),
                termux = KilnConfigTermuxV1(
                    enabled = termux.enabled,
                    packageId = termux.packageId,
                ),
            ),
            hints = KilnConfigHintsV1(githubPatWasConfigured = patConfigured),
        )
    }

    private suspend fun applyPreferences(prefs: KilnConfigPreferencesV1): Int {
        var applied = 0
        prefs.backgroundUpdates?.let { section ->
            backgroundUpdatePreferences.setEnabled(section.enabled)
            backgroundUpdatePreferences.setUnmeteredOnly(section.unmeteredOnly)
            backgroundUpdatePreferences.setAutoInstallMatching(section.autoInstallMatching)
            backgroundUpdatePreferences.setUpdateCooldownDays(section.updateCooldownDays)
            applied++
        }
        prefs.automation?.let { section ->
            automationPreferences.setEnabled(section.enabled)
            applied++
        }
        prefs.maintenance?.let { section ->
            maintenancePreferences.setEnabled(section.enabled)
            maintenancePreferences.setTrimCaches(section.trimCaches)
            maintenancePreferences.setPruneDownloads(section.pruneDownloads)
            maintenancePreferences.setPruneRollback(section.pruneRollback)
            maintenancePreferences.setPruneBackupTemp(section.pruneBackupTemp)
            applied++
        }
        prefs.appearance?.let { section ->
            appearancePreferences.setTheme(enumOrDefault(section.theme, KilnThemeStyle.DARK))
            appearancePreferences.setLanguage(enumOrDefault(section.language, KilnLanguage.SYSTEM))
            appearancePreferences.setFontStyle(enumOrDefault(section.fontStyle, KilnFontStyle.DEFAULT))
            appearancePreferences.setDynamicColor(section.dynamicColor)
            applied++
        }
        prefs.installer?.let { section ->
            installerPreferences.setPreferred(enumOrDefault(section.preferred, PreferredInstaller.SYSTEM))
            applied++
        }
        prefs.termux?.let { section ->
            termuxPreferences.setEnabled(section.enabled)
            if (section.packageId.isNotBlank()) {
                termuxPreferences.setPackageId(section.packageId.trim())
            }
            applied++
        }
        return applied
    }

    private fun KilnConfigRepoV1.toEntityOrNull(): RepoEntity? {
        if (id.isBlank() || address.isBlank()) return null
        return RepoEntity(
            id = id.trim(),
            name = name.ifBlank { id },
            address = address.trim(),
            fingerprint = fingerprint.trim(),
            kind = kind.ifBlank { "FDROID" },
            enabled = enabled,
            lastSyncedAtEpochMs = null,
            priority = priority,
        )
    }

    private fun KilnConfigManagedAppV1.toEntityOrNull(): ManagedAppEntity? {
        if (packageName.isBlank() || sourceId.isBlank()) return null
        return ManagedAppEntity(
            packageName = packageName.trim(),
            sourceId = sourceId.trim(),
            confidence = confidence.ifBlank { "MEDIUM" },
            pinned = pinned,
            ignoreUpdates = ignoreUpdates,
            githubOwnerRepo = githubOwnerRepo?.trim()?.takeIf { it.isNotEmpty() },
            githubAssetRegex = githubAssetRegex?.trim()?.takeIf { it.isNotEmpty() },
            githubChannel = githubChannel.ifBlank { "STABLE" },
            watchOnly = watchOnly,
        )
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(raw: String, default: T): T =
        runCatching { enumValueOf<T>(raw.trim().uppercase()) }.getOrDefault(default)
}
