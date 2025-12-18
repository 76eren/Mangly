package org.example.project.viewmodels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.example.project.FavoritesManager
import org.example.project.rooms.entities.FavoritesEntity
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel
@Inject constructor(private val favoritesManager: FavoritesManager) : ViewModel() {
    val favorites = mutableStateOf<List<FavoritesEntity>>(emptyList())

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
        }
    }
}