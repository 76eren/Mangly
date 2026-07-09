package com.eren76.mangly.workers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.eren76.mangly.ExtensionManager
import com.eren76.mangly.R
import com.eren76.mangly.downloads.DownloadManager
import com.eren76.mangly.downloads.DownloadStorage
import com.eren76.mangly.downloads.models.DownloadWorkerRequest
import com.eren76.mangly.permissions.NotificationPermissionHandling
import com.eren76.mangly.rooms.dao.ExtensionDao
import com.eren76.mangly.rooms.entities.ExtensionEntity
import com.eren76.manglyextension.plugins.ExtensionMetadata
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import java.io.File
import java.io.IOException
import java.util.UUID

class ChapterDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerDependencies {
        fun downloadManager(): DownloadManager
        fun extensionDao(): ExtensionDao
        fun extensionManager(): ExtensionManager
    }

    private val deps: WorkerDependencies by lazy {
        EntryPointAccessors.fromApplication(applicationContext, WorkerDependencies::class.java)
    }

    override suspend fun doWork(): Result {
        val request: DownloadWorkerRequest = readDownloadRequest()

        if (request.missingFields.isNotEmpty()) {
            return Result.success(
                queueStateData(
                    request = request,
                    status = STATUS_FAILED,
                    message = "Download request missing ${request.missingFields.joinToString(", ")}"
                )
            )
        }

        val extensionId: UUID = runCatching { UUID.fromString(request.extensionIdValue) }
            .getOrElse {
                return Result.success(
                    queueStateData(
                        request = request,
                        status = STATUS_FAILED,
                        message = "Invalid extension id for this download"
                    )
                )
            }

        createNotificationChannelIfNeeded()
        runCatching {
            setProgress(queueStateData(request = request, status = STATUS_RUNNING))
            setForeground(
                createForegroundInfo(
                    mangaName = request.mangaName,
                    progressChapter = request.queueIndex,
                    totalChapters = request.queueTotal
                )
            )
        }.onFailure { error ->
            if (shouldRetryTransientFailure()) {
                return Result.retry()
            }

            runCatching {
                setProgress(
                    queueStateData(
                        request = request,
                        status = STATUS_RUNNING,
                        message = "Foreground notification unavailable: ${queueErrorMessage(error)}"
                    )
                )
            }
        }

        val extensionEntry: ExtensionEntity = deps.extensionDao().getById(extensionId)
            ?: return Result.success(
                queueStateData(
                    request = request,
                    status = STATUS_FAILED,
                    message = "Extension is no longer installed"
                )
            )

        return try {
            val metadata: ExtensionMetadata = deps.extensionManager().extractExtensionMetadata(
                zipBytes = File(extensionEntry.filePath).readBytes(),
                context = applicationContext
            )

            updateProgressNotification(
                mangaName = request.mangaName,
                progressChapter = request.queueIndex,
                totalChapters = request.queueTotal
            )

            deps.downloadManager().downloadChapter(
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

            showFinalNotification(
                mangaName = request.mangaName,
                isSuccess = true,
                finishedChapters = request.queueIndex,
                totalChapters = request.queueTotal
            )
            Result.success(
                queueStateData(
                    request = request,
                    status = STATUS_SUCCEEDED
                )
            )
        } catch (error: CancellationException) {
            showFinalNotification(
                mangaName = request.mangaName,
                isSuccess = false,
                finishedChapters = (request.queueIndex - 1).coerceAtLeast(0),
                totalChapters = request.queueTotal
            )
            throw error
        } catch (error: IOException) {
            val message = queueErrorMessage(error)
            if (shouldRetryTransientFailure()) {
                runCatching {
                    setProgress(
                        queueStateData(
                            request = request,
                            status = STATUS_RETRYING,
                            message = message
                        )
                    )
                }
                return Result.retry()
            }

            showFinalNotification(
                mangaName = request.mangaName,
                isSuccess = false,
                finishedChapters = (request.queueIndex - 1).coerceAtLeast(0),
                totalChapters = request.queueTotal
            )
            Result.success(
                queueStateData(
                    request = request,
                    status = STATUS_FAILED,
                    message = message
                )
            )
        } catch (error: Exception) {
            val message = queueErrorMessage(error)
            showFinalNotification(
                mangaName = request.mangaName,
                isSuccess = false,
                finishedChapters = (request.queueIndex - 1).coerceAtLeast(0),
                totalChapters = request.queueTotal
            )
            Result.success(
                queueStateData(
                    request = request,
                    status = STATUS_FAILED,
                    message = message
                )
            )
        }
    }


    private fun readDownloadRequest(): DownloadWorkerRequest {
        val mangaUrl = inputData.getString(KEY_MANGA_URL).orEmpty()
        val mangaName = inputData.getString(KEY_MANGA_NAME).orFallback("Manga")
        val mangaSummary = inputData.getString(KEY_MANGA_SUMMARY).orEmpty()
        val chapterUrl = inputData.getString(KEY_CHAPTER_URL).orEmpty()
        val chapterName = inputData.getString(KEY_CHAPTER_NAME).orFallback("Chapter")
        val extensionIdValue = inputData.getString(KEY_EXTENSION_ID).orEmpty()
        val downloadsDirectory = inputData.getString(KEY_DOWNLOADS_DIR) ?: DEFAULT_DOWNLOADS_DIR
        val queueIndex = inputData.getInt(KEY_QUEUE_INDEX, 1).coerceAtLeast(1)
        val queueTotal = inputData.getInt(KEY_QUEUE_TOTAL, 1).coerceAtLeast(queueIndex)
        val queueBatchId = inputData.getString(KEY_QUEUE_BATCH_ID).orEmpty()
        val queuedAt = inputData.getLong(KEY_QUEUED_AT, 0L)
        val queueKey = inputData.getString(KEY_QUEUE_KEY).orEmpty()

        val missingFields = buildList {
            if (mangaUrl.isBlank()) add(KEY_MANGA_URL)
            if (chapterUrl.isBlank()) add(KEY_CHAPTER_URL)
            if (extensionIdValue.isBlank()) add(KEY_EXTENSION_ID)
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

    private fun queueStateData(
        request: DownloadWorkerRequest,
        status: String,
        message: String? = null
    ): Data {
        val builder = Data.Builder()
            .putString(KEY_MANGA_NAME, request.mangaName)
            .putString(KEY_CHAPTER_NAME, request.chapterName)
            .putString(KEY_CHAPTER_URL, request.chapterUrl)
            .putInt(KEY_QUEUE_INDEX, request.queueIndex)
            .putInt(KEY_QUEUE_TOTAL, request.queueTotal)
            .putString(KEY_QUEUE_BATCH_ID, request.queueBatchId)
            .putLong(KEY_QUEUED_AT, request.queuedAt)
            .putString(KEY_QUEUE_KEY, request.queueKey)
            .putString(KEY_QUEUE_STATUS, status)

        message?.takeIf { it.isNotBlank() }?.let { value ->
            builder.putString(KEY_QUEUE_MESSAGE, value.take(MAX_QUEUE_MESSAGE_LENGTH))
        }

        return builder.build()
    }

    private fun String?.orFallback(fallback: String): String {
        return this?.takeIf { it.isNotBlank() } ?: fallback
    }

    private fun shouldRetryTransientFailure(): Boolean {
        return runAttemptCount < MAX_TRANSIENT_RETRIES
    }

    private fun queueErrorMessage(error: Throwable): String {
        val message = error.message
            ?.takeIf { it.isNotBlank() }
            ?: error::class.java.simpleName

        return "${error::class.java.simpleName}: $message".take(MAX_QUEUE_MESSAGE_LENGTH)
    }

    private fun cancelQueuePendingIntent(): PendingIntent {
        val intent = Intent(applicationContext, CancelDownloadReceiver::class.java).apply {
            action = CancelDownloadReceiver.ACTION_CANCEL_DOWNLOADS
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(applicationContext, 0, intent, flags)
    }

    private fun createForegroundInfo(
        mangaName: String,
        progressChapter: Int,
        totalChapters: Int
    ): ForegroundInfo {
        val notification = buildProgressNotification(mangaName, progressChapter, totalChapters)
        val notificationId = notificationIdForWork()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    private fun buildProgressNotification(
        mangaName: String,
        progressChapter: Int,
        totalChapters: Int
    ): Notification {
        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.icon)
            .setContentTitle("Downloading $mangaName")
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Cancel queue",
                cancelQueuePendingIntent()
            )

        if (totalChapters > 0) {
            val clampedProgress = progressChapter.coerceIn(0, totalChapters)
            builder
                .setContentText("Chapter $clampedProgress/$totalChapters")
                .setProgress(totalChapters, (clampedProgress - 1).coerceAtLeast(0), false)
        } else {
            builder
                .setContentText("Preparing download...")
                .setProgress(0, 0, true)
        }

        return builder.build()
    }

    private fun updateProgressNotification(
        mangaName: String,
        progressChapter: Int,
        totalChapters: Int
    ) {
        if (!NotificationPermissionHandling.canPostNotifications(applicationContext)) return

        val notification = buildProgressNotification(mangaName, progressChapter, totalChapters)
        val manager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationIdForWork(), notification)
    }

    private fun showFinalNotification(
        mangaName: String,
        isSuccess: Boolean,
        finishedChapters: Int,
        totalChapters: Int
    ) {
        if (!NotificationPermissionHandling.canPostNotifications(applicationContext)) return

        val progressText = if (totalChapters > 0) {
            val clampedFinished = finishedChapters.coerceIn(0, totalChapters)
            "$clampedFinished/$totalChapters chapters finished downloading"
        } else {
            "Download interrupted"
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.icon)
            .setContentTitle(mangaName)
            .setContentText(if (isSuccess) progressText else "$progressText, one chapter failed")
            .setOngoing(false)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val manager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationIdForWork(), notification)
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows ongoing manga chapter download progress"
        }

        manager.createNotificationChannel(channel)
    }

    private fun notificationIdForWork(): Int {
        return DOWNLOAD_QUEUE_NOTIFICATION_ID
    }

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

        private const val CHANNEL_ID = "chapter_downloads"
        private const val CHANNEL_NAME = "Chapter downloads"
        private const val DOWNLOAD_QUEUE_NOTIFICATION_ID = 1001
        private const val MAX_TRANSIENT_RETRIES = 2
        private const val MAX_QUEUE_MESSAGE_LENGTH = 500
        private const val MAX_QUEUE_TAG_VALUE_LENGTH = 120
        private val queueTagWhitespace = Regex("\\s+")

        fun queueMetadataTag(prefix: String, value: String): String {
            val normalized = value.replace(queueTagWhitespace, " ").trim()
            return prefix + normalized.take(MAX_QUEUE_TAG_VALUE_LENGTH)
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        createNotificationChannelIfNeeded()
        return createForegroundInfo(
            mangaName = inputData.getString(KEY_MANGA_NAME) ?: "Manga",
            progressChapter = inputData.getInt(KEY_QUEUE_INDEX, 1),
            totalChapters = inputData.getInt(KEY_QUEUE_TOTAL, 1)
        )
    }
}
