package org.cis_india.wsreader.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add the new column language_code with default value
        database.execSQL("ALTER TABLE books ADD COLUMN language_code TEXT NOT NULL DEFAULT 'auto'")
    }
}