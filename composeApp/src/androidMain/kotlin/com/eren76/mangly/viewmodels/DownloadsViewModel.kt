package com.eren76.mangly.viewmodels

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.eren76.mangly.FileManager
import com.eren76.mangly.rooms.dao.DownloadsDao
import com.eren76.mangly.rooms.relations.DownloadWithChapters
import com.eren76.mangly.workers.ChapterDownloadWorker
import com.eren76.manglyextension.plugins.ExtensionMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel
@Inject constructor(
    private val downloadsDao: DownloadsDao,
    private val fileManager: FileManager
) : ViewModel() {
    private val DOWNLOADS_DIRECTORY = "download_covers"
    private val DOWNLOAD_QUEUE_WORK_NAME = "chapter_download_queue"

    val downloads = mutableStateOf<List<DownloadWithChapters>>(emptyList())

    private var queueWorkInfoLiveData: LiveData<List<WorkInfo>>? = null
    private var queueObserver: Observer<List<WorkInfo>>? = null

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            downloads.value = downloadsDao.getAllWithChapters()
        }
    }

    fun getCoverFile(filename: String, context: Context): File? {
        return fileManager.getFileInDir(
            context = context,
            relativeDir = DOWNLOADS_DIRECTORY,
            fileName = filename
        )
    }

    fun createDownload(
        mangaurl: String,
        mangaName: String,
        mangaSummary: String,
        chapterUrl: String,
        chapterName: String,
        extensionMetadata: ExtensionMetadata,
        context: Context,
        queueIndex: Int = 1,
        queueTotal: Int = 1
    ) {
        ensureQueueObserver(context)

        val safeQueueIndex = queueIndex.coerceAtLeast(1)
        val safeQueueTotal = queueTotal.coerceAtLeast(safeQueueIndex)

        val inputData = Data.Builder()
            .putString(ChapterDownloadWorker.KEY_MANGA_URL, mangaurl)
            .putString(ChapterDownloadWorker.KEY_MANGA_NAME, mangaName)
            .putString(ChapterDownloadWorker.KEY_MANGA_SUMMARY, mangaSummary)
            .putString(ChapterDownloadWorker.KEY_CHAPTER_URL, chapterUrl)
            .putString(ChapterDownloadWorker.KEY_CHAPTER_NAME, chapterName)
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
                DOWNLOAD_QUEUE_WORK_NAME,
                ExistingWorkPolicy.APPEND,
                request
            )
            .enqueue()

        // Update quickly after queuing so the Home downloads mode can react immediately.
        refresh()
    }

    // Because the downloading happens in a worker, the view model won't automatically know when to refresh the downloads list.
    // This observer ensures that whenever there's a change in the download queue (like a chapter finishing downloading), the view model will refresh its data.
    private fun ensureQueueObserver(context: Context) {
        if (queueObserver != null) return

        val liveData = WorkManager.getInstance(context.applicationContext)
            .getWorkInfosForUniqueWorkLiveData(DOWNLOAD_QUEUE_WORK_NAME)

        val observer = Observer<List<WorkInfo>> { workInfos ->
            if (workInfos.isNotEmpty()) {
                refresh()
            }
        }

        liveData.observeForever(observer)
        queueWorkInfoLiveData = liveData
        queueObserver = observer
    }

    override fun onCleared() {
        queueObserver?.let { observer ->
            queueWorkInfoLiveData?.removeObserver(observer)
        }
        queueObserver = null
        queueWorkInfoLiveData = null
        super.onCleared()
    }
}
