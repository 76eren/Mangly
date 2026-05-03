package com.eren76.mangly.workers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.WorkManager


class CancelDownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_CANCEL_DOWNLOADS) {
            WorkManager.getInstance(context.applicationContext)
                .cancelUniqueWork(ChapterDownloadWorker.DOWNLOAD_QUEUE_WORK_NAME)
        }
    }

    companion object {
        const val ACTION_CANCEL_DOWNLOADS = "com.eren76.mangly.ACTION_CANCEL_DOWNLOADS"
    }
}
