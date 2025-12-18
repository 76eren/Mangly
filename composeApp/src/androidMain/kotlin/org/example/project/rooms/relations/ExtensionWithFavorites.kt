package org.example.project.rooms.relations

import androidx.room.Embedded
import androidx.room.Relation
import org.example.project.rooms.entities.ExtensionEntity
import org.example.project.rooms.entities.FavoritesEntity

data class ExtensionWithFavorites(
    @Embedded val extension: ExtensionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "extensionId"
    )
    val favorites: List<FavoritesEntity>
)

