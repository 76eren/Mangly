package com.eren76.mangly.downloads

import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.eren76.mangly.downloads.models.DownloadQueueItem
import com.eren76.mangly.downloads.models.DownloadQueueStatus
import com.eren76.mangly.workers.ChapterDownloadWorker
import java.util.UUID

internal object DownloadQueueReader {
    fun observeDownloadQueueWorkInfos(context: Context): LiveData<List<WorkInfo>> {
        return WorkManager.getInstance(context.applicationContext)
            .getWorkInfosForUniqueWorkLiveData(ChapterDownloadWorker.DOWNLOAD_QUEUE_WORK_NAME)
    }

    // We track the dismissed work IDs in shared preferences so that they can be filtered out from the queue items list.
    fun dismissFinishedQueueItem(context: Context, workId: UUID) {
        downloadQueuePreferences(context).edit {
            putStringSet(
                KEY_DISMISSED_WORK_IDS,
                dismissedWorkIds(context) + workId.toString()
            )
        }
    }

    fun visibleQueueItemsFromWorkInfos(
        context: Context,
        workInfos: List<WorkInfo>
    ): List<DownloadQueueItem> {
        val dismissedWorkIds: Set<String> = dismissedWorkIds(context)
        val allQueueItems: List<DownloadQueueItem> = workInfos.mapNotNull(::mapWorkInfoToQueueItem)

        val visibleQueueItems = mutableListOf<DownloadQueueItem>()
        for (item in allQueueItems) {
            val isDismissed: Boolean = dismissedWorkIds.contains(item.workId.toString())
            if (item.isActive || !isDismissed) {
                visibleQueueItems.add(item)
            }
        }

        return visibleQueueItems.sortedWith(
            compareBy<DownloadQueueItem>(
                { if (it.isActive) 0 else 1 },
                { it.queuedAt.takeIf { queuedAt -> queuedAt > 0 } ?: Long.MAX_VALUE },
                { it.queueIndex },
                { it.workId.toString() }
            )
        )
    }

    private fun mapWorkInfoToQueueItem(workInfo: WorkInfo): DownloadQueueItem? {
        val tags = workInfo.tags
        val stateData = if (workInfo.state.isFinished) workInfo.outputData else workInfo.progress

        if (ChapterDownloadWorker.DOWNLOAD_QUEUE_TAG !in tags && workInfo.state.isFinished) {
            return null
        }

        val queueIndex = stateData.getInt(
            ChapterDownloadWorker.KEY_QUEUE_INDEX,
            tagValueForPrefix(tags, ChapterDownloadWorker.TAG_QUEUE_INDEX_PREFIX)?.toIntOrNull()
                ?: 1
        )
        val queueTotal = stateData.getInt(
            ChapterDownloadWorker.KEY_QUEUE_TOTAL,
            tagValueForPrefix(tags, ChapterDownloadWorker.TAG_QUEUE_TOTAL_PREFIX)?.toIntOrNull()
                ?: queueIndex
        ).coerceAtLeast(queueIndex)

        val item = DownloadQueueItem(
            workId = workInfo.id,
            queueKey = stateData.getString(ChapterDownloadWorker.KEY_QUEUE_KEY)
                ?: tagValueForPrefix(tags, ChapterDownloadWorker.TAG_QUEUE_KEY_PREFIX)
                ?: workInfo.id.toString(),
            mangaName = stateData.getString(ChapterDownloadWorker.KEY_MANGA_NAME)
                ?: tagValueForPrefix(tags, ChapterDownloadWorker.TAG_MANGA_NAME_PREFIX)
                ?: "Queued download",
            chapterName = stateData.getString(ChapterDownloadWorker.KEY_CHAPTER_NAME)
                ?: tagValueForPrefix(tags, ChapterDownloadWorker.TAG_CHAPTER_NAME_PREFIX)
                ?: "Chapter",
            queueIndex = queueIndex,
            queueTotal = queueTotal,
            queuedAt = stateData.getLong(
                ChapterDownloadWorker.KEY_QUEUED_AT,
                tagValueForPrefix(tags, ChapterDownloadWorker.TAG_QUEUED_AT_PREFIX)?.toLongOrNull()
                    ?: 0L
            ),
            status = queueStatusFromWorkInfo(
                workInfo = workInfo,
                statusValue = stateData.getString(ChapterDownloadWorker.KEY_QUEUE_STATUS)
            ),
            attempts = workInfo.runAttemptCount,
            message = stateData.getString(ChapterDownloadWorker.KEY_QUEUE_MESSAGE)
                ?.takeIf { it.isNotBlank() }
        )

        return item.takeIf {
            it.isActive ||
                    it.status == DownloadQueueStatus.Failed ||
                    it.status == DownloadQueueStatus.Cancelled
        }
    }

    private fun queueStatusFromWorkInfo(
        workInfo: WorkInfo,
        statusValue: String?
    ): DownloadQueueStatus {
        return when {
            statusValue == ChapterDownloadWorker.STATUS_FAILED -> DownloadQueueStatus.Failed
            statusValue == ChapterDownloadWorker.STATUS_SUCCEEDED -> DownloadQueueStatus.Done
            statusValue == ChapterDownloadWorker.STATUS_RETRYING -> DownloadQueueStatus.Retrying
            workInfo.state == WorkInfo.State.RUNNING -> DownloadQueueStatus.Running
            workInfo.state == WorkInfo.State.BLOCKED -> DownloadQueueStatus.Queued
            workInfo.state == WorkInfo.State.ENQUEUED && workInfo.runAttemptCount > 0 ->
                DownloadQueueStatus.Retrying

            workInfo.state == WorkInfo.State.ENQUEUED -> DownloadQueueStatus.Queued
            workInfo.state == WorkInfo.State.CANCELLED -> DownloadQueueStatus.Cancelled
            workInfo.state == WorkInfo.State.FAILED -> DownloadQueueStatus.Failed
            else -> DownloadQueueStatus.Done
        }
    }

    private fun dismissedWorkIds(context: Context): Set<String> {
        return downloadQueuePreferences(context)
            .getStringSet(KEY_DISMISSED_WORK_IDS, emptySet())
            .orEmpty()
            .toSet()
    }

    private fun downloadQueuePreferences(context: Context) =
        context.applicationContext.getSharedPreferences(
            QUEUE_PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )

    private fun tagValueForPrefix(tags: Set<String>, prefix: String): String? {
        return tags.firstOrNull { it.startsWith(prefix) }?.removePrefix(prefix)
    }

    private const val QUEUE_PREFERENCES_NAME = "download_queue_preferences"
    private const val KEY_DISMISSED_WORK_IDS = "dismissed_work_ids"
}
