package com.eren76.mangly.rooms.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 2 -> 3: introduces HistoryEntity table for reading history.
 * Fields: id (UUID as BLOB) PK, mangaUrl TEXT, mangaName TEXT, readChapters TEXT, extensionId (UUID as BLOB) FK
 */
val Migration2To3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `HistoryEntity` (" +
                    "`id` BLOB NOT NULL, " +
                    "`mangaUrl` TEXT NOT NULL, " +
                    "`mangaName` TEXT NOT NULL, " +
                    "`readChapters` TEXT NOT NULL, " +
                    "`extensionId` BLOB NOT NULL, " +
                    "PRIMARY KEY(`id`), " +
                    "FOREIGN KEY(`extensionId`) REFERENCES `ExtensionEntity`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE" +
                    ")"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_HistoryEntity_extensionId` ON `HistoryEntity`(`extensionId`)"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_HistoryEntity_mangaUrl` ON `HistoryEntity`(`mangaUrl`)"
        )
    }
}
