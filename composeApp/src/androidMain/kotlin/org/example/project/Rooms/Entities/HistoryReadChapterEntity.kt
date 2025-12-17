package org.example.project.Rooms.Entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import java.util.UUID

@Entity(
    tableName = "HistoryReadChapterEntity",
    primaryKeys = ["historyId", "chapterUrl"],
    indices = [Index("historyId")],
    foreignKeys = [
        ForeignKey(
            entity = HistoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["historyId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class HistoryReadChapterEntity(
    val historyId: UUID,
    val chapterUrl: String,
    val readAt: Long? = null
)
