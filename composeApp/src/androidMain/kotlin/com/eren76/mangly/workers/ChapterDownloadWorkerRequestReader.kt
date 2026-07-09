package com.eren76.mangly.workers

import androidx.work.Data
import com.eren76.mangly.downloads.models.DownloadWorkerRequest

internal object ChapterDownloadWorkerRequestReader {
    fun readFromInputData(inputData: Data): DownloadWorkerRequest {
        val mangaUrl = inputData.getString(ChapterDownloadWorker.KEY_MANGA_URL).orEmpty()
        val mangaName = inputData.getString(ChapterDownloadWorker.KEY_MANGA_NAME).orFallback("Manga")
        val mangaSummary = inputData.getString(ChapterDownloadWorker.KEY_MANGA_SUMMARY).orEmpty()
        val chapterUrl = inputData.getString(ChapterDownloadWorker.KEY_CHAPTER_URL).orEmpty()
        val chapterName =
            inputData.getString(ChapterDownloadWorker.KEY_CHAPTER_NAME).orFallback("Chapter")
        val extensionIdValue = inputData.getString(ChapterDownloadWorker.KEY_EXTENSION_ID).orEmpty()
        val downloadsDirectory = inputData.getString(ChapterDownloadWorker.KEY_DOWNLOADS_DIR)
            ?: ChapterDownloadWorker.DEFAULT_DOWNLOADS_DIR
        val queueIndex = inputData.getInt(ChapterDownloadWorker.KEY_QUEUE_INDEX, 1).coerceAtLeast(1)
        val queueTotal = inputData.getInt(
            ChapterDownloadWorker.KEY_QUEUE_TOTAL,
            1
        ).coerceAtLeast(queueIndex)
        val queueBatchId = inputData.getString(ChapterDownloadWorker.KEY_QUEUE_BATCH_ID).orEmpty()
        val queuedAt = inputData.getLong(ChapterDownloadWorker.KEY_QUEUED_AT, 0L)
        val queueKey = inputData.getString(ChapterDownloadWorker.KEY_QUEUE_KEY).orEmpty()

        val missingFields = buildList {
            if (mangaUrl.isBlank()) add(ChapterDownloadWorker.KEY_MANGA_URL)
            if (chapterUrl.isBlank()) add(ChapterDownloadWorker.KEY_CHAPTER_URL)
            if (extensionIdValue.isBlank()) add(ChapterDownloadWorker.KEY_EXTENSION_ID)
        }

        return DownloadWorkerRequest(
            mangaUrl = mangaUrl,
            mangaName = mangaName,
            mangaSummary = mangaSummary,
            chapterUrl = chapterUrl,
            chapterName = chapterName,
            extensionIdValue = extensionIdValue,
            downloadsDirectory = downloadsDirectory,
            queueIndex = queueIndex,
            queueTotal = queueTotal,
            queueBatchId = queueBatchId,
            queuedAt = queuedAt,
            queueKey = queueKey,
            missingFields = missingFields
        )
    }

    private fun String?.orFallback(fallback: String): String {
        return this?.takeIf { it.isNotBlank() } ?: fallback
    }
}
