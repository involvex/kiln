package dev.involvex.kiln.core.database.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.involvex.kiln.core.common.normalizeFingerprint
import dev.involvex.kiln.core.database.IndexDao
import dev.involvex.kiln.core.database.InstallJobDao
import dev.involvex.kiln.core.database.KilnDatabase
import dev.involvex.kiln.core.database.ManagedAppDao
import dev.involvex.kiln.core.database.RepoDao
import dev.involvex.kiln.core.database.RepoEntity
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
            seedDefaultRepos(db.repoDao())
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

internal suspend fun seedDefaultRepos(repoDao: RepoDao) {
    if (repoDao.getAll().isNotEmpty()) return
    repoDao.upsertAll(
        listOf(
            RepoEntity(
                id = "fdroid",
                name = "F-Droid",
                address = "https://f-droid.org/repo",
                fingerprint = normalizeFingerprint(
                    "43238D512C1E5EB2D6569F4A3AFBF5523418B82E0A3ED1552770ABB9A9C9CCAB",
                ),
                kind = "FDROID",
                enabled = true,
                lastSyncedAtEpochMs = null,
                priority = 1,
            ),
            RepoEntity(
                id = "izzy",
                name = "IzzyOnDroid",
                address = "https://apt.izzysoft.de/fdroid/repo",
                fingerprint = normalizeFingerprint(
                    "3BF0EE3F971FDAC86BBB970A33919B72932B8EF9F8664890C1B975C718BA0EBB",
                ),
                kind = "FDROID",
                enabled = true,
                lastSyncedAtEpochMs = null,
                priority = 0,
            ),
        ),
    )
}
