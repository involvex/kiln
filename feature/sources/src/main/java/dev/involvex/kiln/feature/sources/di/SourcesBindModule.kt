package dev.involvex.kiln.feature.sources.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.involvex.kiln.core.model.AppUpdateRepository
import dev.involvex.kiln.feature.sources.DefaultAppUpdateRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SourcesBindModule {
    @Binds
    @Singleton
    abstract fun bindAppUpdateRepository(impl: DefaultAppUpdateRepository): AppUpdateRepository
}
