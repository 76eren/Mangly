package com.eren76.mangly.viewmodels

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.eren76.mangly.rooms.dao.DownloadsDao
import com.eren76.mangly.rooms.relations.DownloadWithChapters
import com.eren76.mangly.workers.ChapterDownloadWorker
import com.eren76.manglyextension.plugins.ExtensionMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel
@Inject constructor(
    private val downloadsDao: DownloadsDao
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
        context: Context,
        queueIndex: Int = 1,
        queueTotal: Int = 1
    ) {
        val safeQueueIndex = queueIndex.coerceAtLeast(1)
        val safeQueueTotal = queueTotal.coerceAtLeast(safeQueueIndex)

        val inputData = Data.Builder()
            .putString(ChapterDownloadWorker.KEY_MANGA_URL, mangaurl)
            .putString(ChapterDownloadWorker.KEY_MANGA_NAME, mangaName)
            .putString(ChapterDownloadWorker.KEY_CHAPTER_URL, chapterUrl)
            .putString(
                ChapterDownloadWorker.KEY_EXTENSION_ID,
                extensionMetadata.source.getExtensionId()
            )
            .putString(ChapterDownloadWorker.KEY_DOWNLOADS_DIR, DOWNLOADS_DIRECTORY)
            .putInt(ChapterDownloadWorker.KEY_QUEUE_INDEX, safeQueueIndex)
            .putInt(ChapterDownloadWorker.KEY_QUEUE_TOTAL, safeQueueTotal)
            .build()

        val request = OneTimeWorkRequestBuilder<ChapterDownloadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInputData(inputData)
            .build()

        // This limits the download queue to one worker, but ensures that chapters are downloaded sequentially and not in parallel
        // In the future I might consider allowing multiple parallel downloads
        // REMINDER BEFORE CHANGING THIS: if changing to parallel make sure to not mess up the notifications
        WorkManager.getInstance(context.applicationContext)
            .beginUniqueWork(
                "chapter_download_queue",
                ExistingWorkPolicy.APPEND,
                request
            )
            .enqueue()
    }
}
