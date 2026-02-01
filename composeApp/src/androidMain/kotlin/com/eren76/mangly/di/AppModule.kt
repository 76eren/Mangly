package com.eren76.mangly.di

import android.content.Context
import com.eren76.mangly.ExtensionManager
import com.eren76.mangly.FileManager
import com.eren76.mangly.rooms.dao.ExtensionDao
import com.eren76.mangly.rooms.dao.HistoryDao
import com.eren76.mangly.rooms.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
