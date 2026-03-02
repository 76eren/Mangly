package com.eren76.mangly.rooms.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.UUID

@Entity(
    tableName = "HistoryReadChapterEntity",
    primaryKeys = ["history_id", "chapter_url"],
    indices = [Index("history_id")],
    foreignKeys = [
        ForeignKey(
            entity = HistoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["history_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class HistoryChapterEntity(
    @ColumnInfo(name = "history_id")
    val historyId: UUID,

    @ColumnInfo(name = "chapter_url")
    val chapterUrl: String,

    @ColumnInfo(name = "read_at")
    val readAt: Long? = null
)
