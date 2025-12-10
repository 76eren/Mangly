package org.example.project.Rooms.Database.Migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration 1 -> 2: introduces FavoritesEntity with FK to ExtensionEntity and index on extensionId.
 */
val Migration1To2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS `FavoritesEntity` (" +
                    "`id` BLOB NOT NULL, " +
                    "`mangaUrl` TEXT NOT NULL, " +
                    "`mangaTitle` TEXT NOT NULL, " +
                    "`created_at` INTEGER NOT NULL, " +
                    "`extensionId` BLOB NOT NULL, " +
                    "PRIMARY KEY(`id`), " +
                    "FOREIGN KEY(`extensionId`) REFERENCES `ExtensionEntity`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE" +
                    ")"
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_FavoritesEntity_extensionId` ON `FavoritesEntity`(`extensionId`)"
        )
    }
}
