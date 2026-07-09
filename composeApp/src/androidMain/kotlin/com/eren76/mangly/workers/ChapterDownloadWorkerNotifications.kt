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
import androidx.work.ForegroundInfo
import com.eren76.mangly.R
import com.eren76.mangly.permissions.NotificationPermissionHandling

internal class ChapterDownloadWorkerNotifications(
    private val applicationContext: Context
) {
    fun createForegroundInfo(
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

    fun updateProgressNotification(
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

    fun showFinalNotification(
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

    fun createNotificationChannelIfNeeded() {
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

    private fun cancelQueuePendingIntent(): PendingIntent {
        val intent = Intent(applicationContext, CancelDownloadReceiver::class.java).apply {
            action = CancelDownloadReceiver.ACTION_CANCEL_DOWNLOADS
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(applicationContext, 0, intent, flags)
    }

    private fun notificationIdForWork(): Int {
        return DOWNLOAD_QUEUE_NOTIFICATION_ID
    }

    private companion object {
        const val CHANNEL_ID = "chapter_downloads"
        const val CHANNEL_NAME = "Chapter downloads"
        const val DOWNLOAD_QUEUE_NOTIFICATION_ID = 1001
    }
}
