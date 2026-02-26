package com.eren76.mangly.rooms.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds nullable column CoverImageFilename to FavoritesEntity and HistoryEntity.
 */
val Migration5To6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE FavoritesEntity ADD COLUMN CoverImageFilename TEXT")
        db.execSQL("ALTER TABLE HistoryEntity ADD COLUMN CoverImageFilename TEXT")
    }
}
