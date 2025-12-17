package org.example.project.Rooms.Dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.example.project.Rooms.Entities.HistoryReadChapterEntity
import java.util.UUID

@Dao
interface HistoryReadChapterDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(readChapter: HistoryReadChapterEntity)

    @Query("SELECT * FROM HistoryReadChapterEntity WHERE historyId = :historyId")
    suspend fun getByHistoryId(historyId: UUID): List<HistoryReadChapterEntity>

    @Query("DELETE FROM HistoryReadChapterEntity WHERE historyId = :historyId AND chapterUrl = :chapterUrl")
    suspend fun delete(historyId: UUID, chapterUrl: String)

    @Query("DELETE FROM HistoryReadChapterEntity WHERE historyId = :historyId")
    suspend fun deleteForHistory(historyId: UUID)
}
