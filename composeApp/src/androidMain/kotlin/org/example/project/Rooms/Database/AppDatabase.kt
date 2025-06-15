package org.example.project.Rooms.Database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import org.example.project.Rooms.Dao.ExtensionDao
import org.example.project.Rooms.Entities.ExtensionEntity

@Database(entities = [ExtensionEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun extensionEntryDao(): ExtensionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}