package com.eren76.mangly

import android.content.Context
import com.eren76.mangly.rooms.dao.DownloadsDao
import com.eren76.mangly.rooms.entities.DownloadedChapterEntity
import com.eren76.mangly.rooms.entities.DownloadsEntity
import com.eren76.manglyextension.plugins.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.UUID
import javax.inject.Inject

class DownloadManager @Inject constructor(
    private val fileManager: FileManager,
    private val downloadsDao: DownloadsDao,
) {
    suspend fun downloadImage(
        imageUrl: String,
        headers: List<Source.Header>,
        context: Context
    ): ByteArray? {
        return try {
            withContext(Dispatchers.IO) {
                val client = OkHttpClient()
                val requestBuilder = Request.Builder().url(imageUrl)
                for (header in headers) {
                    requestBuilder.addHeader(header.name, header.value)
                }

                client.newCall(requestBuilder.build()).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    response.body?.bytes()
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun downloadChapter(
        mangaurl: String,
        mangaName: String,
        chapterUrl: String,
        source: Source,
        extensionId: UUID,
        context: Context,
        downloadsDirectory: String
    ) {
        return withContext(Dispatchers.IO) {
            val existingDownload = downloadsDao.getWithChaptersByMangaUrl(mangaurl)
            val id = existingDownload?.download?.downloadId ?: UUID.randomUUID()
            val existingChapterId = existingDownload?.chapters?.firstOrNull {
                it.chapterUrl == chapterUrl
            }?.id

            val chapterImages: Source.ChapterImages = source.getChapterImages(chapterUrl)

            val downloadEntity = DownloadsEntity(
                downloadId = id,
                mangaUrl = mangaurl,
                mangaName = mangaName,
                extensionId = extensionId
            )

            val chapterId = existingChapterId ?: UUID.randomUUID()
            val chapterEntity = DownloadedChapterEntity(
                id = chapterId,
                downloadId = id,
                chapterName = null,
                chapterUrl = chapterUrl,
                downloadedAt = null,
                filePath = null,
                isFullyDownloaded = false
            )

            downloadsDao.ensureDownloadAndInsertChapter(
                download = downloadEntity,
                chapter = chapterEntity
            )

            val chapterDir =
                "${downloadsDirectory}/${downloadEntity.downloadId}/${chapterEntity.id}"
            var savedImages = 0
            for (imageUrl in chapterImages.images) {
                val imageBytes = downloadImage(imageUrl, chapterImages.headers, context)
                if (imageBytes != null) {
                    val fileName =
                        "${mangaName}_${chapterUrl.hashCode()}_${imageUrl.hashCode()}.jpg"
                    fileManager.saveBytesToStorage(
                        context = context,
                        relativeDir = chapterDir,
                        fileName = fileName,
                        bytes = imageBytes
                    )

                    savedImages++
                }
            }

            val isFullyDownloaded = savedImages == chapterImages.images.size && savedImages > 0
            downloadsDao.updateDownloadedChapterState(
                chapterId = chapterId,
                isFullyDownloaded = isFullyDownloaded,
                filePath = chapterDir,
                downloadedAt = if (isFullyDownloaded) System.currentTimeMillis() else null
            )

        }
    }

}
