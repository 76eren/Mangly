package org.example.project.rooms.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import org.example.project.rooms.entities.ExtensionEntity
import org.example.project.rooms.relations.ExtensionWithFavorites
import java.util.UUID

@Dao
interface ExtensionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(zipFileEntry: ExtensionEntity)

    @Query("SELECT * FROM ExtensionEntity")
    suspend fun getAll(): List<ExtensionEntity>

    @Query("SELECT * FROM ExtensionEntity WHERE id = :id")
    suspend fun getById(id: UUID): ExtensionEntity?

    @Query("SELECT * FROM ExtensionEntity WHERE name = :name")
    suspend fun getByName(name: String): List<ExtensionEntity>

    @Query("DELETE FROM ExtensionEntity WHERE id = :id")
    suspend fun delete(id: UUID)

    @Transaction
    @Query("SELECT * FROM ExtensionEntity WHERE id = :extensionId")
    suspend fun getExtensionWithFavorites(extensionId: UUID): ExtensionWithFavorites
}
