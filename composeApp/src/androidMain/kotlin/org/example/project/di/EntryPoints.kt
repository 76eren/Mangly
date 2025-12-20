package org.example.project.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.example.project.ExtensionManager
import org.example.project.FileManager

@EntryPoint
@InstallIn(SingletonComponent::class)
interface FileManagersEntryPoint {
    // Methods that can be called from places Hilt does not own
    fun fileManager(): FileManager
    fun extensionManager(): ExtensionManager
}

