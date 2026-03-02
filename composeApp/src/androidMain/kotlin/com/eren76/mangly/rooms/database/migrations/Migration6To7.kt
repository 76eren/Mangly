package com.eren76.mangly.rooms.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Rebuilds tables to enforce snake_case column naming.
 */
val Migration6To7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("PRAGMA foreign_keys=OFF")
        try {
            migrateExtensionEntity(db)
            migrateFavoritesEntity(db)
            migrateHistoryEntity(db)
            migrateHistoryReadChapterEntity(db)
        } finally {
            db.execSQL("PRAGMA foreign_keys=ON")
        }
    }

    private fun migrateExtensionEntity(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `ExtensionEntity_new` (
                `id` BLOB NOT NULL,
                `name` TEXT NOT NULL,
                `file_path` TEXT NOT NULL,
                `upload_time` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )

        // Old columns were: id, name, filePath, uploadTime
        db.execSQL(
            """
            INSERT INTO `ExtensionEntity_new` (`id`, `name`, `file_path`, `upload_time`)
            SELECT `id`, `name`, `filePath`, `uploadTime`
            FROM `ExtensionEntity`
            """.trimIndent()
        )

        db.execSQL("DROP TABLE `ExtensionEntity`")
        db.execSQL("ALTER TABLE `ExtensionEntity_new` RENAME TO `ExtensionEntity`")
    }

    private fun migrateFavoritesEntity(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `FavoritesEntity_new` (
                `id` BLOB NOT NULL,
                `manga_url` TEXT NOT NULL,
                `manga_title` TEXT NOT NULL,
                `created_at` INTEGER NOT NULL,
                `cover_image_filename` TEXT,
                `extension_id` BLOB NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`extension_id`) REFERENCES `ExtensionEntity`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )

        val favoritesColumns = existingColumns(db, "FavoritesEntity")
        val coverExpr = when {
            favoritesColumns.contains("coverImageFilename") -> "`coverImageFilename`"
            favoritesColumns.contains("CoverImageFilename") -> "`CoverImageFilename`"
            else -> "NULL"
        }

        db.execSQL(
            """
            INSERT INTO `FavoritesEntity_new` (
                `id`, `manga_url`, `manga_title`, `created_at`, `cover_image_filename`, `extension_id`
            )
            SELECT
                `id`,
                `mangaUrl`,
                `mangaTitle`,
                `created_at`,
                $coverExpr,
                `extensionId`
            FROM `FavoritesEntity`
            """.trimIndent()
        )

        db.execSQL("DROP TABLE `FavoritesEntity`")
        db.execSQL("ALTER TABLE `FavoritesEntity_new` RENAME TO `FavoritesEntity`")

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_FavoritesEntity_extension_id` ON `FavoritesEntity` (`extension_id`)"
        )
    }

    private fun migrateHistoryEntity(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `HistoryEntity_new` (
                `id` BLOB NOT NULL,
                `manga_url` TEXT NOT NULL,
                `manga_name` TEXT NOT NULL,
                `cover_image_filename` TEXT,
                `extension_id` BLOB NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`extension_id`) REFERENCES `ExtensionEntity`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )

        val historyColumns = existingColumns(db, "HistoryEntity")
        val coverExpr = when {
            historyColumns.contains("coverImageFilename") -> "`coverImageFilename`"
            historyColumns.contains("CoverImageFilename") -> "`CoverImageFilename`"
            else -> "NULL"
        }

        db.execSQL(
            """
            INSERT INTO `HistoryEntity_new` (
                `id`, `manga_url`, `manga_name`, `cover_image_filename`, `extension_id`
            )
            SELECT
                `id`,
                `mangaUrl`,
                `mangaName`,
                $coverExpr,
                `extensionId`
            FROM `HistoryEntity`
            """.trimIndent()
        )

        db.execSQL("DROP TABLE `HistoryEntity`")
        db.execSQL("ALTER TABLE `HistoryEntity_new` RENAME TO `HistoryEntity`")

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_HistoryEntity_extension_id` ON `HistoryEntity` (`extension_id`)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_HistoryEntity_manga_url` ON `HistoryEntity` (`manga_url`)"
        )
    }

    private fun migrateHistoryReadChapterEntity(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `HistoryReadChapterEntity_new` (
                `history_id` BLOB NOT NULL,
                `chapter_url` TEXT NOT NULL,
                `read_at` INTEGER,
                PRIMARY KEY(`history_id`, `chapter_url`),
                FOREIGN KEY(`history_id`) REFERENCES `HistoryEntity`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `HistoryReadChapterEntity_new` (`history_id`, `chapter_url`, `read_at`)
            SELECT `historyId`, `chapterUrl`, `readAt`
            FROM `HistoryReadChapterEntity`
            """.trimIndent()
        )

        db.execSQL("DROP TABLE `HistoryReadChapterEntity`")
        db.execSQL("ALTER TABLE `HistoryReadChapterEntity_new` RENAME TO `HistoryReadChapterEntity`")

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_HistoryReadChapterEntity_history_id` ON `HistoryReadChapterEntity` (`history_id`)"
        )
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

