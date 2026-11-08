package dev.involvex.kiln.core.common.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.involvex.kiln.core.common.DefaultDispatcher
import dev.involvex.kiln.core.common.IoDispatcher
import dev.involvex.kiln.core.common.MainDispatcher
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {
    @Provides
    @Singleton
    @IoDispatcher
    fun providesIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    @DefaultDispatcher
    fun providesDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @Singleton
    @MainDispatcher
    fun providesMainDispatcher(): CoroutineDispatcher = Dispatchers.Main.immediate
}
