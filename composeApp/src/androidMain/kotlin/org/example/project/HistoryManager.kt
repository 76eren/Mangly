package org.example.project

import org.example.project.rooms.dao.HistoryDao
import org.example.project.rooms.entities.HistoryEntity
import org.example.project.rooms.entities.HistoryReadChapterEntity
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
            HistoryReadChapterEntity(
                historyId = historyId,
                chapterUrl = chapterUrl,
                readAt = readAt
            )
        )
    }

    suspend fun getChapters(historyId: UUID): List<HistoryReadChapterEntity> {
        return historyDao.getReadChaptersByHistoryId(historyId)
    }

    suspend fun findByMangaUrl(mangaUrl: String): HistoryEntity? {
        return historyDao.getByMangaUrl(mangaUrl)
    }

    suspend fun deleteChaptersByMangaUrlAndChapterUrls(
        mangaUrl: String,
        chapterUrls: Collection<String>
    ) {
        if (chapterUrls.isEmpty()) return

        val historyEntity = historyDao.getByMangaUrl(mangaUrl) ?: return
        historyDao.deleteReadChaptersByHistoryIdAndUrls(historyEntity.id, chapterUrls.toList())
    }
}