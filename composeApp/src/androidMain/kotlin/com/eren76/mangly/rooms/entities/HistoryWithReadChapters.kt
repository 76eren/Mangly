package com.eren76.mangly.rooms.entities

import androidx.room.Embedded
import androidx.room.Relation


data class HistoryWithReadChapters(
    @Embedded val history: HistoryEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "history_id"
    )
    val readChapters: List<HistoryChapterEntity>
)