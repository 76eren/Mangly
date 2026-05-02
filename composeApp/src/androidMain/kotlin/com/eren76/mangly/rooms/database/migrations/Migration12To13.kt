package com.eren76.mangly.rooms.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds chapter_index to DownloadedChapterEntity to allow correct ordering.
 */
val Migration12To13 = object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Nullable column, so it is safe to add without a default.
        db.execSQL(
            "ALTER TABLE `DownloadedChapterEntity` ADD COLUMN `chapter_index` INTEGER"
        )
        // index for faster ordering/grouping.
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_DownloadedChapterEntity_download_id_chapter_index` " +
                    "ON `DownloadedChapterEntity` (`download_id`, `chapter_index`)"
        )
    }
}

