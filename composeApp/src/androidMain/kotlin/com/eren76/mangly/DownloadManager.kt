package com.eren76.mangly

import android.content.Context
import com.eren76.mangly.composables.shared.image.CoverCache
import com.eren76.mangly.rooms.dao.DownloadsDao
import com.eren76.mangly.rooms.entities.DownloadedChapterEntity
import com.eren76.mangly.rooms.entities.DownloadsEntity
import com.eren76.mangly.rooms.relations.DownloadWithChapters
import com.eren76.mangly.viewmodels.DownloadsViewModel
import com.eren76.manglyextension.plugins.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class DownloadManager @Inject constructor(
    private val fileManager: FileManager,
    private val downloadsDao: DownloadsDao,
) {
    private val coverDir = DownloadsViewModel.DOWNLOADS_COVERS_DIRECTORY

    suspend fun downloadImage(
        imageUrl: String,
        headers: List<Source.Header>
    ): ByteArray? {
        return runCatching { CoverCache.downloadImage(imageUrl, headers)?.bytes }.getOrNull()
    }

    suspend fun downloadChapter(
        mangaurl: String,
        mangaName: String,
        mangaSummary: String,
        chapterUrl: String,
        source: Source,
        extensionId: UUID,
        context: Context,
        downloadsDirectory: String,
        chapterName: String,
    ) {
        return withContext(Dispatchers.IO) {
            val existingDownload: DownloadWithChapters? =
                downloadsDao.getWithChaptersByMangaUrl(mangaurl)
            val id = existingDownload?.download?.downloadId ?: UUID.randomUUID()
            val existingChapterId = existingDownload?.chapters?.firstOrNull {
                it.chapterUrl == chapterUrl
            }?.id

            val chapterImages: Source.ChapterImages = source.getChapterImages(chapterUrl)
            val normalizedSummary = mangaSummary.takeIf { it.isNotBlank() }

            val downloadEntity = DownloadsEntity(
                downloadId = id,
                mangaUrl = mangaurl,
                mangaName = mangaName,
                mangaSummary = normalizedSummary ?: existingDownload?.download?.mangaSummary,
                coverImageFilename = existingDownload?.download?.coverImageFilename,
                extensionId = extensionId
            )

            val chapterId = existingChapterId ?: UUID.randomUUID()
            val chapterEntity = DownloadedChapterEntity(
                id = chapterId,
                downloadId = id,
                chapterName = chapterName,
                chapterUrl = chapterUrl,
                downloadedAt = null,
                filePath = null,
                isFullyDownloaded = false
            )

            downloadsDao.ensureDownloadAndInsertChapter(
                download = downloadEntity,
                chapter = chapterEntity
            )

            downloadCoverIfMissing(
                existingCoverFilename = existingDownload?.download?.coverImageFilename,
                downloadId = id,
                mangaUrl = mangaurl,
                source = source,
                context = context
            )

            val chapterDir =
                "${downloadsDirectory}/${downloadEntity.downloadId}/${chapterEntity.id}"
            var savedImages = 0
            for (imageUrl in chapterImages.images) {
                val imageBytes = downloadImage(imageUrl, chapterImages.headers)
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

    private suspend fun downloadCoverIfMissing(
        existingCoverFilename: String?,
        downloadId: UUID,
        mangaUrl: String,
        source: Source,
        context: Context
    ) {
        if (existingCoverFilename != null) return

        val coverInfo: Source.ImageForChaptersList? = runCatching {
            source.getImageForChaptersList(mangaUrl)
        }.getOrNull()

        if (coverInfo == null || coverInfo.imageUrl.isBlank()) return

        val cover = runCatching {
            CoverCache.downloadImage(coverInfo.imageUrl, coverInfo.headers)
        }.getOrNull()

        if (cover == null || cover.bytes.isEmpty()) return

        val extension = CoverCache.inferImageExtension(
            contentType = cover.contentType,
            finalUrl = cover.finalUrl,
            originalUrl = coverInfo.imageUrl
        )
        val filename = "${downloadId}.$extension"

        fileManager.saveBytesToStorage(
            context = context,
            relativeDir = coverDir,
            fileName = filename,
            bytes = cover.bytes,
            overwrite = true
        )
        downloadsDao.updateCoverFilename(downloadId = downloadId, filename = filename)
    }

}
