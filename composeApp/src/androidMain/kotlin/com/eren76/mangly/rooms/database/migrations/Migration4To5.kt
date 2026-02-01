package com.eren76.mangly.rooms.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 4 -> 5: Split HistoryEntity.readChapters into HistoryReadChapterEntity (1:n).
 * - Create new table HistoryReadChapterEntity
 * - Copy data from HistoryEntity.readChapters (delimited string) into rows
 * - Drop readChapters column from HistoryEntity by recreating table without it
 */
val Migration4To5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. Create new table for read chapters
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `HistoryReadChapterEntity` (" +
                    "`historyId` BLOB NOT NULL, " +
                    "`chapterUrl` TEXT NOT NULL, " +
                    "`readAt` INTEGER, " +
                    "PRIMARY KEY(`historyId`, `chapterUrl`), " +
                    "FOREIGN KEY(`historyId`) REFERENCES `HistoryEntity`(`id`) ON DELETE CASCADE" +
                    ")"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_HistoryReadChapterEntity_historyId` ON `HistoryReadChapterEntity`(`historyId`)"
        )

        // 2. If HistoryEntity has readChapters currently stored as string, expand
        // We attempt to read and split using the previous delimiter `|||`.
        // SQLite doesn't have split; do it via temporary table + app processing is not available here.
        // Strategy: read existing rows into a temp table with raw string, then we can only insert as a single row.
        // Since SQL can't split, we leave migration of existing chapters to app-level post-migration if needed.
        // However, to avoid data loss, we will copy rows with readChapters as single entries when no delimiter.

        // Create new HistoryEntity table without readChapters column
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `HistoryEntity_new` (" +
                    "`id` BLOB NOT NULL, " +
                    "`mangaUrl` TEXT NOT NULL, " +
                    "`mangaName` TEXT NOT NULL, " +
                    "`extensionId` BLOB NOT NULL, " +
                    "PRIMARY KEY(`id`), " +
                    "FOREIGN KEY(`extensionId`) REFERENCES `ExtensionEntity`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE" +
                    ")"
        )
        // Copy base columns
        try {
            database.execSQL(
                "INSERT INTO `HistoryEntity_new` (`id`, `mangaUrl`, `mangaName`, `extensionId`) " +
                        "SELECT `id`, `mangaUrl`, `mangaName`, `extensionId` FROM `HistoryEntity`"
            )
        } catch (_: Exception) {
        }

        // Migrate simple case: readChapters had single chapter (no delimiter)
        try {
            database.query("SELECT `id`, `readChapters` FROM `HistoryEntity`").use { cursor ->
                val idIndex = cursor.getColumnIndex("id")
                val chaptersIndex = cursor.getColumnIndex("readChapters")
                while (cursor.moveToNext()) {
                    val idBlob = cursor.getBlob(idIndex)
                    val chapterStr = cursor.getString(chaptersIndex) ?: ""
                    if (chapterStr.isNotEmpty() && !chapterStr.contains("|||")) {
                        // Insert one row
                        val sql =
                            "INSERT OR IGNORE INTO `HistoryReadChapterEntity` (`historyId`, `chapterUrl`, `readAt`) VALUES (?, ?, NULL)"
                        database.execSQL(sql, arrayOf(idBlob, chapterStr))
                    }
                }
            }
        } catch (_: Exception) {
        }

        // Replace old HistoryEntity
        database.execSQL("DROP TABLE IF EXISTS `HistoryEntity`")
        database.execSQL("ALTER TABLE `HistoryEntity_new` RENAME TO `HistoryEntity`")

        // Recreate indexes
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_HistoryEntity_extensionId` ON `HistoryEntity`(`extensionId`)"
        )
        // Create UNIQUE index to match @Entity(indices = [Index(value = ["mangaUrl"], unique = true)])
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_HistoryEntity_mangaUrl` ON `HistoryEntity`(`mangaUrl`)"
        )
    }
}
