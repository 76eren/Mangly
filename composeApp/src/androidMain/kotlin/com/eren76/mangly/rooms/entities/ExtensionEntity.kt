package com.eren76.mangly.rooms.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class ExtensionEntity(
    // Matches the id of the extension
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: UUID,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "file_path")
    val filePath: String,

    @ColumnInfo(name = "upload_time")
    val uploadTime: Long
)
