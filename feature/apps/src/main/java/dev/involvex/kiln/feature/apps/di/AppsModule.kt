package dev.involvex.kiln.feature.apps.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.involvex.kiln.core.model.InstalledAppsReader
import dev.involvex.kiln.feature.apps.data.DefaultInstalledAppsReader
import dev.involvex.kiln.feature.apps.data.DefaultInstalledAppsRepository
import dev.involvex.kiln.feature.apps.data.InstalledAppsRepository
import dev.involvex.kiln.feature.apps.data.PackageInfoSource
import dev.involvex.kiln.feature.apps.data.PackageManagerPackageInfoSource
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppsModule {
    @Binds
    @Singleton
    abstract fun bindPackageInfoSource(impl: PackageManagerPackageInfoSource): PackageInfoSource

    @Binds
    @Singleton
    abstract fun bindInstalledAppsRepository(impl: DefaultInstalledAppsRepository): InstalledAppsRepository

    @Binds
    @Singleton
    abstract fun bindInstalledAppsReader(impl: DefaultInstalledAppsReader): InstalledAppsReader
}
