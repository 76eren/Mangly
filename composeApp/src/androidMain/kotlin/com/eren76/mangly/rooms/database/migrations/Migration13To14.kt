package com.eren76.mangly.rooms.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Makes download metadata required.
 *
 * Downloads without a valid extension cannot satisfy the new non-null FK contract,
 * so they are not copied into the rebuilt table.
 */
val Migration13To14 = object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TEMP TABLE `DownloadedChapterEntity_valid_backup` AS
            SELECT c.*
            FROM `DownloadedChapterEntity` c
            INNER JOIN `DownloadsEntity` d
                ON c.`download_id` = d.`download_id`
            WHERE d.`extension_id` IS NOT NULL
                AND EXISTS (
                    SELECT 1
                    FROM `ExtensionEntity` e
                    WHERE e.`id` = d.`extension_id`
                )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `DownloadsEntity_new` (
                `download_id` BLOB NOT NULL,
                `manga_url` TEXT NOT NULL,
                `manga_name` TEXT,
                `summary` TEXT NOT NULL,
                `cover_image_filename` TEXT NOT NULL,
                `extension_id` BLOB NOT NULL,
                PRIMARY KEY(`download_id`),
                FOREIGN KEY(`extension_id`) REFERENCES `ExtensionEntity`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            INSERT INTO `DownloadsEntity_new` (
                `download_id`,
                `manga_url`,
                `manga_name`,
                `summary`,
                `cover_image_filename`,
                `extension_id`
            )
            SELECT
                d.`download_id`,
                d.`manga_url`,
                d.`manga_name`,
                IFNULL(d.`summary`, ''),
                IFNULL(d.`cover_image_filename`, ''),
                d.`extension_id`
            FROM `DownloadsEntity` d
            WHERE d.`extension_id` IS NOT NULL
                AND EXISTS (
                    SELECT 1
                    FROM `ExtensionEntity` e
                    WHERE e.`id` = d.`extension_id`
                )
            """.trimIndent()
        )

        db.execSQL("DROP TABLE `DownloadsEntity`")
        db.execSQL("ALTER TABLE `DownloadsEntity_new` RENAME TO `DownloadsEntity`")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_DownloadsEntity_extension_id` " +
                    "ON `DownloadsEntity` (`extension_id`)"
        )

        db.execSQL("DELETE FROM `DownloadedChapterEntity`")
        db.execSQL(
            """
            INSERT INTO `DownloadedChapterEntity` (
                `id`,
                `download_id`,
                `chapter_name`,
                `chapter_url`,
                `chapter_index`,
                `downloaded_at`,
                `file_path`,
                `is_fully_downloaded`
            )
            SELECT
                `id`,
                `download_id`,
                `chapter_name`,
                `chapter_url`,
                `chapter_index`,
                `downloaded_at`,
                `file_path`,
                `is_fully_downloaded`
            FROM `DownloadedChapterEntity_valid_backup`
            """.trimIndent()
        )
        db.execSQL("DROP TABLE `DownloadedChapterEntity_valid_backup`")
    }
}
