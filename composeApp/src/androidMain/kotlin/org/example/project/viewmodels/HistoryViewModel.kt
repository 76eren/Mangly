package org.example.project.viewmodels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.example.project.HistoryManager
import org.example.project.rooms.entities.HistoryEntity
import org.example.project.rooms.entities.HistoryWithReadChapters
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

    fun addNewHistoryEntity(historyEntity: HistoryEntity) {
        viewModelScope.launch {
            historyManager.addHistoryEntry(historyEntity)
            refresh()
        }
    }

    fun ensureHistoryAndAddChapter(
        mangaUrl: String,
        mangaName: String,
        extensionId: UUID,
        chapterUrl: String
    ) {
        viewModelScope.launch {
            val existing = historyManager.findByMangaUrl(mangaUrl)
            val historyEntity = existing ?: HistoryEntity(
                id = UUID.randomUUID(),
                mangaUrl = mangaUrl,
                mangaName = mangaName,
                extensionId = extensionId
            ).also { historyManager.addHistoryEntry(it) }

            historyManager.addChapter(historyEntity.id, chapterUrl, System.currentTimeMillis())
            refresh()
        }
    }

}