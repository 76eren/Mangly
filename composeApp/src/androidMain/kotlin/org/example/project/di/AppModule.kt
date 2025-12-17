package org.example.project.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.example.project.Extension.ExtensionManager
import org.example.project.FileManager.FileManager
import org.example.project.Rooms.Dao.ExtensionDao
import org.example.project.Rooms.Dao.HistoryDao
import org.example.project.Rooms.Database.AppDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = AppDatabase.getDatabase(context)

    @Provides
    fun provideFavoritesDao(db: AppDatabase) = db.favoritesDao()

    @Provides
    fun provideExtensionDao(db: AppDatabase): ExtensionDao = db.extensionEntryDao()

    @Provides
    fun provideHistoryDao(db: AppDatabase): HistoryDao = db.historyDao()

    @Provides
    @Singleton
    fun provideExtensionManager(extensionDao: ExtensionDao): ExtensionManager =
        ExtensionManager(extensionDao)

    @Provides
    @Singleton
    fun provideFileManager(
        extensionManager: ExtensionManager,
        extensionDao: ExtensionDao
    ): FileManager = FileManager(extensionManager, extensionDao)
}
