package org.example.project.HistoryManager

import org.example.project.Rooms.Dao.HistoryDao
import org.example.project.Rooms.Dao.HistoryReadChapterDao
import org.example.project.Rooms.Entities.HistoryEntity
import org.example.project.Rooms.Entities.HistoryReadChapterEntity
import javax.inject.Inject

class HistoryManager @Inject constructor(
    private val historyDao: HistoryDao,
    private val historyReadChapterDao: HistoryReadChapterDao
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

    suspend fun addChapter(historyId: java.util.UUID, chapterUrl: String, readAt: Long? = null) {
        historyReadChapterDao.insert(
            HistoryReadChapterEntity(
                historyId = historyId,
                chapterUrl = chapterUrl,
                readAt = readAt
            )
        )
    }

    suspend fun getChapters(historyId: java.util.UUID): List<HistoryReadChapterEntity> {
        return historyReadChapterDao.getByHistoryId(historyId)
    }
}