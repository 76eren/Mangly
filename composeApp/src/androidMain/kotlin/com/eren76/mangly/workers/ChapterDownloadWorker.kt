package com.eren76.mangly.workers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.eren76.mangly.DownloadManager
import com.eren76.mangly.ExtensionManager
import com.eren76.mangly.R
import com.eren76.mangly.permissions.NotificationPermissionHandling
import com.eren76.mangly.rooms.dao.ExtensionDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
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
        val mangaUrl = inputData.getString(KEY_MANGA_URL) ?: return Result.failure()
        val mangaName = inputData.getString(KEY_MANGA_NAME) ?: return Result.failure()
        val mangaSummary = inputData.getString(KEY_MANGA_SUMMARY).orEmpty()
        val chapterUrl = inputData.getString(KEY_CHAPTER_URL) ?: return Result.failure()
        val chapterName = inputData.getString(KEY_CHAPTER_NAME).orEmpty()
        val extensionIdValue = inputData.getString(KEY_EXTENSION_ID) ?: return Result.failure()
        val downloadsDirectory = inputData.getString(KEY_DOWNLOADS_DIR) ?: DEFAULT_DOWNLOADS_DIR
        val queueIndex = inputData.getInt(KEY_QUEUE_INDEX, 1).coerceAtLeast(1)
        val queueTotal = inputData.getInt(KEY_QUEUE_TOTAL, 1).coerceAtLeast(queueIndex)

        val extensionId = runCatching { UUID.fromString(extensionIdValue) }
            .getOrElse { return Result.failure() }

        val extensionEntry = deps.extensionDao().getById(extensionId) ?: return Result.failure()

        createNotificationChannelIfNeeded()
        setForeground(
            createForegroundInfo(
                mangaName = mangaName,
                progressChapter = queueIndex,
                totalChapters = queueTotal
            )
        )

        return try {
            val metadata = deps.extensionManager().extractExtensionMetadata(
                zipBytes = File(extensionEntry.filePath).readBytes(),
                context = applicationContext
            )

            updateProgressNotification(
                mangaName = mangaName,
                progressChapter = queueIndex,
                totalChapters = queueTotal
            )

            deps.downloadManager().downloadChapter(
                mangaurl = mangaUrl,
                mangaName = mangaName,
                mangaSummary = mangaSummary,
                chapterUrl = chapterUrl,
                chapterName = chapterName,
                source = metadata.source,
                extensionId = extensionId,
                context = applicationContext,
                downloadsDirectory = downloadsDirectory,
            )

            showFinalNotification(
                mangaName = mangaName,
                isSuccess = true,
                finishedChapters = queueIndex,
                totalChapters = queueTotal
            )
            Result.success()
        } catch (_: IOException) {
            showFinalNotification(
                mangaName = mangaName,
                isSuccess = false,
                finishedChapters = (queueIndex - 1).coerceAtLeast(0),
                totalChapters = queueTotal
            )
            Result.retry()
        } catch (_: Exception) {
            showFinalNotification(
                mangaName = mangaName,
                isSuccess = false,
                finishedChapters = (queueIndex - 1).coerceAtLeast(0),
                totalChapters = queueTotal
            )
            Result.failure()
        }
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

        if (totalChapters > 0) {
            val clampedProgress = progressChapter.coerceIn(0, totalChapters)
            builder
                .setContentText("Progress $clampedProgress/$totalChapters chapters")
                .setProgress(totalChapters, clampedProgress, false)
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
            .setContentText(if (isSuccess) progressText else "$progressText (interrupted)")
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
        const val DEFAULT_DOWNLOADS_DIR = "downloads"
        const val KEY_QUEUE_INDEX = "queue_index"
        const val KEY_QUEUE_TOTAL = "queue_total"

        private const val CHANNEL_ID = "chapter_downloads"
        private const val CHANNEL_NAME = "Chapter downloads"
        private const val DOWNLOAD_QUEUE_NOTIFICATION_ID = 1001
    }
}
