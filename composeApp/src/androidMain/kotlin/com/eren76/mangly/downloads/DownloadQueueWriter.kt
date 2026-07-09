package com.eren76.mangly.downloads

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.eren76.mangly.downloads.models.DownloadChapterQueueRequest
import com.eren76.mangly.downloads.models.DownloadQueueItem
import com.eren76.mangly.workers.ChapterDownloadWorker
import com.eren76.manglyextension.plugins.ExtensionMetadata
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit

internal object DownloadQueueWriter {
    fun enqueueChapterDownloads(
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
            .filterNot { chapter ->
                queueKeyForChapter(mangaUrl, chapter.chapterUrl) in activeQueueKeys
            }

        if (uniqueChapters.isEmpty()) return 0

        val batchId = UUID.randomUUID().toString()
        val queuedAt = System.currentTimeMillis()
        val queueTotal = uniqueChapters.size
        val requests: List<OneTimeWorkRequest> = uniqueChapters.mapIndexed { index, chapter ->
            buildChapterDownloadWorkRequest(
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

        Log.i(
            TAG,
            "Enqueuing batch=$batchId chapters=${requests.size} manga=$mangaName"
        )
        requests.forEachIndexed { index, request ->
            Log.d(
                TAG,
                "Queued work id=${request.id} chapter=${index + 1}/${requests.size} " +
                        "name=${uniqueChapters[index].chapterName}"
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
            .enqueue()

        return requests.size
    }

    fun cancelDownloadQueue(context: Context) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(ChapterDownloadWorker.DOWNLOAD_QUEUE_WORK_NAME)
    }

    private fun buildChapterDownloadWorkRequest(
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
        val queueKey = queueKeyForChapter(mangaUrl, chapter.chapterUrl)
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
            .addQueueMetadataTag(ChapterDownloadWorker.TAG_MANGA_NAME_PREFIX, mangaName)
            .addQueueMetadataTag(ChapterDownloadWorker.TAG_CHAPTER_NAME_PREFIX, chapter.chapterName)
            .addQueueMetadataTag(ChapterDownloadWorker.TAG_QUEUE_INDEX_PREFIX, queueIndex.toString())
            .addQueueMetadataTag(ChapterDownloadWorker.TAG_QUEUE_TOTAL_PREFIX, queueTotal.toString())
            .addQueueMetadataTag(ChapterDownloadWorker.TAG_QUEUE_BATCH_PREFIX, batchId)
            .addQueueMetadataTag(ChapterDownloadWorker.TAG_QUEUED_AT_PREFIX, queuedAt.toString())
            .addQueueMetadataTag(ChapterDownloadWorker.TAG_QUEUE_KEY_PREFIX, queueKey)
            .build()
    }

    private fun OneTimeWorkRequest.Builder.addQueueMetadataTag(
        prefix: String,
        value: String
    ): OneTimeWorkRequest.Builder {
        return addTag(ChapterDownloadWorker.queueMetadataTag(prefix, value))
    }

    private fun queueKeyForChapter(mangaUrl: String, chapterUrl: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$mangaUrl\n$chapterUrl".toByteArray(StandardCharsets.UTF_8))

        return digest.joinToString("") { byte ->
            "%02x".format(byte.toInt() and 0xff)
        }.take(16)
    }

    private const val TAG = "DownloadQueue"
}
