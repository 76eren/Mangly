package com.eren76.mangly

import com.eren76.mangly.rooms.dao.FavoritesDao
import com.eren76.mangly.rooms.entities.FavoritesEntity
import java.util.UUID
import javax.inject.Inject

class FavoritesManager @Inject constructor(
    private val favoritesDao: FavoritesDao
) {

    suspend fun getAllFavoritesFromDb(): List<FavoritesEntity> {
        return favoritesDao.getAll()
    }

    suspend fun addFavoriteToDb(favorite: FavoritesEntity) {
        favoritesDao.insert(favorite)
    }

    suspend fun removeFavoriteFromDb(id: UUID) {
        favoritesDao.delete(id)
    }

    suspend fun isFavoriteInDb(url: String): Boolean {
        return favoritesDao.getByUrl(url).isNotEmpty()
    }

}