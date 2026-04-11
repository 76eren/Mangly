package com.eren76.mangly.rooms.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 10 -> 11
 * Adds nullable cover_image_filename to DownloadsEntity.
 */
val Migration10To11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE `DownloadsEntity`
            ADD COLUMN `cover_image_filename` TEXT
            """.trimIndent()
        )
    }
}

