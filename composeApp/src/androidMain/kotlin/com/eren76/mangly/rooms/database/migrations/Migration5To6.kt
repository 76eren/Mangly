package com.eren76.mangly.rooms.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds nullable column coverImageFilename to FavoritesEntity and HistoryEntity.
 *
 * Note: A previous migration version accidentally created the column as `CoverImageFilename`.
 * This migration is defensive and will repair that state by creating the correctly named column
 * and copying values over.
 */
val Migration5To6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        migrateCoverColumn(db = db, table = "FavoritesEntity")
        migrateCoverColumn(db = db, table = "HistoryEntity")
    }

    private fun migrateCoverColumn(db: SupportSQLiteDatabase, table: String) {
        val expected = "coverImageFilename"
        val legacy = "CoverImageFilename"

        val columns = existingColumns(db, table)

        // If neither exists, create the expected column.
        if (!columns.contains(expected) && !columns.contains(legacy)) {
            db.execSQL("ALTER TABLE $table ADD COLUMN $expected TEXT")
            return
        }

        // If legacy exists but expected doesn't, create expected and copy data.
        if (!columns.contains(expected) && columns.contains(legacy)) {
            db.execSQL("ALTER TABLE $table ADD COLUMN $expected TEXT")
            db.execSQL(
                "UPDATE $table SET $expected = $legacy WHERE $expected IS NULL AND $legacy IS NOT NULL"
            )
        }

        // If expected already exists, nothing to do.
    }

    private fun existingColumns(db: SupportSQLiteDatabase, table: String): Set<String> {
        val result = mutableSetOf<String>()
        val cursor = db.query("PRAGMA table_info(`$table`)")
        cursor.use {
            val nameIndex = it.getColumnIndex("name")
            while (it.moveToNext()) {
                if (nameIndex >= 0) {
                    result += it.getString(nameIndex)
                }
            }
        }
        return result
    }
}
