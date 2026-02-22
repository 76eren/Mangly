package com.eren76.mangly.viewmodels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eren76.mangly.HistoryManager
import com.eren76.mangly.rooms.entities.HistoryWithReadChapters
import com.eren76.manglyextension.plugins.Source
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject


@HiltViewModel
class HistoryViewModel
@Inject constructor(private val historyManager: HistoryManager) : ViewModel() {
    val historyWithChapters = mutableStateOf<List<HistoryWithReadChapters>>(emptyList())

    private val imageCache = ConcurrentHashMap<String, HistoryCachedImageData>()

    init {
        refresh()
    }

    private fun refresh() {
        viewModelScope.launch {
            historyWithChapters.value = historyManager.getAllHistoryWithReadChapters()
        }
    }

    fun ensureHistoryAndAddChapter(
        mangaUrl: String,
        mangaName: String,
        extensionId: UUID,
        chapterUrl: String
    ) {
        viewModelScope.launch {
            historyManager.ensureHistoryAndAddChapter(
                mangaUrl = mangaUrl,
                mangaName = mangaName,
                extensionId = extensionId,
                chapterUrl = chapterUrl,
                readAt = System.currentTimeMillis()
            )
            refresh()
        }
    }

    fun deleteChapterFromHistory(mangaUrl: String, chapterUrl: String) {
        viewModelScope.launch {
            historyManager.deleteChapterByMangaUrlAndChapterUrl(mangaUrl, chapterUrl)
            refresh()
        }
    }

    suspend fun isChapterInHistory(mangaUrl: String, chapterUrl: String): Boolean {
        val historyEntity = historyManager.findByMangaUrl(mangaUrl) ?: return false
        val readChapters = historyManager.getChapters(historyEntity.id)
        return readChapters.any { it.chapterUrl == chapterUrl }
    }


    fun getCachedImageData(mangaUrl: String): HistoryCachedImageData? {
        return imageCache[mangaUrl]
    }


    fun addImageDataToCache(mangaUrl: String, imageUrl: String, headers: List<Source.Header>) {
        imageCache[mangaUrl] = HistoryCachedImageData(imageUrl, headers)
    }
}

data class HistoryCachedImageData(
    val imageUrl: String,
    val headers: List<Source.Header>
)
