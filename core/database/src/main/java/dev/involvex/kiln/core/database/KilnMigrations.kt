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
