package org.example.project.Extension

import android.content.Context
import com.example.manglyextension.plugins.ExtensionMetadata
import com.example.manglyextension.plugins.PluginMetadata
import com.example.manglyextension.plugins.Source
import com.google.gson.Gson
import dalvik.system.DexClassLoader
import java.io.*
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

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

        val vectorImage = zipContents["meta/icon.svg"]
            ?: throw IllegalArgumentException("Missing meta/icon.svg in ${zipFile.name}")

        val dex = zipContents["dex/classes.dex"]
            ?: throw IllegalArgumentException("Missing dex/classes.dex in ${zipFile.name}")

        return ExtensionMetadata(
            entryClass = metadata.entryClass,
            name = metadata.name,
            version = metadata.version,
            icon = vectorImage,
            dexFile = dex
        )
    }

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
        return clazz.getDeclaredConstructor().newInstance() as Source
    }



}