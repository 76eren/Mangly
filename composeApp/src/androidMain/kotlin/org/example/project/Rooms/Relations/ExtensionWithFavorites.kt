package org.example.project.Rooms.Relations

import androidx.room.Embedded
import androidx.room.Relation
import org.example.project.Rooms.Entities.ExtensionEntity
import org.example.project.Rooms.Entities.FavoritesEntity

data class ExtensionWithFavorites(
    @Embedded val extension: ExtensionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "extensionId"
    )
    val favorites: List<FavoritesEntity>
)

