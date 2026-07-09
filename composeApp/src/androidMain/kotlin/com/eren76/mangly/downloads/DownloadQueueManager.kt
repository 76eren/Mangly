package com.eren76.mangly.downloads

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.eren76.mangly.downloads.models.DownloadChapterQueueRequest
import com.eren76.mangly.downloads.models.DownloadQueueItem
import com.eren76.mangly.downloads.models.DownloadQueueStatus
import com.eren76.mangly.workers.ChapterDownloadWorker
import com.eren76.manglyextension.plugins.ExtensionMetadata
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit

object DownloadQueueManager {
    fun enqueue(
        context: Context,
        mangaUrl: String,
        mangaName: String,
        mangaSummary: String,
        chapters: List<DownloadChapterQueueRequest>,
        extensionMetadata: ExtensionMetadata,
        activeQueueItems: List<DownloadQueueItem>
    ): Int {
        val activeQueueKeys = activeQueueItems
            .filter { it.isActive }
            .mapTo(mutableSetOf()) { it.queueKey }

        // Remove duplicate chapters and filter out chapters that are already in the active queue
        val uniqueChapters = chapters
            .distinctBy { it.chapterUrl }
            .filterNot { chapter -> queueKey(mangaUrl, chapter.chapterUrl) in activeQueueKeys }

        if (uniqueChapters.isEmpty()) return 0

        val batchId = UUID.randomUUID().toString()
        val queuedAt = System.currentTimeMillis()
        val queueTotal = uniqueChapters.size
        val requests: List<OneTimeWorkRequest> = uniqueChapters.mapIndexed { index, chapter ->
            buildWorkRequest(
                mangaUrl = mangaUrl,
                mangaName = mangaName,
                mangaSummary = mangaSummary,
                chapter = chapter,
                extensionMetadata = extensionMetadata,
                queueIndex = index + 1,
                queueTotal = queueTotal,
                batchId = batchId,
                queuedAt = queuedAt + index
            )
        }

        val workManager = WorkManager.getInstance(context.applicationContext)
        requests.drop(1)
            .fold(
                workManager.beginUniqueWork(
                    ChapterDownloadWorker.DOWNLOAD_QUEUE_WORK_NAME,
                    ExistingWorkPolicy.APPEND_OR_REPLACE,
                    requests.first()
                )
            ) { continuation, request ->
                continuation.then(request)
            }
            .enqueue() // This will enqueue the entire chain of work requests and now the chain will start inside of ChapterDownloadWorker.doWork

        return requests.size
    }

    fun observe(context: Context): LiveData<List<WorkInfo>> {
        return WorkManager.getInstance(context.applicationContext)
            .getWorkInfosForUniqueWorkLiveData(ChapterDownloadWorker.DOWNLOAD_QUEUE_WORK_NAME)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(ChapterDownloadWorker.DOWNLOAD_QUEUE_WORK_NAME)
    }

    fun queueItems(workInfos: List<WorkInfo>): List<DownloadQueueItem> {
        return workInfos.mapNotNull(::queueItem)
            .sortedWith(
                compareBy<DownloadQueueItem>(
                    { if (it.isActive) 0 else 1 },
                    { it.queuedAt.takeIf { queuedAt -> queuedAt > 0 } ?: Long.MAX_VALUE },
                    { it.queueIndex },
                    { it.workId.toString() }
                )
            )
    }

    private fun buildWorkRequest(
        mangaUrl: String,
        mangaName: String,
        mangaSummary: String,
        chapter: DownloadChapterQueueRequest,
        extensionMetadata: ExtensionMetadata,
        queueIndex: Int,
        queueTotal: Int,
        batchId: String,
        queuedAt: Long
    ): OneTimeWorkRequest {
        val queueKey = queueKey(mangaUrl, chapter.chapterUrl)
        val inputData = Data.Builder()
            .putString(ChapterDownloadWorker.KEY_MANGA_URL, mangaUrl)
            .putString(ChapterDownloadWorker.KEY_MANGA_NAME, mangaName)
            .putString(ChapterDownloadWorker.KEY_MANGA_SUMMARY, mangaSummary)
            .putString(ChapterDownloadWorker.KEY_CHAPTER_URL, chapter.chapterUrl)
            .putString(ChapterDownloadWorker.KEY_CHAPTER_NAME, chapter.chapterName)
            .putString(
                ChapterDownloadWorker.KEY_EXTENSION_ID,
                extensionMetadata.source.getExtensionId()
            )
            .putString(
                ChapterDownloadWorker.KEY_DOWNLOADS_DIR,
                DownloadStorage.DOWNLOADS_DIRECTORY
            )
            .putInt(ChapterDownloadWorker.KEY_QUEUE_INDEX, queueIndex)
            .putInt(ChapterDownloadWorker.KEY_QUEUE_TOTAL, queueTotal)
            .putString(ChapterDownloadWorker.KEY_QUEUE_BATCH_ID, batchId)
            .putLong(ChapterDownloadWorker.KEY_QUEUED_AT, queuedAt)
            .putString(ChapterDownloadWorker.KEY_QUEUE_KEY, queueKey)
            .build()

        return OneTimeWorkRequestBuilder<ChapterDownloadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setInputData(inputData)
            .addTag(ChapterDownloadWorker.DOWNLOAD_QUEUE_TAG)
            .addQueueTag(ChapterDownloadWorker.TAG_MANGA_NAME_PREFIX, mangaName)
            .addQueueTag(ChapterDownloadWorker.TAG_CHAPTER_NAME_PREFIX, chapter.chapterName)
            .addQueueTag(ChapterDownloadWorker.TAG_QUEUE_INDEX_PREFIX, queueIndex.toString())
            .addQueueTag(ChapterDownloadWorker.TAG_QUEUE_TOTAL_PREFIX, queueTotal.toString())
            .addQueueTag(ChapterDownloadWorker.TAG_QUEUE_BATCH_PREFIX, batchId)
            .addQueueTag(ChapterDownloadWorker.TAG_QUEUED_AT_PREFIX, queuedAt.toString())
            .addQueueTag(ChapterDownloadWorker.TAG_QUEUE_KEY_PREFIX, queueKey)
            .build()
    }

    private fun OneTimeWorkRequest.Builder.addQueueTag(
        prefix: String,
        value: String
    ): OneTimeWorkRequest.Builder {
        return addTag(ChapterDownloadWorker.queueMetadataTag(prefix, value))
    }

    private fun queueItem(workInfo: WorkInfo): DownloadQueueItem? {
        val tags = workInfo.tags
        val stateData = if (workInfo.state.isFinished) workInfo.outputData else workInfo.progress

        if (ChapterDownloadWorker.DOWNLOAD_QUEUE_TAG !in tags && workInfo.state.isFinished) {
            return null
        }

        val queueIndex = stateData.getInt(
            ChapterDownloadWorker.KEY_QUEUE_INDEX,
            tagValue(tags, ChapterDownloadWorker.TAG_QUEUE_INDEX_PREFIX)?.toIntOrNull() ?: 1
        )
        val queueTotal = stateData.getInt(
            ChapterDownloadWorker.KEY_QUEUE_TOTAL,
            tagValue(tags, ChapterDownloadWorker.TAG_QUEUE_TOTAL_PREFIX)?.toIntOrNull()
                ?: queueIndex
        ).coerceAtLeast(queueIndex)

        val item = DownloadQueueItem(
            workId = workInfo.id,
            queueKey = stateData.getString(ChapterDownloadWorker.KEY_QUEUE_KEY)
                ?: tagValue(tags, ChapterDownloadWorker.TAG_QUEUE_KEY_PREFIX)
                ?: workInfo.id.toString(),
            mangaName = stateData.getString(ChapterDownloadWorker.KEY_MANGA_NAME)
                ?: tagValue(tags, ChapterDownloadWorker.TAG_MANGA_NAME_PREFIX)
                ?: "Queued download",
            chapterName = stateData.getString(ChapterDownloadWorker.KEY_CHAPTER_NAME)
                ?: tagValue(tags, ChapterDownloadWorker.TAG_CHAPTER_NAME_PREFIX)
                ?: "Chapter",
            queueIndex = queueIndex,
            queueTotal = queueTotal,
            queuedAt = stateData.getLong(
                ChapterDownloadWorker.KEY_QUEUED_AT,
                tagValue(tags, ChapterDownloadWorker.TAG_QUEUED_AT_PREFIX)?.toLongOrNull() ?: 0L
            ),
            status = queueStatus(
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

    private fun queueStatus(
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

    private fun tagValue(tags: Set<String>, prefix: String): String? {
        return tags.firstOrNull { it.startsWith(prefix) }?.removePrefix(prefix)
    }

    private fun queueKey(mangaUrl: String, chapterUrl: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$mangaUrl\n$chapterUrl".toByteArray(StandardCharsets.UTF_8))

        return digest.joinToString("") { byte ->
            "%02x".format(byte.toInt() and 0xff)
        }.take(16)
    }
}
