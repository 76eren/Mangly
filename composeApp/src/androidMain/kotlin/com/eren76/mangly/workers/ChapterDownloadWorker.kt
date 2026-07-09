package com.eren76.mangly.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.eren76.mangly.downloads.DownloadStorage
import com.eren76.mangly.downloads.models.DownloadWorkerRequest
import com.eren76.mangly.rooms.entities.ExtensionEntity
import com.eren76.manglyextension.plugins.ExtensionMetadata
import kotlinx.coroutines.CancellationException
import java.io.File
import java.io.IOException
import java.util.UUID

private const val TAG = "ChapterDownloadWorker"
private const val MAX_TRANSIENT_RETRIES = 2

class ChapterDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_MANGA_URL = "manga_url"
        const val KEY_MANGA_NAME = "manga_name"
        const val KEY_MANGA_SUMMARY = "manga_summary"
        const val KEY_CHAPTER_URL = "chapter_url"
        const val KEY_CHAPTER_NAME = "chapter_name"
        const val KEY_EXTENSION_ID = "extension_id"
        const val KEY_DOWNLOADS_DIR = "downloads_dir"
        const val DEFAULT_DOWNLOADS_DIR = DownloadStorage.DOWNLOADS_DIRECTORY
        const val KEY_QUEUE_INDEX = "queue_index"
        const val KEY_QUEUE_TOTAL = "queue_total"
        const val KEY_QUEUE_BATCH_ID = "queue_batch_id"
        const val KEY_QUEUED_AT = "queued_at"
        const val KEY_QUEUE_KEY = "queue_key"
        const val KEY_QUEUE_STATUS = "queue_status"
        const val KEY_QUEUE_MESSAGE = "queue_message"

        const val STATUS_RUNNING = "running"
        const val STATUS_RETRYING = "retrying"
        const val STATUS_SUCCEEDED = "succeeded"
        const val STATUS_FAILED = "failed"

        // Shared with DownloadsViewModel so both refer to the same unique work chain
        const val DOWNLOAD_QUEUE_WORK_NAME = "chapter_download_queue"
        const val DOWNLOAD_QUEUE_TAG = "mangly_download_queue"
        const val TAG_MANGA_NAME_PREFIX = "mangly_queue:manga_name="
        const val TAG_CHAPTER_NAME_PREFIX = "mangly_queue:chapter_name="
        const val TAG_QUEUE_INDEX_PREFIX = "mangly_queue:index="
        const val TAG_QUEUE_TOTAL_PREFIX = "mangly_queue:total="
        const val TAG_QUEUE_BATCH_PREFIX = "mangly_queue:batch="
        const val TAG_QUEUED_AT_PREFIX = "mangly_queue:queued_at="
        const val TAG_QUEUE_KEY_PREFIX = "mangly_queue:key="

        private const val MAX_QUEUE_TAG_VALUE_LENGTH = 120
        private val queueTagWhitespace = Regex("\\s+")

        fun queueMetadataTag(prefix: String, value: String): String {
            val normalized = value.replace(queueTagWhitespace, " ").trim()
            return prefix + normalized.take(MAX_QUEUE_TAG_VALUE_LENGTH)
        }
    }

    override suspend fun doWork(): Result {
        val request: DownloadWorkerRequest =
            ChapterDownloadWorkerRequestReader.readFromInputData(inputData)
        Log.i(
            TAG,
            "Starting work id=$id chapter=${request.queueIndex}/${request.queueTotal} " +
                    "name=${request.chapterName} attempt=$runAttemptCount"
        )

        if (request.missingFields.isNotEmpty()) {
            val message = "Download request missing ${request.missingFields.joinToString(", ")}"
            Log.e(TAG, "Rejecting work id=$id: $message")
            return failedResult(message)
        }

        val extensionId: UUID = runCatching { UUID.fromString(request.extensionIdValue) }
            .getOrElse {
                Log.e(TAG, "Rejecting work id=$id: invalid extension id")
                return failedResult("Invalid extension id for this download")
            }

        notifications.createNotificationChannelIfNeeded()
        setProgress(
            ChapterDownloadWorkerQueueData.progressData(
                request = request,
                status = STATUS_RUNNING
            )
        )
        foregroundSetup.prepareForegroundExecution(request)

        val extensionEntry: ExtensionEntity = dependencies.extensionDao().getById(extensionId)
            ?: run {
                Log.e(TAG, "Extension $extensionId missing for work id=$id")
                return failedResult("Extension is no longer installed")
            }

        return try {
            val metadata: ExtensionMetadata =
                dependencies.extensionManager().extractExtensionMetadata(
                    zipBytes = File(extensionEntry.filePath).readBytes(),
                    context = applicationContext
                )

            notifications.updateProgressNotification(
                mangaName = request.mangaName,
                progressChapter = request.queueIndex,
                totalChapters = request.queueTotal
            )

            Log.i(TAG, "Downloading work id=$id chapter=${request.chapterName}")
            dependencies.downloadManager().downloadChapter(
                mangaurl = request.mangaUrl,
                mangaName = request.mangaName,
                mangaSummary = request.mangaSummary,
                chapterUrl = request.chapterUrl,
                chapterName = request.chapterName,
                source = metadata.source,
                extensionId = extensionId,
                context = applicationContext,
                downloadsDirectory = request.downloadsDirectory,
            )

            notifications.showFinalNotification(
                mangaName = request.mangaName,
                isSuccess = true,
                finishedChapters = request.queueIndex,
                totalChapters = request.queueTotal
            )
            Log.i(TAG, "Completed work id=$id chapter=${request.chapterName}")
            Result.success(
                ChapterDownloadWorkerQueueData.resultData(status = STATUS_SUCCEEDED)
            )
        } catch (error: CancellationException) {
            Log.i(TAG, "Cancelled work id=$id chapter=${request.chapterName}")
            notifications.showFinalNotification(
                mangaName = request.mangaName,
                isSuccess = false,
                finishedChapters = (request.queueIndex - 1).coerceAtLeast(0),
                totalChapters = request.queueTotal
            )
            throw error
        } catch (error: IOException) {
            handleDownloadFailure(request = request, error = error, canRetry = true)
        } catch (error: Exception) {
            handleDownloadFailure(request = request, error = error, canRetry = false)
        }
    }

    private val dependencies: ChapterDownloadWorkerDependencies by lazy {
        ChapterDownloadWorkerDependencyProvider.from(applicationContext)
    }

    private val notifications: ChapterDownloadWorkerNotifications by lazy {
        ChapterDownloadWorkerNotifications(applicationContext)
    }

    private val foregroundSetup: ChapterDownloadWorkerForegroundSetup by lazy {
        ChapterDownloadWorkerForegroundSetup(
            applicationContext = applicationContext,
            notifications = notifications,
            workIdProvider = { id.toString() },
            setForegroundInfo = { foregroundInfo -> setForeground(foregroundInfo) },
            setProgressData = { data -> setProgress(data) }
        )
    }


    private suspend fun handleDownloadFailure(
        request: DownloadWorkerRequest,
        error: Exception,
        canRetry: Boolean
    ): Result {
        val message = ChapterDownloadWorkerQueueData.errorMessage(error)
        if (canRetry && shouldRetryTransientFailure()) {
            Log.w(
                TAG,
                "Retrying work id=$id chapter=${request.chapterName} attempt=$runAttemptCount",
                error
            )
            runCatching {
                setProgress(
                    ChapterDownloadWorkerQueueData.progressData(
                        request = request,
                        status = STATUS_RETRYING,
                        message = message
                    )
                )
            }
            return Result.retry()
        }

        Log.e(TAG, "Download failed for work id=$id chapter=${request.chapterName}", error)
        notifications.showFinalNotification(
            mangaName = request.mangaName,
            isSuccess = false,
            finishedChapters = (request.queueIndex - 1).coerceAtLeast(0),
            totalChapters = request.queueTotal
        )
        return failedResult(message)
    }

    private fun failedResult(message: String): Result {
        return Result.success(
            ChapterDownloadWorkerQueueData.resultData(
                status = STATUS_FAILED,
                message = message
            )
        )
    }

    private fun shouldRetryTransientFailure(): Boolean {
        return runAttemptCount < MAX_TRANSIENT_RETRIES
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        notifications.createNotificationChannelIfNeeded()
        return notifications.createForegroundInfo(
            mangaName = inputData.getString(KEY_MANGA_NAME) ?: "Manga",
            progressChapter = inputData.getInt(KEY_QUEUE_INDEX, 1),
            totalChapters = inputData.getInt(KEY_QUEUE_TOTAL, 1)
        )
    }
}