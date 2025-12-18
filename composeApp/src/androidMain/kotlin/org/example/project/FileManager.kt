package org.example.project

import android.content.Context
import com.example.manglyextension.plugins.ExtensionMetadata
import org.example.project.rooms.dao.ExtensionDao
import org.example.project.rooms.entities.ExtensionEntity
import java.io.File
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject

class FileManager @Inject constructor(
    private val extensionManager: ExtensionManager,
    private val extensionDao: ExtensionDao
) {

    // TODO: Handle extension updates and duplicates
    suspend fun saveAndInsertEntry(
        context: Context,
        inputStream: InputStream,
    ): File {
        val zipBytes = inputStream.readBytes()

        val extensionMetadata: ExtensionMetadata =
            extensionManager.extractExtensionMetadata(zipBytes, context)

        val extensionId = extensionMetadata.source.getExtensionId()
        val fileName = "$extensionId.zip"
        val file = File(context.filesDir, fileName)

        file.outputStream().use { it.write(zipBytes) }

        val id = UUID.fromString(extensionId)

        val zipFileEntry = ExtensionEntity(
            id = id,
            name = extensionMetadata.name,
            filePath = file.absolutePath,
            uploadTime = System.currentTimeMillis()
        )

        extensionDao.insert(zipFileEntry)

        return file
    }

    suspend fun deleteAndRemoveEntry(entityToBeDeleted: ExtensionEntity, context: Context) {
        extensionDao.delete(entityToBeDeleted.id)

        val file = File(entityToBeDeleted.filePath)
        if (file.exists()) {
            file.delete()
        }
    }

    suspend fun getAllEntries(context: Context): List<ExtensionEntity> {
        return extensionDao.getAll()
    }


}