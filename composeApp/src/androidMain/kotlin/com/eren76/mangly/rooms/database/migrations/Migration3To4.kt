package com.eren76.mangly.rooms.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 3 -> 4: Adjust HistoryEntity schema (add mangaName, change layout) safely.
 * Since HistoryEntity was introduced in 3, we recreate the table to match the new schema.
 */
val Migration3To4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create new table with the desired schema
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `HistoryEntity_new` (" +
                    "`id` BLOB NOT NULL, " +
                    "`mangaUrl` TEXT NOT NULL, " +
                    "`mangaName` TEXT NOT NULL, " +
                    "`readChapters` TEXT NOT NULL, " +
                    "`extensionId` BLOB NOT NULL, " +
                    "PRIMARY KEY(`id`), " +
                    "FOREIGN KEY(`extensionId`) REFERENCES `ExtensionEntity`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE" +
                    ")"
        )
        // Migrate data from old table if it exists
        try {
            database.execSQL(
                "INSERT INTO `HistoryEntity_new` (`id`, `mangaUrl`, `mangaName`, `readChapters`, `extensionId`) " +
                        "SELECT `id`, `mangaUrl`, '' AS `mangaName`, `readChapters`, `extensionId` FROM `HistoryEntity`"
            )
        } catch (e: Exception) {
            // If old table doesn't exist or columns mismatch, ignore copy.
        }
        // Drop old table and rename new one
        database.execSQL("DROP TABLE IF EXISTS `HistoryEntity`")
        database.execSQL("ALTER TABLE `HistoryEntity_new` RENAME TO `HistoryEntity`")
        // Recreate indexes
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_HistoryEntity_extensionId` ON `HistoryEntity`(`extensionId`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_HistoryEntity_mangaUrl` ON `HistoryEntity`(`mangaUrl`)"
        )
    }
}

