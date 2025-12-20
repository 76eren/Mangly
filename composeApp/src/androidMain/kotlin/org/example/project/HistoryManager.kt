package org.example.project

import org.example.project.rooms.dao.HistoryDao
import org.example.project.rooms.entities.HistoryChapterEntity
import org.example.project.rooms.entities.HistoryEntity
import org.example.project.rooms.entities.HistoryWithReadChapters
import java.util.UUID
import javax.inject.Inject

class HistoryManager @Inject constructor(
    private val historyDao: HistoryDao
) {

    suspend fun getAllHistoryWithReadChapters(): List<HistoryWithReadChapters> {
        return historyDao.getAllWithReadChapters()
    }

    suspend fun getChapters(historyId: UUID): List<HistoryChapterEntity> {
        return historyDao.getReadChaptersByHistoryId(historyId)
    }

    suspend fun findByMangaUrl(mangaUrl: String): HistoryEntity? {
        return historyDao.getByMangaUrl(mangaUrl)
    }

    suspend fun deleteChapterByMangaUrlAndChapterUrl(
        mangaUrl: String,
        chapterUrl: String
    ) {
        val historyEntity = historyDao.getByMangaUrl(mangaUrl) ?: return
        historyDao.deleteReadChapterByHistoryIdAndChapterUrl(
            historyEntity.id,
            chapterUrl
        )
    }

    suspend fun ensureHistoryAndAddChapter(
        mangaUrl: String,
        mangaName: String,
        extensionId: UUID,
        chapterUrl: String,
        readAt: Long?
    ) {
        historyDao.ensureHistoryAndAddChapterTransactional(
            mangaUrl = mangaUrl,
            mangaName = mangaName,
            extensionId = extensionId,
            chapterUrl = chapterUrl,
            readAt = readAt
        )
    }
}