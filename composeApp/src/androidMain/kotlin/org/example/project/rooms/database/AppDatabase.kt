package org.example.project.rooms.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.example.project.rooms.converters.UuidConverters
import org.example.project.rooms.dao.ExtensionDao
import org.example.project.rooms.dao.FavoritesDao
import org.example.project.rooms.dao.HistoryDao
import org.example.project.rooms.database.migrations.Migration1To2
import org.example.project.rooms.database.migrations.Migration2To3
import org.example.project.rooms.database.migrations.Migration3To4
import org.example.project.rooms.database.migrations.Migration4To5
import org.example.project.rooms.entities.ExtensionEntity
import org.example.project.rooms.entities.FavoritesEntity
import org.example.project.rooms.entities.HistoryChapterEntity
import org.example.project.rooms.entities.HistoryEntity

@Database(
    entities = [ExtensionEntity::class, FavoritesEntity::class, HistoryEntity::class, HistoryChapterEntity::class],
    version = 5,
    exportSchema = true
)
@TypeConverters(UuidConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun extensionEntryDao(): ExtensionDao
    abstract fun favoritesDao(): FavoritesDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(Migration1To2, Migration2To3, Migration3To4, Migration4To5)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}