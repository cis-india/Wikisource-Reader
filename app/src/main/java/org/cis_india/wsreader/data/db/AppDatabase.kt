/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package org.cis_india.wsreader.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.cis_india.wsreader.data.model.*
import org.cis_india.wsreader.data.model.Book
import org.cis_india.wsreader.data.model.Bookmark
import org.cis_india.wsreader.data.model.Catalog
import org.cis_india.wsreader.data.model.Highlight

@Database(
    entities = [Book::class, Bookmark::class, Highlight::class, Catalog::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(
    HighlightConverters::class
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun booksDao(): BooksDao

    abstract fun catalogDao(): CatalogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}
