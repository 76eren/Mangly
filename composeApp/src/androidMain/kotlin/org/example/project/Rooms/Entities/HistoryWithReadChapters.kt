package org.example.project.Rooms.Entities

import androidx.room.Embedded
import androidx.room.Relation


data class HistoryWithReadChapters(
    @Embedded val history: HistoryEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "historyId"
    )
    val readChapters: List<HistoryReadChapterEntity>
)