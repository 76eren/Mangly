package org.example.project.Rooms.Dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.example.project.Rooms.Entities.ExtensionEntity
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
}
