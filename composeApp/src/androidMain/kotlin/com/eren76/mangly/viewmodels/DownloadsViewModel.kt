package com.eren76.mangly.viewmodels

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eren76.mangly.DownloadManager
import com.eren76.mangly.rooms.dao.DownloadsDao
import com.eren76.mangly.rooms.relations.DownloadWithChapters
import com.eren76.manglyextension.plugins.ExtensionMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel
@Inject constructor(
    private val downloadsDao: DownloadsDao,
    private val downloadManager: DownloadManager
) : ViewModel() {
    private val DOWNLOADS_DIRECTORY = "downloads"

    val downloads = mutableStateOf<List<DownloadWithChapters>>(emptyList())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            downloads.value = downloadsDao.getAllWithChapters()
        }
    }

    fun createDownload(
        mangaurl: String,
        mangaName: String,
        chapterUrl: String,
        extensionMetadata: ExtensionMetadata,
        context: Context
    ) {
        viewModelScope.launch {
            downloadManager.downloadChapter(
                mangaurl = mangaurl,
                mangaName = mangaName,
                chapterUrl = chapterUrl,
                extensionMetadata = extensionMetadata,
                context = context,
                downoadsDirectory = DOWNLOADS_DIRECTORY,
                currentDownloads = downloads.value
            )
            refresh()
        }

    }
}
