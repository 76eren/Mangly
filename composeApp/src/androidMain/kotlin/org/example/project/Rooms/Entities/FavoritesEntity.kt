package org.example.project.Rooms.Entities

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
            childColumns = ["extensionId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index(value = ["extensionId"])]
)
data class FavoritesEntity(
    @PrimaryKey val id: UUID,
    val mangaUrl: String,
    val mangaTitle: String,
    val created_at: Long,

    // Reference to the owning extension
    val extensionId: UUID
)
