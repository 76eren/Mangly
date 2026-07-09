package com.eren76.mangly.workers

import android.content.Context
import com.eren76.mangly.ExtensionManager
import com.eren76.mangly.downloads.DownloadManager
import com.eren76.mangly.rooms.dao.ExtensionDao
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface ChapterDownloadWorkerDependencies {
    fun downloadManager(): DownloadManager
    fun extensionDao(): ExtensionDao
    fun extensionManager(): ExtensionManager
}

internal object ChapterDownloadWorkerDependencyProvider {
    fun from(context: Context): ChapterDownloadWorkerDependencies {
        return EntryPointAccessors.fromApplication(
            context.applicationContext,
            ChapterDownloadWorkerDependencies::class.java
        )
    }
}
