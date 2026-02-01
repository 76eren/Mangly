package com.eren76.mangly.rooms.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.eren76.mangly.rooms.converters.UuidConverters
import com.eren76.mangly.rooms.dao.ExtensionDao
import com.eren76.mangly.rooms.dao.FavoritesDao
import com.eren76.mangly.rooms.dao.HistoryDao
import com.eren76.mangly.rooms.database.migrations.Migration1To2
import com.eren76.mangly.rooms.database.migrations.Migration2To3
import com.eren76.mangly.rooms.database.migrations.Migration3To4
import com.eren76.mangly.rooms.database.migrations.Migration4To5
import com.eren76.mangly.rooms.entities.ExtensionEntity
import com.eren76.mangly.rooms.entities.FavoritesEntity
import com.eren76.mangly.rooms.entities.HistoryChapterEntity
import com.eren76.mangly.rooms.entities.HistoryEntity

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