package com.eren76.mangly.rooms.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Rebuilds DownloadsEntity to add extension FK metadata and index.
 */
val Migration8To9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("PRAGMA foreign_keys=OFF")
        try {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `DownloadsEntity_new` (
                    `download_id` BLOB NOT NULL,
                    `manga_url` TEXT NOT NULL,
                    `manga_name` TEXT,
                    `chapter_url` TEXT,
                    `extension_id` BLOB,
                    PRIMARY KEY(`download_id`),
                    FOREIGN KEY(`extension_id`) REFERENCES `ExtensionEntity`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )

            val extensionExpr = if (existingColumns(db, "DownloadsEntity").contains("extension_id")) {
                """
                CASE
                    WHEN EXISTS (
                        SELECT 1
                        FROM `ExtensionEntity` e
                        WHERE e.`id` = d.`extension_id`
                    ) THEN d.`extension_id`
                    ELSE NULL
                END
                """.trimIndent()
            } else {
                "NULL"
            }

            // Keep old rows and only retain extension references that still exist.
            db.execSQL(
                """
                INSERT INTO `DownloadsEntity_new` (`download_id`, `manga_url`, `manga_name`, `chapter_url`, `extension_id`)
                SELECT
                    d.`download_id`,
                    d.`manga_url`,
                    d.`manga_name`,
                    d.`chapter_url`,
                    $extensionExpr
                FROM `DownloadsEntity` d
                """.trimIndent()
            )

            db.execSQL("DROP TABLE `DownloadsEntity`")
            db.execSQL("ALTER TABLE `DownloadsEntity_new` RENAME TO `DownloadsEntity`")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_DownloadsEntity_extension_id` ON `DownloadsEntity` (`extension_id`)"
            )
        } finally {
            db.execSQL("PRAGMA foreign_keys=ON")
        }
    }

    private fun existingColumns(db: SupportSQLiteDatabase, table: String): Set<String> {
        val result = mutableSetOf<String>()
        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (nameIndex >= 0) {
                    result += cursor.getString(nameIndex)
                }
            }
        }
        return result
    }
}


