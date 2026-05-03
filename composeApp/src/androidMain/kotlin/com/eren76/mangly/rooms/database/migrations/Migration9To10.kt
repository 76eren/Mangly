package com.eren76.mangly.rooms.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 9 -> 10
 * Removes `chapter_url` column from DownloadsEntity.
 */
val Migration9To10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create new table without chapter_url column
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `DownloadsEntity_new` (
                `download_id` BLOB NOT NULL,
                `manga_url` TEXT NOT NULL,
                `manga_name` TEXT,
                `extension_id` BLOB,
                PRIMARY KEY(`download_id`),
                FOREIGN KEY(`extension_id`) REFERENCES `ExtensionEntity`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )

        // Copy data from old table excluding chapter_url
        db.execSQL(
            """
            INSERT INTO `DownloadsEntity_new` (`download_id`, `manga_url`, `manga_name`, `extension_id`)
            SELECT `download_id`, `manga_url`, `manga_name`, `extension_id`
            FROM `DownloadsEntity`
            """.trimIndent()
        )

        // Drop old table
        db.execSQL("DROP TABLE `DownloadsEntity`")

        // Rename new table to old name
        db.execSQL("ALTER TABLE `DownloadsEntity_new` RENAME TO `DownloadsEntity`")

        // Recreate index on extension_id
        db.execSQL(
            """
            CREATE INDEX IF NOT EXISTS `index_DownloadsEntity_extension_id` ON `DownloadsEntity` (`extension_id`)
            """.trimIndent()
        )
    }
}

