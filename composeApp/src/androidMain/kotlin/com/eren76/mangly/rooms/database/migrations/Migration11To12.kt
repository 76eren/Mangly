package com.eren76.mangly.rooms.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 11 -> 12
 * Adds nullable summary to DownloadsEntity.
 */
val Migration11To12 = object : Migration(11, 12) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            ALTER TABLE `DownloadsEntity`
            ADD COLUMN `summary` TEXT
            """.trimIndent()
        )
    }
}

