package org.example.project.Rooms.Database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.example.project.Rooms.Converters.UuidConverters
import org.example.project.Rooms.Dao.ExtensionDao
import org.example.project.Rooms.Dao.FavoritesDao
import org.example.project.Rooms.Database.Migrations.Migration1To2
import org.example.project.Rooms.Entities.ExtensionEntity
import org.example.project.Rooms.Entities.FavoritesEntity

@Database(
    entities = [ExtensionEntity::class, FavoritesEntity::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(UuidConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun extensionEntryDao(): ExtensionDao
    abstract fun favoritesDao(): FavoritesDao

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
                    .addMigrations(Migration1To2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}