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

    suspend fun addChapter(historyId: UUID, chapterUrl: String, readAt: Long? = null) {
        historyDao.insertReadChapter(
            HistoryChapterEntity(
                historyId = historyId,
                chapterUrl = chapterUrl,
                readAt = readAt
            )
        )
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

}