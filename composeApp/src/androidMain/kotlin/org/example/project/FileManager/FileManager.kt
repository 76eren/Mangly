package org.example.project.FileManager

import android.content.Context
import org.example.project.Rooms.Database.AppDatabase
import org.example.project.Rooms.Entities.ExtensionEntity
import java.io.File
import java.io.InputStream
import java.util.UUID

class FileManager {

    suspend fun saveAndInsertEntry(
        context: Context,
        fileName: String,
        inputStream: InputStream,
        id: UUID
    ): File {
        val file = File(context.filesDir, fileName)

        inputStream.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }

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

    suspend fun getAllEntries(context: Context): List<ExtensionEntity> {
        val db = AppDatabase.getDatabase(context)
        return db.extensionEntryDao().getAll()
    }




}