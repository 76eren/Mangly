package com.eren76.mangly.rooms.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class ExtensionEntity(
    // Matches the id of the extension
    @PrimaryKey val id: UUID,

    val name: String,
    val filePath: String,
    val uploadTime: Long
)
