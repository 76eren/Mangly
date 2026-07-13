package com.eren76.mangly

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.eren76.mangly.rooms.database.AppDatabase
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.DigestInputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupExportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
) {

    companion object {
        private const val BUFFER_SIZE = 64 * 1024
        private const val FORMAT_VERSION = 2

        // TODO: make this dynamic
        private val EXCLUDED_DIRECTORIES = setOf(
            Constants.FAVORITE_COVERS_DIRECTORY,
            Constants.HISTORY_COVERS_DIRECTORY,
        )
    }

    private val gson: Gson = GsonBuilder()
        .disableHtmlEscaping()
        .create()

    data class BackupMetadata(
        val formatVersion: Int,
        val createdAtEpochMs: Long,
        val appId: String,
        val databaseName: String,
        val databaseUserVersion: Int?,
        val databaseFiles: List<String>,
        val databaseSha256: Map<String, String>,
        val preferencesSha256: String,
        val overallContentSha256: String,
        val includedRoots: List<String>,
    )

    private data class PreferenceValue(
        val type: String,
        val value: Any?,
    )

    suspend fun export(
        outputUri: Uri,
        databaseName: String = "app_database",
    ) = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver

        resolver.openOutputStream(outputUri, "wt")
            ?.buffered(BUFFER_SIZE)
            ?.use { fileOutput ->

                ZipOutputStream(fileOutput).use { zos ->

                    zos.setLevel(Deflater.DEFAULT_COMPRESSION)

                    val contentHashes = mutableListOf<String>()
                    val databaseHashes = linkedMapOf<String, String>()

                    val databaseUserVersion = runCatching {
                        database.openHelper.readableDatabase.version
                    }.getOrNull()

                    runCatching {
                        database.openHelper
                            .writableDatabase
                            .query("PRAGMA wal_checkpoint(FULL)")
                            .close()
                    }


                    //Ensure Room finishes ongoing transactions.

                    database.withTransaction {
                        Unit
                    }

                    val dbDir = context
                        .getDatabasePath(databaseName)
                        .parentFile

                    val dbFiles = dbDir
                        ?.listFiles()
                        ?.filter {
                            it.name == databaseName ||
                                    it.name == "$databaseName-wal" ||
                                    it.name == "$databaseName-shm"
                        }
                        ?.sortedBy { it.name.lowercase(Locale.US) }
                        .orEmpty()

                    for (file in dbFiles) {

                        val entryName = when {
                            file.name.endsWith("-wal") -> "database.sqlite-wal"
                            file.name.endsWith("-shm") -> "database.sqlite-shm"
                            else -> "database.sqlite"
                        }

                        val hash = addFileToZip(
                            zos = zos,
                            file = file,
                            entryName = entryName,
                        )

                        databaseHashes[entryName] = hash
                        contentHashes += hash
                    }

                    val prefsJson: ByteArray = exportSharedPreferences()

                    val prefsHash = addBytesToZip(
                        zos = zos,
                        bytes = prefsJson,
                        entryName = "preferences.json",
                    )

                    contentHashes += prefsHash

                    exportFilesDirectory(
                        zos = zos,
                        contentHashes = contentHashes,
                    )

                    val overallContentSha256 = sha256Hex(
                        stableJsonArray(contentHashes)
                            .toByteArray(Charsets.UTF_8)
                    )

                    val metadata = BackupMetadata(
                        formatVersion = FORMAT_VERSION,
                        createdAtEpochMs = System.currentTimeMillis(),
                        appId = context.packageName,
                        databaseName = databaseName,
                        databaseUserVersion = databaseUserVersion,
                        databaseFiles = databaseHashes.keys.toList(),
                        databaseSha256 = databaseHashes,
                        preferencesSha256 = prefsHash,
                        overallContentSha256 = overallContentSha256,
                        includedRoots = buildList {
                            addAll(databaseHashes.keys)
                            add("preferences.json")
                            add(
                                "files/* excluding ${Constants.FAVORITE_COVERS_DIRECTORY} " +
                                        "and ${Constants.HISTORY_COVERS_DIRECTORY}"
                            )
                        }
                    )

                    addBytesToZip(
                        zos = zos,
                        bytes = gson.toJson(metadata)
                            .toByteArray(Charsets.UTF_8),
                        entryName = "metadata.json",
                    )

                    zos.finish()
                }
            }
            ?: error("Unable to open output stream")
    }

    private fun exportFilesDirectory(
        zos: ZipOutputStream,
        contentHashes: MutableList<String>,
    ) {

        val filesRoot = context.filesDir

        if (!filesRoot.exists()) {
            return
        }

        filesRoot
            .listFiles()
            ?.filterNot { file ->
                file.isDirectory &&
                        file.name in EXCLUDED_DIRECTORIES
            }
            ?.sortedBy { it.name.lowercase(Locale.US) }
            ?.forEach { file ->

                if (file.isDirectory) {

                    zipDirectory(
                        zos = zos,
                        rootDir = file,
                        zipPrefix = "files/${file.name}/",
                        contentHashes = contentHashes,
                    )

                } else {

                    val hash = addFileToZip(
                        zos = zos,
                        file = file,
                        entryName = "files/${file.name}",
                    )

                    contentHashes += hash
                }
            }
    }

    private fun exportSharedPreferences(): ByteArray {
        val prefsDir = File(
            context.applicationInfo.dataDir,
            "shared_prefs"
        )

        val result = mutableMapOf<String, MutableMap<String, PreferenceValue>>()

        prefsDir.listFiles()?.forEach { file ->
            if (!file.isFile || file.extension != "xml") return@forEach

            val prefsName = file.nameWithoutExtension
            val prefs = context.getSharedPreferences(
                prefsName,
                Context.MODE_PRIVATE
            )

            val values = mutableMapOf<String, PreferenceValue>()

            for ((key, value) in prefs.all) {
                val wrapped = when (value) {
                    null -> PreferenceValue("null", null)
                    is Int -> PreferenceValue("int", value)
                    is Long -> PreferenceValue("long", value)
                    is Float -> PreferenceValue("float", value)
                    is Boolean -> PreferenceValue("boolean", value)
                    is String -> PreferenceValue("string", value)
                    is Set<*> -> PreferenceValue(
                        "string_set",
                        value.filterIsInstance<String>()
                    )

                    else -> PreferenceValue("string", value.toString())
                }

                values[key] = wrapped
            }

            result[prefsName] = values
        }

        return gson.toJson(result).toByteArray(Charsets.UTF_8)
    }

    private fun zipDirectory(
        zos: ZipOutputStream,
        rootDir: File,
        zipPrefix: String,
        contentHashes: MutableList<String>,
    ) {

        val basePath = rootDir.absolutePath.length + 1

        rootDir
            .walkTopDown()
            .filter { it != rootDir }
            .sortedBy { it.absolutePath }
            .forEach { file ->

                val relativePath = file.absolutePath
                    .substring(basePath)
                    .replace(File.separatorChar, '/')

                val entryName = "$zipPrefix$relativePath"

                if (file.isDirectory) {

                    zos.putNextEntry(
                        ZipEntry("$entryName/")
                    )

                    zos.closeEntry()

                } else {

                    val hash = addFileToZip(
                        zos = zos,
                        file = file,
                        entryName = entryName,
                    )

                    contentHashes += hash
                }
            }
    }

    private fun addFileToZip(
        zos: ZipOutputStream,
        file: File,
        entryName: String,
    ): String {

        FileInputStream(file)
            .buffered(BUFFER_SIZE)
            .use { input ->

                return addStreamToZip(
                    zos = zos,
                    input = input,
                    entryName = entryName,
                )
            }
    }

    private fun addBytesToZip(
        zos: ZipOutputStream,
        bytes: ByteArray,
        entryName: String,
    ): String {

        return ByteArrayInputStream(bytes).use { input ->
            addStreamToZip(
                zos = zos,
                input = input,
                entryName = entryName,
            )
        }
    }

    private fun addStreamToZip(
        zos: ZipOutputStream,
        input: InputStream,
        entryName: String,
    ): String {

        val digest = MessageDigest.getInstance("SHA-256")

        zos.putNextEntry(
            ZipEntry(entryName)
        )

        DigestInputStream(input, digest).use { digestStream ->
            digestStream.copyTo(zos, BUFFER_SIZE)
        }

        zos.closeEntry()

        return digest.digest().toHex()
    }

    private fun stableJsonArray(
        values: List<String>,
    ): String {

        return gson.toJson(
            values.sorted()
        )
    }

    private fun sha256Hex(
        bytes: ByteArray,
    ): String {

        return MessageDigest
            .getInstance("SHA-256")
            .digest(bytes)
            .toHex()
    }

    private fun ByteArray.toHex(): String {

        val chars = CharArray(size * 2)
        val hex = "0123456789abcdef"

        var index = 0

        for (b in this) {

            val value = b.toInt() and 0xFF

            chars[index++] = hex[value ushr 4]
            chars[index++] = hex[value and 0x0F]
        }

        return String(chars)
    }

    private fun OutputStream.buffered(
        size: Int,
    ): BufferedOutputStream {

        return BufferedOutputStream(
            this,
            size
        )
    }

    private fun InputStream.buffered(
        size: Int,
    ): BufferedInputStream {

        return BufferedInputStream(
            this,
            size
        )
    }
}
