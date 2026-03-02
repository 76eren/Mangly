package com.eren76.mangly.rooms.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "FavoritesEntity",
    foreignKeys = [
        ForeignKey(
            entity = ExtensionEntity::class,
            parentColumns = ["id"],
            childColumns = ["extension_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index(value = ["extension_id"])]
)
data class FavoritesEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: UUID,

    @ColumnInfo(name = "manga_url")
    val mangaUrl: String,

    @ColumnInfo(name = "manga_title")
    val mangaTitle: String,

    @ColumnInfo(name = "created_at")
    val created_at: Long,

    @ColumnInfo(name = "cover_image_filename")
    val coverImageFilename: String? = null,

    // Reference to the owning extension
    @ColumnInfo(name = "extension_id")
    val extensionId: UUID
)
