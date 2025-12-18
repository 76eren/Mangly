package org.example.project.rooms.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "HistoryEntity",
    foreignKeys = [
        ForeignKey(
            entity = ExtensionEntity::class,
            parentColumns = ["id"],
            childColumns = ["extensionId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["extensionId"]),
        Index(value = ["mangaUrl"], unique = true)
    ]
)
data class HistoryEntity(
    @PrimaryKey val id: UUID,
    val mangaUrl: String,
    val mangaName: String,
    // Reference to the owning extension
    val extensionId: UUID
)