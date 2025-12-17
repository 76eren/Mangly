package org.example.project.ViewModels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.example.project.HistoryManager.HistoryManager
import org.example.project.Rooms.Entities.HistoryEntity
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel
@Inject constructor(private val historyManager: HistoryManager) : ViewModel() {
    val history = mutableStateOf<List<HistoryEntity>>(emptyList())

    init {
        viewModelScope.launch {
            history.value = historyManager.getAllHistoryEntries()
        }
    }

    fun addNewHistoryEntity(historyEntity: HistoryEntity) {
        viewModelScope.launch {
            historyManager.addHistoryEntry(historyEntity)
            history.value = historyManager.getAllHistoryEntries()
        }
    }

    fun addChapterToExistingHistoryEntity(mangaUrl: String, chapterToAdd: String) {
        viewModelScope.launch {
            val existingHistoryEntities = history.value.filter { it.mangaUrl == mangaUrl }
            if (existingHistoryEntities.isNotEmpty()) {
                val existingEntity: HistoryEntity = existingHistoryEntities[0]
                historyManager.addChapter(
                    existingEntity.id,
                    chapterToAdd,
                    System.currentTimeMillis()
                )
            }
        }
    }

}