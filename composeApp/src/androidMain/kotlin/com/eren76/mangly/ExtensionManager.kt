package com.eren76.mangly

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.core.graphics.createBitmap
import com.caverock.androidsvg.SVG
import com.eren76.mangly.rooms.dao.ExtensionDao
import com.eren76.mangly.rooms.entities.ExtensionEntity
import com.eren76.manglyextension.plugins.ExtensionMetadata
import com.eren76.manglyextension.plugins.IPreferences
import com.eren76.manglyextension.plugins.PluginMetadata
import com.eren76.manglyextension.plugins.PreferenceImplementation
import com.eren76.manglyextension.plugins.Source
import com.google.gson.Gson
import dalvik.system.DexClassLoader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.inject.Inject

class ExtensionManager @Inject constructor(
    private val extensionDao: ExtensionDao
) {
    /**
     * Reads just the metadata out of a plugin .zip file (as a bytearray).
     */
    fun extractExtensionMetadata(zipBytes: ByteArray, context: Context): ExtensionMetadata {
        val zipContents = readZip(zipBytes)

        val jsonBytes = zipContents["meta/plugin.json"]
            ?: throw IllegalArgumentException("Missing meta/plugin.json")

        val metadata =
            Gson().fromJson(jsonBytes.toString(Charsets.UTF_8), PluginMetadata::class.java)

        val vectorImageBytes = zipContents["meta/icon.svg"]
            ?: throw IllegalArgumentException("Missing meta/icon.svg")

        val bitmapPainter = byteArrayToImageBitmap(vectorImageBytes)

        val dex = zipContents["dex/classes.dex"]
            ?: throw IllegalArgumentException("Missing dex/classes.dex")

        return ExtensionMetadata(
            entryClass = metadata.entryClass,
            name = metadata.name,
            version = metadata.version,
            icon = bitmapPainter,
            dexFile = dex,
            loadPluginSource(metadata.entryClass, dex, context)
        )
    }

    /**
     * Loads and instantiates the plugin Source from the .zip
     */
    private fun loadPluginSource(entryClass: String, dex: ByteArray, context: Context): Source {
        val dexFile = File.createTempFile("plugin", ".dex", context.codeCacheDir)

        dexFile.writeBytes(dex)

        dexFile.setReadOnly()

        val optimizedDir = File(context.codeCacheDir, "opt_dex")
        if (!optimizedDir.exists()) optimizedDir.mkdirs()

        val classLoader = DexClassLoader(
            dexFile.absolutePath,
            optimizedDir.absolutePath,
            null,
            context.classLoader
        )

        val clazz = classLoader.loadClass(entryClass)

        // The problem is that IPreferences depends on the extensionId and you cannot get the extensionId without an instance of Source.
        // To fix this we first create an instance of Source with null preferences to get the extensionId,
        val sourceWithoutPrefs = clazz.getDeclaredConstructor(IPreferences::class.java)
            .newInstance(null) as Source

        val settingsKey: String = sourceWithoutPrefs.getExtensionId()

        val settingsSharedPreferences: SharedPreferences = context.getSharedPreferences(
            settingsKey,
            Context.MODE_PRIVATE
        )

        val preferences = PreferenceImplementation(settingsSharedPreferences, context)

        return clazz.getDeclaredConstructor(IPreferences::class.java)
            .newInstance(preferences) as Source
    }

    /**
     * Gets the database entry for a given ExtensionMetadata.
     */
    suspend fun getDatabaseEntryByMetadata(
        currentMetadata: ExtensionMetadata,
        context: Context
    ): ExtensionEntity {

        val allEntries: List<ExtensionEntity> = extensionDao.getAll()
        for (entry in allEntries) {
            val targetMetaData: ExtensionMetadata =
                extractExtensionMetadata(File(entry.filePath).readBytes(), context)

            if (
                targetMetaData.entryClass == currentMetadata.entryClass && targetMetaData.dexFile.contentEquals(
                    currentMetadata.dexFile
                )
            ) {
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
        return androidx.compose.ui.graphics.painter.BitmapPainter(imageBitmap)
    }

}