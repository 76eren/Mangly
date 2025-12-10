package org.example.project.Rooms.Dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.example.project.Rooms.Entities.FavoritesEntity
import java.util.UUID

@Dao
interface FavoritesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(fav: FavoritesEntity)

    @Query("SELECT * FROM FavoritesEntity WHERE extensionId = :extensionId")
    suspend fun getByExtensionId(extensionId: UUID): List<FavoritesEntity>

    @Query("DELETE FROM FavoritesEntity WHERE id = :id")
    suspend fun delete(id: UUID)

    @Query("SELECT * FROM FavoritesEntity")
    suspend fun getAll(): List<FavoritesEntity>
}

