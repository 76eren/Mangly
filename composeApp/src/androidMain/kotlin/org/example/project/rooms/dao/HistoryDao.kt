package org.example.project.rooms.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import org.example.project.rooms.entities.HistoryChapterEntity
import org.example.project.rooms.entities.HistoryEntity
import org.example.project.rooms.entities.HistoryWithReadChapters
import java.util.UUID

@Dao
interface HistoryDao {
    // HistoryEntity operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: HistoryEntity)

    @Update
    suspend fun update(history: HistoryEntity)

    @Query("SELECT * FROM HistoryEntity WHERE mangaUrl = :mangaUrl LIMIT 1")
    suspend fun getByMangaUrl(mangaUrl: String): HistoryEntity?

    @Query("DELETE FROM HistoryEntity")
    suspend fun clearAll()

    @Query("SELECT * FROM HistoryEntity ORDER BY mangaUrl ASC")
    suspend fun getAll(): List<HistoryEntity>

    // HistoryReadChapterEntity operations
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReadChapter(readChapter: HistoryChapterEntity)

    @Query("SELECT * FROM HistoryReadChapterEntity WHERE historyId = :historyId")
    suspend fun getReadChaptersByHistoryId(historyId: UUID): List<HistoryChapterEntity>

    @Query("DELETE FROM HistoryReadChapterEntity WHERE historyId = :historyId AND chapterUrl IN (:chapterUrls)")
    suspend fun deleteReadChaptersByHistoryIdAndUrls(historyId: UUID, chapterUrls: List<String>)

    @Query("DELETE FROM HistoryReadChapterEntity WHERE historyId = :historyId AND chapterUrl = :chapterUrl")
    suspend fun deleteReadChapterByHistoryIdAndChapterUrl(historyId: UUID, chapterUrl: String)

    // Relations
    @Transaction
    @Query("SELECT * FROM HistoryEntity ORDER BY mangaUrl ASC")
    suspend fun getAllWithReadChapters(): List<HistoryWithReadChapters>
}
