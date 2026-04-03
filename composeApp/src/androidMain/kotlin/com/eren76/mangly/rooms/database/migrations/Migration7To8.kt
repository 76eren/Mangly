package com.eren76.mangly.rooms.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds downloads tables and links downloaded chapters to their parent download.
 */
val Migration7To8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("PRAGMA foreign_keys=OFF")
        try {
            createDownloadsEntityTable(db)
            createDownloadedChapterEntityTable(db)
            migrateLegacyDownloadEntityIfPresent(db)
        } finally {
            db.execSQL("PRAGMA foreign_keys=ON")
        }
    }

    private fun createDownloadsEntityTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `DownloadsEntity` (
                `download_id` BLOB NOT NULL,
                `manga_url` TEXT NOT NULL,
                `manga_name` TEXT,
                `chapter_url` TEXT,
                PRIMARY KEY(`download_id`)
            )
            """.trimIndent()
        )
    }

    private fun createDownloadedChapterEntityTable(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `DownloadedChapterEntity` (
                `id` BLOB NOT NULL,
                `download_id` BLOB NOT NULL,
                `chapter_name` TEXT,
                `chapter_url` TEXT,
                `downloaded_at` INTEGER,
                `file_path` TEXT,
                `is_fully_downloaded` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`download_id`) REFERENCES `DownloadsEntity`(`download_id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_DownloadedChapterEntity_download_id` ON `DownloadedChapterEntity` (`download_id`)"
        )
    }

    private fun migrateLegacyDownloadEntityIfPresent(db: SupportSQLiteDatabase) {
        if (!tableExists(db, "DownloadEntity")) return

        val columns = existingColumns(db, "DownloadEntity")

        val downloadIdExpr = when {
            columns.contains("download_id") -> "`download_id`"
            columns.contains("id") -> "`id`"
            else -> "NULL"
        }

        val mangaUrlExpr = if (columns.contains("manga_url")) "`manga_url`" else "''"
        val mangaNameExpr = if (columns.contains("manga_name")) "`manga_name`" else "NULL"
        val chapterUrlExpr = if (columns.contains("chapter_url")) "`chapter_url`" else "NULL"

        db.execSQL(
            """
            INSERT OR IGNORE INTO `DownloadsEntity` (`download_id`, `manga_url`, `manga_name`, `chapter_url`)
            SELECT DISTINCT
                $downloadIdExpr,
                $mangaUrlExpr,
                $mangaNameExpr,
                $chapterUrlExpr
            FROM `DownloadEntity`
            WHERE $downloadIdExpr IS NOT NULL
            """.trimIndent()
        )

        val idExpr = if (columns.contains("id")) "`id`" else "randomblob(16)"
        val childDownloadIdExpr = if (columns.contains("download_id")) "`download_id`" else "NULL"
        val chapterNameExpr = if (columns.contains("chapter_name")) "`chapter_name`" else "NULL"
        val childChapterUrlExpr = if (columns.contains("chapter_url")) "`chapter_url`" else "NULL"
        val downloadedAtExpr = if (columns.contains("downloaded_at")) "`downloaded_at`" else "NULL"
        val filePathExpr = if (columns.contains("file_path")) "`file_path`" else "NULL"
        val isFullyDownloadedExpr =
            if (columns.contains("is_fully_downloaded")) "`is_fully_downloaded`" else "0"

        db.execSQL(
            """
            INSERT OR IGNORE INTO `DownloadedChapterEntity` (
                `id`,
                `download_id`,
                `chapter_name`,
                `chapter_url`,
                `downloaded_at`,
                `file_path`,
                `is_fully_downloaded`
            )
            SELECT
                $idExpr,
                $childDownloadIdExpr,
                $chapterNameExpr,
                $childChapterUrlExpr,
                $downloadedAtExpr,
                $filePathExpr,
                $isFullyDownloadedExpr
            FROM `DownloadEntity`
            WHERE $childDownloadIdExpr IS NOT NULL
            """.trimIndent()
        )
    }

    private fun tableExists(db: SupportSQLiteDatabase, table: String): Boolean {
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='$table'").use {
            return it.moveToFirst()
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

