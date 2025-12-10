package org.example.project.ViewModels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.manglyextension.plugins.Source
import com.example.manglyextension.plugins.Source.ImageForChaptersList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.example.project.Favorites.FavoritesManager
import org.example.project.Rooms.Entities.FavoritesEntity
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChaptersListViewModel
@Inject constructor(private val favoritesManager: FavoritesManager) : ViewModel() {

    private val chapters = mutableStateOf<List<Source.ChapterValue>?>(null)
    private val image = mutableStateOf<ImageForChaptersList?>(null)
    private val summary = mutableStateOf("")
    val isFavorite = mutableStateOf(false)

    fun setChapters(chapterList: List<Source.ChapterValue>?) {
        chapters.value = chapterList
    }

    fun setImage(chapterImage: ImageForChaptersList?) {
        image.value = chapterImage
    }

    fun setSummary(chapterSummary: String) {
        summary.value = chapterSummary
    }

    fun getChapters(): List<Source.ChapterValue> {
        return chapters.value
            ?: emptyList()
    }

    fun getImage(): ImageForChaptersList? {
        return image.value
    }

    fun getSummary(): String {
        return summary.value
    }

    fun clear() {
        chapters.value = null
        image.value = null
        summary.value = ""
    }

    fun setIsFavorite(favorite: Boolean) {
        isFavorite.value = favorite
    }

    fun getIsFavorite(): Boolean {
        return isFavorite.value
    }

    // Example favorite operations; caller must provide identifiers
    fun checkFavorite(extensionId: UUID) {
        viewModelScope.launch {
            val exists = favoritesManager.isFavoriteInDb(extensionId)
            isFavorite.value = exists
        }
    }

    fun addFavorite(fav: FavoritesEntity) {
        viewModelScope.launch {
            favoritesManager.addFavoriteToDb(fav)
            isFavorite.value = true
        }
    }

    fun removeFavorite(id: UUID) {
        viewModelScope.launch {
            favoritesManager.removeFavoriteFromDb(id)
            isFavorite.value = false
        }
    }
}