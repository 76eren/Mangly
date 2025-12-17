package org.example.project.HistoryManager

import org.example.project.Rooms.Dao.HistoryDao
import org.example.project.Rooms.Entities.HistoryEntity
import org.example.project.Rooms.Entities.HistoryReadChapterEntity
import org.example.project.Rooms.Entities.HistoryWithReadChapters
import javax.inject.Inject

class HistoryManager @Inject constructor(
    private val historyDao: HistoryDao
) {
    suspend fun clearHistory() {
        historyDao.clearAll()
    }

    suspend fun addHistoryEntry(historyEntity: HistoryEntity) {
        historyDao.insert(historyEntity)
    }

    suspend fun getAllHistoryEntries(): List<HistoryEntity> {
        return historyDao.getAll()
    }

    suspend fun getAllHistoryWithReadChapters(): List<HistoryWithReadChapters> {
        return historyDao.getAllWithReadChapters()
    }

    suspend fun addChapter(historyId: java.util.UUID, chapterUrl: String, readAt: Long? = null) {
        historyDao.insertReadChapter(
            HistoryReadChapterEntity(
                historyId = historyId,
                chapterUrl = chapterUrl,
                readAt = readAt
            )
        )
    }

    suspend fun getChapters(historyId: java.util.UUID): List<HistoryReadChapterEntity> {
        return historyDao.getReadChaptersByHistoryId(historyId)
    }

    suspend fun findByMangaUrl(mangaUrl: String): HistoryEntity? {
        return historyDao.getByMangaUrl(mangaUrl)
    }
}