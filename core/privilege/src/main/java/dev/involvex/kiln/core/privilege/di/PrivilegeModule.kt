package dev.involvex.kiln.core.privilege.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.involvex.kiln.core.model.PrivilegedExecutor
import dev.involvex.kiln.core.privilege.DefaultPrivilegedExecutor
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PrivilegeModule {
    @Binds
    @Singleton
    abstract fun bindPrivilegedExecutor(impl: DefaultPrivilegedExecutor): PrivilegedExecutor
}
