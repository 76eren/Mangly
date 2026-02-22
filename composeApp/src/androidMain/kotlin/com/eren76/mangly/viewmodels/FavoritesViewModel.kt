package com.eren76.mangly.viewmodels

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eren76.mangly.FavoritesManager
import com.eren76.mangly.rooms.entities.FavoritesEntity
import com.eren76.manglyextension.plugins.ExtensionMetadata
import com.eren76.manglyextension.plugins.Source
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel
@Inject constructor(private val favoritesManager: FavoritesManager) : ViewModel() {
    val favorites = mutableStateOf<List<FavoritesEntity>>(emptyList())
    val imageCache = mutableStateMapOf<UUID, CachedImageData>()

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

    fun preFetchAllImages(sources: List<ExtensionMetadata>) {
        val sourcesById = sources.associateBy { UUID.fromString(it.source.getExtensionId()) }

        viewModelScope.launch(Dispatchers.IO) {
            // Wait for favorites data to be loaded first to prevent race condition
            val favoritesList = favoritesManager.getAllFavoritesFromDb()

            for (favorite in favoritesList) {
                if (imageCache.containsKey(favorite.id)) continue

                val source = sourcesById[favorite.extensionId]?.source ?: continue

                launch {
                    val image = runCatching {
                        source.getImageForChaptersList(favorite.mangaUrl)
                    }.getOrNull()

                    image?.let {
                        imageCache[favorite.id] = CachedImageData(
                            imageUrl = it.imageUrl,
                            headers = it.headers
                        )
                    }
                }
            }
        }
    }

}

data class CachedImageData(
    val imageUrl: String,
    val headers: List<Source.Header>
)