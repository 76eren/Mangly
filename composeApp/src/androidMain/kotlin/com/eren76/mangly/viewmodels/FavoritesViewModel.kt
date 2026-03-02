package com.eren76.mangly.viewmodels

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eren76.mangly.FavoritesManager
import com.eren76.mangly.FileManager
import com.eren76.mangly.rooms.entities.FavoritesEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel
@Inject constructor(
    private val favoritesManager: FavoritesManager,
    private val fileManager: FileManager
) : ViewModel() {
    val favorites = mutableStateOf<List<FavoritesEntity>>(emptyList())

    private val coverDir = "favorite_covers"

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

    fun removeFavorite(id: UUID, context: Context) {
        viewModelScope.launch {
            val toDelete = favorites.value.firstOrNull { it.id == id }
            toDelete?.coverImageFilename?.let { filename ->
                fileManager.deleteFileInDir(
                    context = context,
                    relativeDir = coverDir,
                    fileName = filename
                )
            }

            favoritesManager.removeFavoriteFromDb(id)
            favorites.value = favoritesManager.getAllFavoritesFromDb()

        }
    }

    fun getCoverFile(filename: String, context: Context): File? {
        return fileManager.getFileInDir(
            context = context,
            relativeDir = coverDir,
            fileName = filename
        )
    }

    fun saveCoverBytes(filename: String, bytes: ByteArray, context: Context): File {
        return fileManager.saveBytesToStorage(
            context = context,
            relativeDir = coverDir,
            fileName = filename,
            bytes = bytes,
            overwrite = true
        )
    }

    fun updateFavoriteCoverFilename(favoriteId: UUID, filename: String?) {
        viewModelScope.launch {
            favoritesManager.updateCoverFilename(id = favoriteId, filename = filename)
            favorites.value = favoritesManager.getAllFavoritesFromDb()
        }
    }
}