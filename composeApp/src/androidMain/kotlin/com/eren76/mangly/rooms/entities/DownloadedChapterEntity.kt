package com.eren76.mangly.rooms.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "DownloadedChapterEntity",
    foreignKeys = [
        ForeignKey(
            entity = DownloadsEntity::class,
            parentColumns = ["download_id"],
            childColumns = ["download_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["download_id"]),

        // Must be declared here so Room expects it during schema validation.
        Index(value = ["download_id", "chapter_index"])
    ]
)
data class DownloadedChapterEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: UUID,

    @ColumnInfo(name = "download_id")
    val downloadId: UUID,

    @ColumnInfo(name = "chapter_name")
    val chapterName: String? = null,

    @ColumnInfo(name = "chapter_url")
    val chapterUrl: String? = null,

    /**
     * Ordering index for chapters within the same manga.
     * 0-based; may contain gaps.
     */
    @ColumnInfo(name = "chapter_index")
    val chapterIndex: Int? = null,

    @ColumnInfo(name = "downloaded_at")
    val downloadedAt: Long? = null,

    @ColumnInfo(name = "file_path")
    val filePath: String? = null,

    @ColumnInfo(name = "is_fully_downloaded")
    val isFullyDownloaded: Boolean = false
)
