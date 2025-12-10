package org.example.project.FileManager

import android.content.Context
import com.example.manglyextension.plugins.ExtensionMetadata
import org.example.project.Extension.ExtensionManager
import org.example.project.Rooms.Database.AppDatabase
import org.example.project.Rooms.Entities.ExtensionEntity
import java.io.File
import java.io.InputStream
import java.util.UUID

class FileManager {

    // TODO: Handle extension updates and duplicates
    suspend fun saveAndInsertEntry(
        context: Context,
        inputStream: InputStream,
    ): File {
        val zipBytes = inputStream.readBytes()

        val extensionManager = ExtensionManager()
        val extensionMetadata: ExtensionMetadata =
            extensionManager.extractExtensionMetadata(zipBytes, context)

        val extensionId = extensionMetadata.source.getExtensionId()
        val fileName = "$extensionId.zip"
        val file = File(context.filesDir, fileName)

        file.outputStream().use { it.write(zipBytes) }

        val id = UUID.fromString(extensionId)

        val zipFileEntry = ExtensionEntity(
            id = id,
            name = fileName,
            filePath = file.absolutePath,
            uploadTime = System.currentTimeMillis()
        )

        val db = AppDatabase.getDatabase(context)
        db.extensionEntryDao().insert(zipFileEntry)

        return file
    }

    suspend fun deleteAndRemoveEntry(entityToBeDeleted: ExtensionEntity, context: Context) {
        val db = AppDatabase.getDatabase(context)
        db.extensionEntryDao().delete(entityToBeDeleted.id)

        val file = File(entityToBeDeleted.filePath)
        if (file.exists()) {
            file.delete()
        }
    }

    suspend fun getAllEntries(context: Context): List<ExtensionEntity> {
        val db = AppDatabase.getDatabase(context)
        return db.extensionEntryDao().getAll()
    }


}