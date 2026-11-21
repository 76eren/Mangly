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
import com.eren76.manglyextension.plugins.ExtensionLoadResult
import com.eren76.manglyextension.plugins.ExtensionMetadata
import com.eren76.manglyextension.plugins.IPreferences
import com.eren76.manglyextension.plugins.LoadedSource
import com.eren76.manglyextension.plugins.PluginMetadata
import com.eren76.manglyextension.plugins.PreferenceImplementation
import com.eren76.manglyextension.plugins.Source
import com.google.gson.Gson
import dalvik.system.DexClassLoader
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.reflect.Modifier
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.inject.Inject

class ExtensionManager @Inject constructor(
    private val extensionDao: ExtensionDao
) {
    fun extractExtensionMetadata(
        zipBytes: ByteArray,
        context: Context,
        installedExtensionId: UUID? = null
    ): ExtensionMetadata = loadExtension(zipBytes, context, installedExtensionId).metadata

    fun loadExtension(
        zipBytes: ByteArray,
        context: Context,
        installedExtensionId: UUID? = null
    ): ExtensionLoadResult {
        val zipContents = readZip(zipBytes)

        val jsonBytes = zipContents["meta/plugin.json"]
            ?: throw IllegalArgumentException("Missing meta/plugin.json")

        val metadata =
            Gson().fromJson(jsonBytes.toString(Charsets.UTF_8), PluginMetadata::class.java)
                ?: throw IllegalArgumentException("Invalid meta/plugin.json")

        require(metadata.entryClass.isNotBlank()) { "Plugin entryClass is blank" }
        require(metadata.name.isNotBlank()) { "Plugin name is blank" }
        require(metadata.version.isNotBlank()) { "Plugin version is blank" }

        val vectorImageBytes = zipContents["meta/icon.svg"]
            ?: throw IllegalArgumentException("Missing meta/icon.svg")

        val bitmapPainter = byteArrayToImageBitmap(vectorImageBytes)

        val dex = zipContents["dex/classes.dex"]
            ?: throw IllegalArgumentException("Missing dex/classes.dex")

        val source: Source? = try {
            loadPluginSource(metadata.entryClass, dex, context)
        } catch (error: Exception) {
            if (installedExtensionId == null) throw IllegalArgumentException(
                "Unable to load extension",
                error
            )
            null
        } catch (error: LinkageError) {
            if (installedExtensionId == null) throw IllegalArgumentException(
                "Extension needs updating",
                error
            )
            null
        }
        val extensionId = installedExtensionId?.toString() ?: try {
            requireNotNull(source).getExtensionId()
        } catch (error: LinkageError) {
            throw IllegalArgumentException("Extension ID is unavailable", error)
        }
        UUID.fromString(extensionId)

        val result: ExtensionLoadResult = validateSource(
            ExtensionMetadata(
                entryClass = metadata.entryClass,
                name = metadata.name,
                version = metadata.version,
                icon = bitmapPainter,
                dexFile = dex,
                extensionId = extensionId,
                canSourceBeLoaded = false
            ),
            source
        )
        result.loadedSource?.let {
            require(it.source.getExtensionName().isNotBlank()) { "Extension name is blank" }
        }

        return result
    }

    internal fun validateSource(
        metadata: ExtensionMetadata,
        candidate: Source?
    ): ExtensionLoadResult {
        val source = candidate?.takeIf { determineCanSourceBeLoaded(it) }
        val validatedMetadata: ExtensionMetadata = metadata.copy(canSourceBeLoaded = source != null)
        return ExtensionLoadResult(
            validatedMetadata,
            source?.let { LoadedSource(validatedMetadata, it) })
    }

    fun determineCanSourceBeLoaded(source: Source): Boolean {
        return try {
            val methods = source.javaClass.methods
            val implementsRequiredMethods = Source::class.java.methods
                .filter { Modifier.isAbstract(it.modifiers) }.all { required ->
                    methods.any { implementation ->
                        implementation.name == required.name &&
                                implementation.parameterTypes.contentEquals(required.parameterTypes) &&
                                !Modifier.isAbstract(implementation.modifiers) &&
                                required.returnType.isAssignableFrom(implementation.returnType)
                    }
                }
            // The app's inherited getter does not declare the extension's API version.
            val versionGetter = source.javaClass.getMethod("getApiVersion")
            implementsRequiredMethods && (versionGetter.declaringClass == Source::class.java ||
                    source.apiVersion >= Source.API_VERSION)
        } catch (_: ReflectiveOperationException) {
            false
        } catch (_: LinkageError) {
            false
        }
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

        if (!determineCanSourceBeLoaded(sourceWithoutPrefs)) {
            return sourceWithoutPrefs
        }

        val settingsKey: String = sourceWithoutPrefs.getExtensionId()
        require(settingsKey.isNotBlank()) { "Extension ID is blank" }
        try {
            UUID.fromString(settingsKey)
        } catch (error: IllegalArgumentException) {
            throw IllegalArgumentException("Extension ID is not a valid UUID: $settingsKey", error)
        }

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
        currentMetadata: ExtensionMetadata
    ): ExtensionEntity {
        return extensionDao.getById(UUID.fromString(currentMetadata.extensionId))
            ?: throw IllegalArgumentException("No matching database entry found for metadata: ${currentMetadata.name}")
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
