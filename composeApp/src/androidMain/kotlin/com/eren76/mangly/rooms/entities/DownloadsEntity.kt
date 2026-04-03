package com.eren76.mangly.rooms.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "DownloadsEntity",
)
data class DownloadsEntity(
    @PrimaryKey
    @ColumnInfo(name = "download_id")
    val downloadId: UUID,

    @ColumnInfo(name = "manga_url")
    val mangaUrl: String,

    @ColumnInfo(name = "manga_name")
    val mangaName: String? = null,

    @ColumnInfo(name = "chapter_url")
    val chapterUrl: String? = null
)
