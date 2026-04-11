package com.eren76.mangly.rooms.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.eren76.mangly.rooms.entities.DownloadedChapterEntity
import com.eren76.mangly.rooms.entities.DownloadsEntity

data class DownloadWithChapters(
    @Embedded val download: DownloadsEntity,

    @Relation(
        parentColumn = "download_id",
        entityColumn = "download_id"
    )
    val chapters: List<DownloadedChapterEntity>
)