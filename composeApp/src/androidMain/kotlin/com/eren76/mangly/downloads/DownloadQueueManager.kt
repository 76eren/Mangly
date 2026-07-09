package com.eren76.mangly.downloads

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.work.WorkInfo
import com.eren76.mangly.downloads.models.DownloadChapterQueueRequest
import com.eren76.mangly.downloads.models.DownloadQueueItem
import com.eren76.manglyextension.plugins.ExtensionMetadata
import java.util.UUID

object DownloadQueueManager {
    fun enqueueChapterDownloads(
        context: Context,
        mangaUrl: String,
        mangaName: String,
        mangaSummary: String,
        chapters: List<DownloadChapterQueueRequest>,
        extensionMetadata: ExtensionMetadata,
        activeQueueItems: List<DownloadQueueItem>
    ): Int {
        return DownloadQueueWriter.enqueueChapterDownloads(
            context = context,
            mangaUrl = mangaUrl,
            mangaName = mangaName,
            mangaSummary = mangaSummary,
            chapters = chapters,
            extensionMetadata = extensionMetadata,
            activeQueueItems = activeQueueItems
        )
    }

    fun observeDownloadQueueWorkInfos(context: Context): LiveData<List<WorkInfo>> {
        return DownloadQueueReader.observeDownloadQueueWorkInfos(context)
    }

    fun cancelDownloadQueue(context: Context) {
        DownloadQueueWriter.cancelDownloadQueue(context)
    }

    fun dismissSingleFinishedQueueItemByWorkId(context: Context, workId: UUID) {
        DownloadQueueReader.dismissSingleFinishedQueueItemByWorkId(context, workId)
    }

    fun dismissFinishedQueueItemsByWorkIds(context: Context, workIds: Collection<UUID>) {
        DownloadQueueReader.dismissFinishedQueueItemsByWorkIds(context, workIds)
    }

    fun visibleQueueItemsFromWorkInfos(
        context: Context,
        workInfos: List<WorkInfo>
    ): List<DownloadQueueItem> {
        return DownloadQueueReader.visibleQueueItemsFromWorkInfos(
            context = context,
            workInfos = workInfos
        )
    }
}
