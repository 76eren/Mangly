package com.eren76.mangly.rooms.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.eren76.mangly.rooms.entities.ExtensionEntity
import com.eren76.mangly.rooms.entities.FavoritesEntity

data class ExtensionWithFavorites(
    @Embedded val extension: ExtensionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "extensionId"
    )
    val favorites: List<FavoritesEntity>
)

