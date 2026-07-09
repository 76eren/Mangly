package com.eren76.mangly.downloads

import android.content.Context
import com.eren76.mangly.FileManager
import com.eren76.mangly.composables.shared.image.CoverCache
import com.eren76.mangly.rooms.dao.DownloadsDao
import com.eren76.mangly.rooms.entities.DownloadedChapterEntity
import com.eren76.mangly.rooms.entities.DownloadsEntity
import com.eren76.mangly.rooms.relations.DownloadWithChapters
import com.eren76.manglyextension.plugins.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlin.math.floor

class DownloadManager @Inject constructor(
    private val fileManager: FileManager,
    private val downloadsDao: DownloadsDao,
) {
    private val coverDir = DownloadStorage.COVERS_DIRECTORY

    suspend fun downloadImage(
        imageUrl: String,
        headers: List<Source.Header>
    ): ByteArray {
        val bytes = CoverCache.downloadImage(imageUrl, headers)?.bytes
        if (bytes == null || bytes.isEmpty()) {
            throw IOException("Failed to download image: $imageUrl")
        }
        return bytes
    }

    // In here the actual magic for downloading a chapter happens.
    // It will download the chapter images, save them to storage, and update the database with the chapter's download state.
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

            // Use existing download ID if it exists, otherwise generate a new one
            // This ensures that all chapters of the same manga share the same download ID and if the manga has not been downloaded before, a new download ID is generated for it.
            val id = existingDownload?.download?.downloadId ?: UUID.randomUUID()
            val existingChapter: DownloadedChapterEntity? =
                existingDownload?.chapters?.firstOrNull {
                    it.chapterUrl == chapterUrl
                }

            val chapterImages: Source.ChapterImages = source.getChapterImages(chapterUrl)
            if (chapterImages.images.isEmpty()) {
                throw IOException("Extension returned no images for chapter: $chapterUrl")
            }
            val normalizedSummary = mangaSummary.takeIf { it.isNotBlank() }

            val downloadEntity = DownloadsEntity(
                downloadId = id,
                mangaUrl = mangaurl,
                mangaName = mangaName,
                mangaSummary = normalizedSummary ?: existingDownload?.download?.mangaSummary,
                coverImageFilename = existingDownload?.download?.coverImageFilename,
                extensionId = extensionId
            )

            val chapterId = existingChapter?.id ?: UUID.randomUUID()

            val chapterIndex = parseChapterIndex(chapterName)

            val chapterEntity: DownloadedChapterEntity = DownloadedChapterEntity(
                id = chapterId,
                downloadId = id,
                chapterName = chapterName,
                chapterUrl = chapterUrl,
                chapterIndex = chapterIndex,
                downloadedAt = existingChapter?.downloadedAt,
                filePath = existingChapter?.filePath,
                isFullyDownloaded = existingChapter?.isFullyDownloaded ?: false
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
            val tempChapterDir = "$chapterDir.partial"
            fileManager.deleteDirectory(context = context, relativeDir = tempChapterDir)

            var savedImages = 0
            try {
                for ((pageIndexZeroBased, imageUrl) in chapterImages.images.withIndex()) {
                    val imageBytes = downloadImage(imageUrl, chapterImages.headers)
                    val pageNumber = pageIndexZeroBased + 1

                    val extension = getDownloadExtension(imageUrl)
                    val fileName = "$pageNumber.$extension"
                    fileManager.saveBytesToStorage(
                        context = context,
                        relativeDir = tempChapterDir,
                        fileName = fileName,
                        bytes = imageBytes
                    )

                    savedImages++
                }
            } catch (error: Exception) {
                fileManager.deleteDirectory(context = context, relativeDir = tempChapterDir)
                throw error
            }

            val isFullyDownloaded = savedImages == chapterImages.images.size && savedImages > 0
            if (isFullyDownloaded) {
                val replaced = fileManager.replaceDirectory(
                    context = context,
                    sourceRelativeDir = tempChapterDir,
                    targetRelativeDir = chapterDir
                )

                if (!replaced) {
                    throw IOException("Failed to finalize chapter download: $chapterUrl")
                }
            } else {
                fileManager.deleteDirectory(context = context, relativeDir = tempChapterDir)
            }

            downloadsDao.updateDownloadedChapterState(
                chapterId = chapterId,
                isFullyDownloaded = isFullyDownloaded,
                filePath = chapterDir,
                downloadedAt = if (isFullyDownloaded) System.currentTimeMillis() else null
            )

            if (!isFullyDownloaded) {
                throw IOException(
                    "Downloaded $savedImages/${chapterImages.images.size} pages for chapter: $chapterUrl"
                )
            }

        }
    }

    /**
     * Convert a chapter name like "Chapter 16.5" into an integer ordering index.
     * We use (number * 10) to preserve common .5 chapters (16.5 -> 165) while keeping ints.
     */
    private fun parseChapterIndex(chapterName: String?): Int? {
        if (chapterName.isNullOrBlank()) return null

        // Capture first number in the string (e.g. "16", "16.5", "001")
        val match = Regex("(\\d+(?:\\.\\d+)?)").find(chapterName) ?: return null
        val value = match.value.toDoubleOrNull() ?: return null

        return floor(value * 10.0).toInt()
    }

    fun getDownloadExtension(imageUrl: String): String {
        val extensionFromUrl = runCatching {
            val cleaned = imageUrl.substringBefore('?').substringBefore('#')
            val lastSegment = cleaned.substringAfterLast('/', cleaned)
            lastSegment.substringAfterLast('.', "")
                .lowercase(Locale.US)
                .takeIf { it.isNotBlank() && it.length <= 5 }
        }.getOrNull()
        val extension = when (extensionFromUrl) {
            "jpg", "jpeg", "png", "webp", "gif" -> extensionFromUrl
            else -> "jpg"
        }

        return extension
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

        val cover: CoverCache.DownloadedImage? = runCatching {
            CoverCache.downloadImage(coverInfo.imageUrl, coverInfo.headers)
        }.getOrNull()

        if (cover == null || cover.bytes.isEmpty()) return

        val extension: String = CoverCache.inferImageExtension(
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
