package com.eren76.mangly.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.eren76.mangly.DownloadManager
import com.eren76.mangly.ExtensionManager
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
        val chapterUrl = inputData.getString(KEY_CHAPTER_URL) ?: return Result.failure()
        val extensionIdValue = inputData.getString(KEY_EXTENSION_ID) ?: return Result.failure()
        val downloadsDirectory = inputData.getString(KEY_DOWNLOADS_DIR) ?: DEFAULT_DOWNLOADS_DIR

        val extensionId = runCatching { UUID.fromString(extensionIdValue) }
            .getOrElse { return Result.failure() }

        val extensionEntry = deps.extensionDao().getById(extensionId) ?: return Result.failure()

        return try {
            val metadata = deps.extensionManager().extractExtensionMetadata(
                zipBytes = File(extensionEntry.filePath).readBytes(),
                context = applicationContext
            )

            deps.downloadManager().downloadChapter(
                mangaurl = mangaUrl,
                mangaName = mangaName,
                chapterUrl = chapterUrl,
                source = metadata.source,
                extensionId = extensionId,
                context = applicationContext,
                downloadsDirectory = downloadsDirectory
            )

            Result.success()
        } catch (_: IOException) {
            Result.retry()
        } catch (_: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val KEY_MANGA_URL = "manga_url"
        const val KEY_MANGA_NAME = "manga_name"
        const val KEY_CHAPTER_URL = "chapter_url"
        const val KEY_EXTENSION_ID = "extension_id"
        const val KEY_DOWNLOADS_DIR = "downloads_dir"
        const val DEFAULT_DOWNLOADS_DIR = "downloads"
    }
}


