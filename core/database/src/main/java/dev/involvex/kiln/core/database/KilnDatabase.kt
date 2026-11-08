package dev.involvex.kiln.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        RepoEntity::class,
        IndexedPackageEntity::class,
        IndexedReleaseEntity::class,
        ManagedAppEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class KilnDatabase : RoomDatabase() {
    abstract fun repoDao(): RepoDao

    abstract fun indexDao(): IndexDao

    abstract fun managedAppDao(): ManagedAppDao
}
