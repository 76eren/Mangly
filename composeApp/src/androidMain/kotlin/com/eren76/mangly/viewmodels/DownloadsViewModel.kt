package com.eren76.mangly.viewmodels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eren76.mangly.rooms.dao.DownloadsDao
import com.eren76.mangly.rooms.entities.DownloadedChapterEntity
import com.eren76.mangly.rooms.entities.DownloadsEntity
import com.eren76.mangly.rooms.relations.DownloadWithChapters
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel
@Inject constructor(
    private val downloadsDao: DownloadsDao
) : ViewModel() {
    val downloads = mutableStateOf<List<DownloadWithChapters>>(emptyList())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            downloads.value = downloadsDao.getAllWithChapters()
        }
    }

    fun addOrUpdateDownload(download: DownloadsEntity) {
        viewModelScope.launch {
            downloadsDao.insertDownload(download)
            refresh()
        }
    }

    fun addDownloadedChapter(
        download: DownloadsEntity,
        chapter: DownloadedChapterEntity
    ) {
        viewModelScope.launch {
            downloadsDao.ensureDownloadAndInsertChapter(download, chapter)
            refresh()
        }
    }

    fun updateChapterDownloadState(
        chapterId: UUID,
        isFullyDownloaded: Boolean,
        filePath: String?,
        downloadedAt: Long? = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            downloadsDao.updateDownloadedChapterState(
                chapterId = chapterId,
                isFullyDownloaded = isFullyDownloaded,
                filePath = filePath,
                downloadedAt = downloadedAt
            )
            refresh()
        }
    }

    fun deleteChapter(chapterId: UUID) {
        viewModelScope.launch {
            downloadsDao.deleteDownloadedChapterById(chapterId)
            refresh()
        }
    }

    fun deleteDownload(downloadId: UUID) {
        viewModelScope.launch {
            downloadsDao.deleteDownloadById(downloadId)
            refresh()
        }
    }
}

