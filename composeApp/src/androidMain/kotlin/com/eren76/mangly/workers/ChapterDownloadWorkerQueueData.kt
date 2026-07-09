package com.eren76.mangly.workers

import androidx.work.Data
import com.eren76.mangly.downloads.models.DownloadWorkerRequest

internal object ChapterDownloadWorkerQueueData {
    fun progressData(
        request: DownloadWorkerRequest,
        status: String,
        message: String? = null
    ): Data {
        val builder = Data.Builder()
            .putString(ChapterDownloadWorker.KEY_MANGA_NAME, request.mangaName)
            .putString(ChapterDownloadWorker.KEY_CHAPTER_NAME, request.chapterName)
            .putString(ChapterDownloadWorker.KEY_CHAPTER_URL, request.chapterUrl)
            .putInt(ChapterDownloadWorker.KEY_QUEUE_INDEX, request.queueIndex)
            .putInt(ChapterDownloadWorker.KEY_QUEUE_TOTAL, request.queueTotal)
            .putString(ChapterDownloadWorker.KEY_QUEUE_BATCH_ID, request.queueBatchId)
            .putLong(ChapterDownloadWorker.KEY_QUEUED_AT, request.queuedAt)
            .putString(ChapterDownloadWorker.KEY_QUEUE_KEY, request.queueKey)
            .putString(ChapterDownloadWorker.KEY_QUEUE_STATUS, status)

        message?.takeIf { it.isNotBlank() }?.let { value ->
            builder.putString(
                ChapterDownloadWorker.KEY_QUEUE_MESSAGE,
                value.take(MAX_QUEUE_MESSAGE_LENGTH)
            )
        }

        return builder.build()
    }

    // WorkManager merges prerequisite output into the next worker's input.
    // Result data must not repeat request keys or it can overwrite the next chapter.
    fun resultData(
        status: String,
        message: String? = null
    ): Data {
        val builder = Data.Builder()
            .putString(ChapterDownloadWorker.KEY_QUEUE_STATUS, status)

        message?.takeIf { it.isNotBlank() }?.let { value ->
            builder.putString(
                ChapterDownloadWorker.KEY_QUEUE_MESSAGE,
                value.take(MAX_QUEUE_MESSAGE_LENGTH)
            )
        }

        return builder.build()
    }

    fun errorMessage(error: Throwable): String {
        val message = error.message
            ?.takeIf { it.isNotBlank() }
            ?: error::class.java.simpleName

        return "${error::class.java.simpleName}: $message".take(MAX_QUEUE_MESSAGE_LENGTH)
    }

    private const val MAX_QUEUE_MESSAGE_LENGTH = 500
}
