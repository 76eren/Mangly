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

    /**
     * Returns the existing database entity if an entry with the same extensionId already exists.
     * If not found, returns null.
     * Takes as param an InputStream of the zip file to read metadata from.
     */
    suspend fun getExistingEntryByInputStream(
        context: Context,
        inputStream: InputStream
    ): ExtensionEntity? {
        val zipBytes = inputStream.readBytes()
        val extensionMetadata: ExtensionMetadata =
            extensionManager.extractExtensionMetadata(zipBytes, context)

        val extensionId = extensionMetadata.source.getExtensionId()
        val id = UUID.fromString(extensionId)
        return extensionDao.getById(id)
    }


    /**
     * Replace the zip file contents and update the database entry's name and upload time.
     * id and filePath remain unchanged.
     */
    suspend fun replaceZipFileAndUpdateEntity(
        context: Context,
        existingEntity: ExtensionEntity,
        newZipBytes: ByteArray
    ): File {
        val updatedFile = replaceZipFile(existingEntity, newZipBytes)
        val newMetadata: ExtensionMetadata =
            extensionManager.extractExtensionMetadata(newZipBytes, context)
        val updatedEntity = ExtensionEntity(
            id = existingEntity.id,
            name = newMetadata.name,
            filePath = existingEntity.filePath,
            uploadTime = System.currentTimeMillis()
        )
        extensionDao.insert(updatedEntity)
        return updatedFile
    }

    /**
     * Replace the zip file contents on disk for an existing entity with new bytes.
     * Does not modify the database entry.
     */
    fun replaceZipFile(
        existingEntity: ExtensionEntity,
        newZipBytes: ByteArray
    ): File {
        val file = File(existingEntity.filePath)
        file.parentFile?.let { if (!it.exists()) it.mkdirs() }
        val tmpFile = File(file.parentFile, file.name + ".tmp")
        tmpFile.outputStream().use { it.write(newZipBytes) }
        if (file.exists()) {
            if (!tmpFile.renameTo(file)) {
                file.delete()
                tmpFile.renameTo(file)
            }
        } else {
            tmpFile.renameTo(file)
        }
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