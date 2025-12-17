package org.example.project.Rooms.Dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import org.example.project.Rooms.Entities.HistoryEntity
import java.util.UUID

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: HistoryEntity)

    @Update
    suspend fun update(history: HistoryEntity)

    @Query("SELECT * FROM HistoryEntity WHERE id = :id LIMIT 1")
    suspend fun getById(id: UUID): HistoryEntity?

    @Query("SELECT * FROM HistoryEntity WHERE mangaUrl = :mangaUrl AND extensionId = :extensionId LIMIT 1")
    suspend fun getByMangaAndExtension(mangaUrl: String, extensionId: UUID): HistoryEntity?

    @Query("SELECT * FROM HistoryEntity WHERE mangaUrl = :mangaUrl LIMIT 1")
    suspend fun getByMangaUrl(mangaUrl: String): HistoryEntity?

    @Query("DELETE FROM HistoryEntity WHERE id = :id")
    suspend fun deleteById(id: UUID)

    @Query("DELETE FROM HistoryEntity WHERE mangaUrl = :mangaUrl AND extensionId = :extensionId")
    suspend fun deleteByMangaAndExtension(mangaUrl: String, extensionId: UUID)

    @Query("DELETE FROM HistoryEntity")
    suspend fun clearAll()

    @Query("SELECT * FROM HistoryEntity WHERE extensionId = :extensionId ORDER BY mangaUrl ASC")
    suspend fun getAllForExtension(extensionId: UUID): List<HistoryEntity>

    @Query("SELECT * FROM HistoryEntity ORDER BY mangaUrl ASC")
    suspend fun getAll(): List<HistoryEntity>
}

