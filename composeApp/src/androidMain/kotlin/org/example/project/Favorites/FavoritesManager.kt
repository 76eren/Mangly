package org.example.project.Favorites

import org.example.project.Rooms.Dao.FavoritesDao
import org.example.project.Rooms.Entities.FavoritesEntity
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