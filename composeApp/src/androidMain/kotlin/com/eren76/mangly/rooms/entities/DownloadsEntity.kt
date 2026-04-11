package com.eren76.mangly.rooms.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "DownloadsEntity",
    foreignKeys = [
        ForeignKey(
            entity = ExtensionEntity::class,
            parentColumns = ["id"],
            childColumns = ["extension_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index(value = ["extension_id"])],
)
data class DownloadsEntity(
    @PrimaryKey
    @ColumnInfo(name = "download_id")
    val downloadId: UUID,

    @ColumnInfo(name = "manga_url")
    val mangaUrl: String,

    @ColumnInfo(name = "manga_name")
    val mangaName: String? = null,

    @ColumnInfo(name = "cover_image_filename")
    val coverImageFilename: String? = null,

    @ColumnInfo(name = "extension_id")
    val extensionId: UUID? = null
)
