package dev.involvex.kiln.core.database.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.involvex.kiln.core.database.IndexDao
import dev.involvex.kiln.core.database.InstallJobDao
import dev.involvex.kiln.core.database.KilnDatabase
import dev.involvex.kiln.core.database.ManagedAppDao
import dev.involvex.kiln.core.database.RepoDao
import dev.involvex.kiln.core.database.ensureKnownRepos
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KilnDatabase {
        val db = Room.databaseBuilder(context, KilnDatabase::class.java, "kiln.db")
            .fallbackToDestructiveMigration(true)
            .build()
        CoroutineScope(Dispatchers.IO).launch {
            ensureKnownRepos(db.repoDao())
        }
        return db
    }

    @Provides
    fun provideRepoDao(db: KilnDatabase): RepoDao = db.repoDao()

    @Provides
    fun provideIndexDao(db: KilnDatabase): IndexDao = db.indexDao()

    @Provides
    fun provideManagedAppDao(db: KilnDatabase): ManagedAppDao = db.managedAppDao()

    @Provides
    fun provideInstallJobDao(db: KilnDatabase): InstallJobDao = db.installJobDao()
}
