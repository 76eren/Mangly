package com.eren76.mangly.viewmodels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eren76.mangly.HistoryManager
import com.eren76.mangly.rooms.entities.HistoryWithReadChapters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel
@Inject constructor(private val historyManager: HistoryManager) : ViewModel() {
    val historyWithChapters = mutableStateOf<List<HistoryWithReadChapters>>(emptyList())

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
}
