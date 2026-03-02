package com.eren76.mangly.rooms.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.eren76.mangly.rooms.entities.FavoritesEntity
import java.util.UUID

@Dao
interface FavoritesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(fav: FavoritesEntity)

    @Query("SELECT * FROM FavoritesEntity WHERE extension_id = :extensionId")
    suspend fun getByExtensionId(extensionId: UUID): List<FavoritesEntity>

    @Query("DELETE FROM FavoritesEntity WHERE id = :id")
    suspend fun delete(id: UUID)

    @Query("SELECT * FROM FavoritesEntity")
    suspend fun getAll(): List<FavoritesEntity>

    @Query("SELECT * FROM FavoritesEntity WHERE manga_url = :url")
    suspend fun getByUrl(url: String): List<FavoritesEntity>

    @Query("UPDATE FavoritesEntity SET cover_image_filename = :filename WHERE id = :id")
    suspend fun updateCoverFilename(id: UUID, filename: String?)
}
