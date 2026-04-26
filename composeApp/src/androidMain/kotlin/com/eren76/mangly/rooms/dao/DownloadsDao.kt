package com.eren76.mangly.rooms.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.eren76.mangly.rooms.entities.DownloadedChapterEntity
import com.eren76.mangly.rooms.entities.DownloadsEntity
import com.eren76.mangly.rooms.relations.DownloadWithChapters
import java.util.UUID

@Dao
interface DownloadsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadedChapter(chapter: DownloadedChapterEntity)

    @Query("SELECT * FROM DownloadsEntity WHERE download_id = :downloadId LIMIT 1")
    suspend fun getDownloadById(downloadId: UUID): DownloadsEntity?

    @Query("SELECT * FROM DownloadedChapterEntity WHERE download_id = :downloadId")
    suspend fun getChaptersByDownloadId(downloadId: UUID): List<DownloadedChapterEntity>

    @Transaction
    @Query("SELECT * FROM DownloadsEntity ORDER BY manga_url ASC")
    suspend fun getAllWithChapters(): List<DownloadWithChapters>

    @Transaction
    @Query("SELECT * FROM DownloadsEntity WHERE download_id = :downloadId LIMIT 1")
    suspend fun getWithChaptersByDownloadId(downloadId: UUID): DownloadWithChapters?

    @Transaction
    @Query("SELECT * FROM DownloadsEntity WHERE manga_url = :mangaUrl LIMIT 1")
    suspend fun getWithChaptersByMangaUrl(mangaUrl: String): DownloadWithChapters?

    @Query("DELETE FROM DownloadedChapterEntity WHERE id = :chapterId")
    suspend fun deleteDownloadedChapterById(chapterId: UUID)

    @Query("DELETE FROM DownloadsEntity WHERE download_id = :downloadId")
    suspend fun deleteDownloadById(downloadId: UUID)

    @Query(
        """
        UPDATE DownloadedChapterEntity
        SET is_fully_downloaded = :isFullyDownloaded,
            file_path = :filePath,
            downloaded_at = :downloadedAt
        WHERE id = :chapterId
        """
    )
    suspend fun updateDownloadedChapterState(
        chapterId: UUID,
        isFullyDownloaded: Boolean,
        filePath: String?,
        downloadedAt: Long?
    )

    @Query("UPDATE DownloadsEntity SET cover_image_filename = :filename WHERE download_id = :downloadId")
    suspend fun updateCoverFilename(downloadId: UUID, filename: String?)

    @Query(
        """
        UPDATE DownloadsEntity
        SET manga_name = :mangaName,
            summary = :mangaSummary
        WHERE download_id = :downloadId
        """
    )
    suspend fun updateDownloadMetadata(
        downloadId: UUID,
        mangaName: String?,
        mangaSummary: String?
    )

    @Transaction
    suspend fun ensureDownloadAndInsertChapter(
        download: DownloadsEntity,
        chapter: DownloadedChapterEntity
    ) {
        val existing = getDownloadById(download.downloadId)
        if (existing == null) {
            insertDownload(download)
        } else {
            val resolvedName = download.mangaName ?: existing.mangaName
            val resolvedSummary = download.mangaSummary ?: existing.mangaSummary
            if (resolvedName != existing.mangaName || resolvedSummary != existing.mangaSummary) {
                updateDownloadMetadata(
                    downloadId = download.downloadId,
                    mangaName = resolvedName,
                    mangaSummary = resolvedSummary
                )
            }
        }
        insertDownloadedChapter(chapter)
    }
}
