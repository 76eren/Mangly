package com.eren76.mangly.di

import com.eren76.mangly.ExtensionManager
import com.eren76.mangly.FileManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface FileManagersEntryPoint {
    // Methods that can be called from places Hilt does not own
    fun fileManager(): FileManager
    fun extensionManager(): ExtensionManager
}

