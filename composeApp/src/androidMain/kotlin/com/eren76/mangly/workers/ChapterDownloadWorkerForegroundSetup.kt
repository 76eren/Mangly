package com.eren76.mangly.workers

import android.content.Context
import android.util.Log
import androidx.work.Data
import androidx.work.ForegroundInfo
import com.eren76.mangly.downloads.models.DownloadWorkerRequest

internal class ChapterDownloadWorkerForegroundSetup(
    private val applicationContext: Context,
    private val notifications: ChapterDownloadWorkerNotifications,
    private val workIdProvider: () -> String,
    private val setForegroundInfo: suspend (ForegroundInfo) -> Unit,
    private val setProgressData: suspend (Data) -> Unit
) {
    suspend fun prepareForegroundExecution(request: DownloadWorkerRequest) {
        val workId = workIdProvider()
        if (!ChapterDownloadWorkerForegroundPolicy.canStartForegroundExecution(applicationContext)) {
            Log.i(
                TAG,
                "App is backgrounded and battery optimized for work id=$workId, " +
                        "continuing without foreground service"
            )
            notifications.updateProgressNotification(
                mangaName = request.mangaName,
                progressChapter = request.queueIndex,
                totalChapters = request.queueTotal
            )
            return
        }

        runCatching {
            setForegroundInfo(
                notifications.createForegroundInfo(
                    mangaName = request.mangaName,
                    progressChapter = request.queueIndex,
                    totalChapters = request.queueTotal
                )
            )
        }.onFailure { error ->
            Log.w(TAG, "Foreground setup failed for work id=$workId", error)
            val message = if (ChapterDownloadWorkerForegroundPolicy.isForegroundServiceStartDenied(error)) {
                "Foreground service start was denied because the app is backgrounded"
            } else {
                "Foreground notification unavailable: ${
                    ChapterDownloadWorkerQueueData.errorMessage(error)
                }"
            }
            runCatching {
                setProgressData(
                    ChapterDownloadWorkerQueueData.progressData(
                        request = request,
                        status = ChapterDownloadWorker.STATUS_RUNNING,
                        message = message
                    )
                )
            }
            notifications.updateProgressNotification(
                mangaName = request.mangaName,
                progressChapter = request.queueIndex,
                totalChapters = request.queueTotal
            )
        }
    }

    private companion object {
        const val TAG = "ChapterDownloadWorker"
    }
}
