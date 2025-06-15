package org.example.project.Rooms.Entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class ExtensionEntity(
    @PrimaryKey val id: UUID,
    val name: String,
    val filePath: String,
    val uploadTime: Long
)
