package com.eren76.mangly.viewmodels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eren76.mangly.FavoritesManager
import com.eren76.mangly.rooms.entities.FavoritesEntity
import com.eren76.manglyextension.plugins.Source
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel
@Inject constructor(private val favoritesManager: FavoritesManager) : ViewModel() {
    val favorites = mutableStateOf<List<FavoritesEntity>>(emptyList())

    private val imageCache = ConcurrentHashMap<UUID, CachedImageData>()

    init {
        viewModelScope.launch {
            favorites.value = favoritesManager.getAllFavoritesFromDb()
        }
    }

    fun addFavorite(favorite: FavoritesEntity) {
        viewModelScope.launch {
            favoritesManager.addFavoriteToDb(favorite)
            favorites.value = favoritesManager.getAllFavoritesFromDb()
        }
    }

    fun removeFavorite(id: UUID) {
        viewModelScope.launch {
            favoritesManager.removeFavoriteFromDb(id)
            favorites.value = favoritesManager.getAllFavoritesFromDb()
            // Also remove from image cache
            imageCache.remove(id)
        }
    }


    fun getCachedImageData(favoriteId: UUID): CachedImageData? {
        return imageCache[favoriteId]
    }


    fun addImageDataToCache(favoriteId: UUID, imageUrl: String, headers: List<Source.Header>) {
        imageCache[favoriteId] = CachedImageData(imageUrl, headers)
    }
}

data class CachedImageData(
    val imageUrl: String,
    val headers: List<Source.Header>
)