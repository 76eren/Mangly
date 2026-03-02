package com.eren76.mangly.viewmodels

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eren76.mangly.FileManager
import com.eren76.mangly.HistoryManager
import com.eren76.mangly.rooms.entities.HistoryWithReadChapters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject


@HiltViewModel
class HistoryViewModel
@Inject constructor(
    private val historyManager: HistoryManager,
    private val fileManager: FileManager
) : ViewModel() {
    val historyWithChapters = mutableStateOf<List<HistoryWithReadChapters>>(emptyList())

    private val coverDir = "history_cover"

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

    fun updateHistoryCoverFilename(historyId: UUID, filename: String?) {
        viewModelScope.launch {
            historyManager.updateCoverFilename(id = historyId, filename = filename)
            refresh()
        }
    }
}
