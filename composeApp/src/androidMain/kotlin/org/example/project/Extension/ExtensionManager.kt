package org.example.project.Extension

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import com.caverock.androidsvg.SVG
import com.example.manglyextension.plugins.ExtensionMetadata
import com.example.manglyextension.plugins.PluginMetadata
import com.example.manglyextension.plugins.Source
import com.google.gson.Gson
import dalvik.system.DexClassLoader
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import androidx.core.graphics.createBitmap
import com.example.manglyextension.plugins.PreferenceImplementation
import org.example.project.Composables.Standard.CardData
import org.example.project.FileManager.FileManager
import org.example.project.Rooms.Entities.ExtensionEntity
import java.util.UUID

class ExtensionManager {

    /**
     * Reads just the metadata out of a plugin .zip.
     */
    fun extractExtensionMetadata(zipFile: File): ExtensionMetadata {
        val zipContents = readZip(zipFile.readBytes())

        val jsonBytes = zipContents["meta/plugin.json"]
            ?: throw IllegalArgumentException("Missing meta/plugin.json in ${zipFile.name}")

        val jsonText = jsonBytes.toString(Charsets.UTF_8)

        val metadata = Gson().fromJson(jsonText, PluginMetadata::class.java)

        val vectorImageBytes: ByteArray = zipContents["meta/icon.svg"]
            ?: throw IllegalArgumentException("Missing meta/icon.svg in ${zipFile.name}")

        val bitmapPainter: BitmapPainter = byteArrayToImageBitmap(vectorImageBytes)


        val dex = zipContents["dex/classes.dex"]
            ?: throw IllegalArgumentException("Missing dex/classes.dex in ${zipFile.name}")

        return ExtensionMetadata(
            entryClass = metadata.entryClass,
            name = metadata.name,
            version = metadata.version,
            icon = bitmapPainter,
            dexFile = dex
        )
    }

    /**
     * Loads and instantiates the plugin Source from the .zip.
     */
    fun loadPluginSource(metadata: ExtensionMetadata, context: Context): Source {
        val dexFile = File.createTempFile("plugin", ".dex", context.codeCacheDir)

        dexFile.writeBytes(metadata.dexFile)

        dexFile.setReadOnly()

        val optimizedDir = File(context.codeCacheDir, "opt_dex")
        if (!optimizedDir.exists()) optimizedDir.mkdirs()

        val classLoader = DexClassLoader(
            dexFile.absolutePath,
            optimizedDir.absolutePath,
            null,
            context.classLoader
        )

        val clazz = classLoader.loadClass(metadata.entryClass)

        // To get the correct shared preferences we need to get the settings key, which is part of the source
        val preferenceKeyField = clazz.getDeclaredField("preferenceKey")
        preferenceKeyField.isAccessible = true
        val settingsKey = preferenceKeyField.get(null) as UUID

        val uiPreferenceKeyField = clazz.getDeclaredField("uiPreferenceKey")
        uiPreferenceKeyField.isAccessible = true
        val uiSettingsKey = uiPreferenceKeyField.get(null) as UUID

        val preferences = PreferenceImplementation(settingsKey, uiSettingsKey, context)

        return clazz.getDeclaredConstructor(PreferenceImplementation::class.java)
            .newInstance(preferences) as Source
    }

    /**
     * Gets the database entry for a given ExtensionMetadata.
     */
    suspend fun getDatabaseEntryByMetadata(currentMetadata: ExtensionMetadata, context: Context): ExtensionEntity {
        val fileManager= FileManager()

        val allEntries: List<ExtensionEntity> = fileManager.getAllEntries(context)
        for (entry in allEntries) {
            val targetMetaData: ExtensionMetadata = extractExtensionMetadata(File(entry.filePath))

            if (
                targetMetaData.entryClass == currentMetadata.entryClass && targetMetaData.dexFile.contentEquals(currentMetadata.dexFile)) {
                return entry
            }
        }

        throw IllegalArgumentException("No matching database entry found for metadata: ${currentMetadata.name}")
    }

    /**
     * Helper methods
     */

    private fun readZip(zipBytes: ByteArray): Map<String, ByteArray> {
        val zipMap = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val buffer = ByteArrayOutputStream()
                    zis.copyTo(buffer)
                    zipMap[entry.name] = buffer.toByteArray()
                }
                entry = zis.nextEntry
            }
        }
        return zipMap
    }

    private fun byteArrayToImageBitmap(vectorImageBytes: ByteArray): BitmapPainter {
        val svg: SVG = SVG.getFromString(vectorImageBytes.toString(Charsets.UTF_8))
        val bitmap = createBitmap(64, 64)
        val canvas = Canvas(bitmap)

        svg.setDocumentWidth("100%")
        svg.setDocumentHeight("100%")
        svg.renderToCanvas(canvas)

        val imageBitmap: ImageBitmap = bitmap.asImageBitmap()
        return BitmapPainter(imageBitmap)
    }

}