package com.eren76.mangly.viewmodels

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.eren76.mangly.FileManager
import com.eren76.mangly.downloads.DownloadQueueManager
import com.eren76.mangly.downloads.DownloadStorage
import com.eren76.mangly.downloads.models.DownloadChapterQueueRequest
import com.eren76.mangly.downloads.models.DownloadQueueItem
import com.eren76.mangly.downloads.models.DownloadQueueStatus
import com.eren76.mangly.rooms.dao.DownloadsDao
import com.eren76.mangly.rooms.entities.DownloadedChapterEntity
import com.eren76.mangly.rooms.relations.DownloadWithChapters
import com.eren76.manglyextension.plugins.ExtensionMetadata
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject

private const val DOWNLOADS_VIEW_MODEL_TAG = "DownloadsViewModel"

@HiltViewModel
class DownloadsViewModel
@Inject constructor(
    private val downloadsDao: DownloadsDao,
    private val fileManager: FileManager
) : ViewModel() {
    private val DOWNLOADS_DIRECTORY = DownloadStorage.DOWNLOADS_DIRECTORY

    companion object {
        const val DOWNLOADS_COVERS_DIRECTORY = DownloadStorage.COVERS_DIRECTORY
    }

    val downloads = mutableStateOf<List<DownloadWithChapters>>(emptyList())
    val downloadQueue = mutableStateOf<List<DownloadQueueItem>>(emptyList())
    val isLoading = mutableStateOf(true)

    private var queueWorkInfoLiveData: LiveData<List<WorkInfo>>? = null
    private var queueObserver: Observer<List<WorkInfo>>? = null
    private val refreshMutex = Mutex()
    private val refreshedFinishedWorkIds = mutableSetOf<UUID>()
    private val pendingFinishedWorkIds = mutableSetOf<UUID>()

    init {
        refresh()
    }

    fun refresh() {
        launchRefresh()
    }

    private fun launchRefresh(
        onSuccess: (() -> Unit)? = null,
        onFailure: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                refreshMutex.withLock {
                    downloads.value = getSortedDownloads()
                    isLoading.value = false
                }
                onSuccess?.invoke()
            } catch (error: CancellationException) {
                onFailure?.invoke()
                throw error
            } catch (error: Exception) {
                isLoading.value = false
                onFailure?.invoke()
                Log.e(DOWNLOADS_VIEW_MODEL_TAG, "Failed to refresh downloads", error)
            }
        }
    }

    private suspend fun getSortedDownloads(): List<DownloadWithChapters> {
        val downloadsWithChapters = downloadsDao.getAllWithChapters()

        return withContext(Dispatchers.Default) {
            downloadsWithChapters.map { downloadWithChapters ->
                downloadWithChapters.copy(
                    chapters = downloadWithChapters.chapters
                        .sortedWith(
                            compareBy<DownloadedChapterEntity>(
                                { it.chapterIndex == null },
                                { it.chapterIndex },
                                { it.chapterName?.lowercase() }
                            )
                        )
                )
            }
        }
    }

    fun getCoverFile(filename: String, context: Context): File? {
        return fileManager.getFileInDir(
            context = context,
            relativeDir = DOWNLOADS_COVERS_DIRECTORY,
            fileName = filename
        )
    }

    fun queueDownloads(
        mangaurl: String,
        mangaName: String,
        mangaSummary: String,
        chapters: List<DownloadChapterQueueRequest>,
        extensionMetadata: ExtensionMetadata,
        context: Context
    ): Int {
        ensureQueueObserver(context)
        val queuedCount: Int = DownloadQueueManager.enqueueChapterDownloads(
            context = context,
            mangaUrl = mangaurl,
            mangaName = mangaName,
            mangaSummary = mangaSummary,
            chapters = chapters,
            extensionMetadata = extensionMetadata,
            activeQueueItems = downloadQueue.value
        )
        if (queuedCount > 0) refresh()
        return queuedCount
    }

    fun observeDownloadQueue(context: Context) {
        ensureQueueObserver(context)
    }

    fun cancelDownloadQueue(context: Context) {
        DownloadQueueManager.cancelDownloadQueue(context)
    }

    fun dismissSingleFinishedDownloadQueueItem(
        context: Context,
        item: DownloadQueueItem
    ) {
        if (!canDismissFinishedQueueItem(item)) return

        DownloadQueueManager.dismissSingleFinishedQueueItemByWorkId(context, item.workId)

        // Makes the UI update immediately instead of waiting for the observer to trigger
        refreshVisibleDownloadQueue(context)
    }

    fun dismissAllFinishedDownloadQueueItems(
        context: Context,
        items: List<DownloadQueueItem>
    ) {
        val dismissibleFinishedItems: List<DownloadQueueItem> =
            items.filter(::canDismissFinishedQueueItem)
        if (dismissibleFinishedItems.isEmpty()) return

        DownloadQueueManager.dismissFinishedQueueItemsByWorkIds(
            context = context,
            workIds = dismissibleFinishedItems.map { item -> item.workId }
        )

        // Makes the UI update immediately instead of waiting for the observer to trigger
        refreshVisibleDownloadQueue(context)
    }

    private fun refreshVisibleDownloadQueue(context: Context) {
        queueWorkInfoLiveData?.value?.let { workInfos: List<WorkInfo> ->
            downloadQueue.value = DownloadQueueManager.visibleQueueItemsFromWorkInfos(
                context = context,
                workInfos = workInfos
            )
        }
    }

    private fun canDismissFinishedQueueItem(item: DownloadQueueItem): Boolean {
        return item.status == DownloadQueueStatus.Failed ||
                item.status == DownloadQueueStatus.Cancelled
    }

    // Keep queue progress current, but reload the library only after a worker finishes.
    private fun ensureQueueObserver(context: Context) {
        if (queueObserver != null) return

        val liveData: LiveData<List<WorkInfo>> =
            DownloadQueueManager.observeDownloadQueueWorkInfos(context)

        val observer: Observer<List<WorkInfo>> = Observer<List<WorkInfo>> { workInfos ->
            downloadQueue.value = DownloadQueueManager.visibleQueueItemsFromWorkInfos(
                context = context,
                workInfos = workInfos
            )

            val newlyFinishedWorkIds = workInfos.asSequence()
                .filter { workInfo -> workInfo.state.isFinished }
                .map { workInfo -> workInfo.id }
                .filterNot { workId ->
                    refreshedFinishedWorkIds.contains(workId) ||
                            pendingFinishedWorkIds.contains(workId)
                }
                .toList()

            if (newlyFinishedWorkIds.isNotEmpty()) {
                pendingFinishedWorkIds.addAll(newlyFinishedWorkIds)
                launchRefresh(
                    onSuccess = {
                        pendingFinishedWorkIds.removeAll(newlyFinishedWorkIds)
                        refreshedFinishedWorkIds.addAll(newlyFinishedWorkIds)
                    },
                    onFailure = {
                        pendingFinishedWorkIds.removeAll(newlyFinishedWorkIds)
                    }
                )
            }
        }

        liveData.observeForever(observer)
        this.queueWorkInfoLiveData = liveData
        this.queueObserver = observer
    }

    suspend fun deleteChapter(
        mangaUrl: String,
        chapterUrl: String,
        context: Context
    ) {
        val downloadWithChapters: DownloadWithChapters? =
            downloadsDao.getWithChaptersByMangaUrl(mangaUrl)
        if (downloadWithChapters == null) {
            return
        }

        val chapter: DownloadedChapterEntity? =
            downloadWithChapters.chapters.find { it.chapterUrl == chapterUrl }
        if (chapter == null) {
            return
        }

        downloadsDao.deleteDownloadedChapterById(chapter.id)

        fileManager.deleteDirectory(
            context = context,
            relativeDir = "$DOWNLOADS_DIRECTORY/${downloadWithChapters.download.downloadId}/${chapter.id}"
        )

        refresh()

        deleteDownloadEntityIfNoChaptersRemaining(mangaUrl, context)
    }

    suspend fun deleteDownloadEntityIfNoChaptersRemaining(mangaUrl: String, context: Context) {
        val downloadWithChapters: DownloadWithChapters? =
            downloadsDao.getWithChaptersByMangaUrl(mangaUrl)
        if (downloadWithChapters == null) {
            return
        }

        if (downloadWithChapters.chapters.isEmpty()) {
            downloadsDao.deleteDownloadById(downloadWithChapters.download.downloadId)

            downloadWithChapters.download.coverImageFilename.takeIf { it.isNotBlank() }?.let { filename ->
                fileManager.deleteFileInDir(
                    context = context,
                    relativeDir = DOWNLOADS_COVERS_DIRECTORY,
                    fileName = filename
                )
            }

            fileManager.deleteDirectory(
                context = context,
                relativeDir = "$DOWNLOADS_DIRECTORY/${downloadWithChapters.download.downloadId}"
            )
        }

        refresh()
    }

    fun deleteWholeMangaDownloadByDownloadEntityId(downloadId: UUID, context: Context) {
        viewModelScope.launch {
            val downloadWithChapters: DownloadWithChapters =
                downloadsDao.getWithChaptersByDownloadId(downloadId)
                    ?: return@launch

            downloadWithChapters.download.coverImageFilename.takeIf { it.isNotBlank() }?.let { filename ->
                fileManager.deleteFileInDir(
                    context = context,
                    relativeDir = DOWNLOADS_COVERS_DIRECTORY,
                    fileName = filename
                )
            }

            fileManager.deleteDirectory(
                context = context,
                relativeDir = "$DOWNLOADS_DIRECTORY/$downloadId"
            )

            downloadsDao.deleteDownloadById(downloadId)
            refresh()
        }
    }

    suspend fun hasDownload(mangaUrl: String): Boolean {
        return downloadsDao.getWithChaptersByMangaUrl(mangaUrl) != null
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
