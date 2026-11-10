package dev.involvex.kiln.feature.apps.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.involvex.kiln.feature.apps.data.DefaultInstalledAppsRepository
import dev.involvex.kiln.feature.apps.data.InstalledAppsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppsModule {
    @Binds
    @Singleton
    abstract fun bindInstalledAppsRepository(impl: DefaultInstalledAppsRepository): InstalledAppsRepository
}
