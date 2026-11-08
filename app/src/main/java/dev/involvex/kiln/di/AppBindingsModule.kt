package dev.involvex.kiln.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.involvex.kiln.core.model.ApkRollbackCache
import dev.involvex.kiln.core.model.AppUpdateRepository
import dev.involvex.kiln.core.model.BackgroundUpdatePreferences
import dev.involvex.kiln.core.model.BackgroundUpdateScheduler
import dev.involvex.kiln.core.model.InstallQueue
import dev.involvex.kiln.core.model.InstalledAppsReader
import dev.involvex.kiln.core.model.UpdateObservationStore
import dev.involvex.kiln.feature.apps.data.DefaultInstalledAppsReader
import dev.involvex.kiln.feature.installer.DefaultInstallQueue
import dev.involvex.kiln.feature.installer.rollback.FileApkRollbackCache
import dev.involvex.kiln.feature.settings.data.DataStoreBackgroundUpdatePreferences
import dev.involvex.kiln.feature.sources.DefaultAppUpdateRepository
import dev.involvex.kiln.feature.sources.updatecheck.DefaultBackgroundUpdateScheduler
import dev.involvex.kiln.feature.sources.updatecheck.PrefsUpdateObservationStore
import javax.inject.Singleton

/**
 * Cross-feature contracts from `:core:model` are bound here so `:app` owns the graph edges
 * between feature modules (features must not depend on each other).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindingsModule {
    @Binds
    @Singleton
    abstract fun bindInstalledAppsReader(impl: DefaultInstalledAppsReader): InstalledAppsReader

    @Binds
    @Singleton
    abstract fun bindAppUpdateRepository(impl: DefaultAppUpdateRepository): AppUpdateRepository

    @Binds
    @Singleton
    abstract fun bindInstallQueue(impl: DefaultInstallQueue): InstallQueue

    @Binds
    @Singleton
    abstract fun bindBackgroundUpdatePreferences(impl: DataStoreBackgroundUpdatePreferences): BackgroundUpdatePreferences

    @Binds
    @Singleton
    abstract fun bindBackgroundUpdateScheduler(impl: DefaultBackgroundUpdateScheduler): BackgroundUpdateScheduler

    @Binds
    @Singleton
    abstract fun bindApkRollbackCache(impl: FileApkRollbackCache): ApkRollbackCache

    @Binds
    @Singleton
    abstract fun bindUpdateObservationStore(impl: PrefsUpdateObservationStore): UpdateObservationStore
}
