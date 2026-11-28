package dev.involvex.kiln.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE indexed_releases ADD COLUMN nativeCodesCsv TEXT NOT NULL DEFAULT ''",
        )
    }
}

val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE install_jobs ADD COLUMN targetUserId INTEGER")
    }
}

val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE managed_apps ADD COLUMN releaseForge TEXT NOT NULL DEFAULT 'GITHUB'",
        )
    }
}
