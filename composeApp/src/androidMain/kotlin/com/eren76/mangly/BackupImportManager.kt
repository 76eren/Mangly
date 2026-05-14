package com.eren76.mangly

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.room.withTransaction
import com.eren76.mangly.rooms.database.APP_DATABASE_VERSION
import com.eren76.mangly.rooms.database.AppDatabase
import com.eren76.manglyextension.plugins.PluginMetadata
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupImportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
) {

    companion object {
        private const val BUFFER_SIZE = 64 * 1024
        private const val SUPPORTED_FORMAT_VERSION = 2

        private val DATABASE_ENTRY_NAMES = setOf(
            "database.sqlite",
            "database.sqlite-wal",
            "database.sqlite-shm",
        )
    }

    private val gson: Gson = GsonBuilder()
        .disableHtmlEscaping()
        .create()


    data class ExtensionZipConflict(
        val extensionName: String?,
        val fileName: String,
        val existingVersion: String?,
        val incomingVersion: String?,
    )

    enum class ConflictResolution {
        KEEP_OLD,
        USE_NEW,
    }

    sealed interface StartImportResult {
        data class Ready(val token: String) : StartImportResult

        data class NeedsExtensionConflictResolution(
            val token: String,
            val conflicts: List<ExtensionZipConflict>,
        ) : StartImportResult
    }

    private data class PendingImport(
        val zip: Map<String, ByteArray>,
        val databaseName: String,
        val prefsBytes: ByteArray,
    )

    private val pendingImports = ConcurrentHashMap<String, PendingImport>()

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

    suspend fun startImport(
        inputUri: Uri,
        databaseName: String = "app_database",
    ): StartImportResult = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val zip = resolver.openInputStream(inputUri)
            ?.let { BufferedInputStream(it, BUFFER_SIZE) }
            ?.use { input: BufferedInputStream ->
                readZip(input)
            } ?: error("Unable to open input stream")

        val metadataBytes = zip["metadata.json"]
            ?: error("Backup is missing metadata.json")

        val metadata = gson.fromJson(
            metadataBytes.toString(Charsets.UTF_8),
            BackupMetadata::class.java
        )

        require(metadata.formatVersion == SUPPORTED_FORMAT_VERSION) {
            "Unsupported backup format version ${metadata.formatVersion}"
        }

        metadata.databaseUserVersion?.let { backupDatabaseVersion ->
            require(backupDatabaseVersion <= APP_DATABASE_VERSION) {
                "This backup was created by a newer version of Mangly. " +
                        "Please update the app before importing it."
            }
        }

        for (dbEntry in metadata.databaseFiles) {
            require(dbEntry in DATABASE_ENTRY_NAMES) {
                "Unexpected database entry name: $dbEntry"
            }
            require(zip.containsKey(dbEntry)) {
                "Backup missing database file: $dbEntry"
            }
        }

        for ((entryName, expectedHash) in metadata.databaseSha256) {
            val actualHash = sha256Hex(zip[entryName] ?: error("Missing $entryName"))
            require(actualHash.equals(expectedHash, true)) {
                "Hash mismatch for $entryName"
            }
        }

        val prefsBytes = zip["preferences.json"]
            ?: error("Backup missing preferences.json")

        val prefsHash = sha256Hex(prefsBytes)
        require(prefsHash.equals(metadata.preferencesSha256, true)) {
            "Hash mismatch for preferences.json"
        }

        require(metadata.overallContentSha256.length == 64) {
            "Invalid overall hash"
        }

        val token = UUID.randomUUID().toString()
        pendingImports[token] = PendingImport(
            zip = zip,
            databaseName = databaseName,
            prefsBytes = prefsBytes,
        )

        val conflicts: List<ExtensionZipConflict> = detectExtensionZipConflicts(zip)
        if (conflicts.isNotEmpty()) {
            StartImportResult.NeedsExtensionConflictResolution(token, conflicts)
        } else {
            StartImportResult.Ready(token)
        }
    }

    suspend fun continueImport(
        token: String,
        conflictResolutions: Map<String, ConflictResolution> = emptyMap(),
        overwriteAllConflicts: Boolean = false,
        skipAllConflicts: Boolean = false,
    ): Unit = withContext(Dispatchers.IO) {

        require(!(overwriteAllConflicts && skipAllConflicts)) {
            "Only one of overwriteAllConflicts/skipAllConflicts can be true"
        }

        val pending = pendingImports.remove(token)
            ?: error("Import session expired. Please start import again.")

        runCatching {
            database.openHelper
                .writableDatabase
                .query("PRAGMA wal_checkpoint(FULL)")
                .close()
        }

        database.withTransaction { }

        restoreDatabaseFiles(
            zip = pending.zip,
            databaseName = pending.databaseName,
        )

        restoreSharedPreferencesFromJson(pending.prefsBytes)

        restoreFilesDir(
            zip = pending.zip,
            conflictResolutions = conflictResolutions,
            overwriteAllConflicts = overwriteAllConflicts,
            skipAllConflicts = skipAllConflicts,
        )

        // TODO: Probably not the best idea to stop the flow from here
        restartApp()
    }

    private fun restoreDatabaseFiles(
        zip: Map<String, ByteArray>,
        databaseName: String,
    ) {
        val dbDir = context.getDatabasePath(databaseName).parentFile
            ?: error("Unable to resolve database directory")

        if (!dbDir.exists()) dbDir.mkdirs()

        val mapping = mapOf(
            "database.sqlite" to databaseName,
            "database.sqlite-wal" to "$databaseName-wal",
            "database.sqlite-shm" to "$databaseName-shm",
        )

        for ((entryName, outName) in mapping) {
            val bytes = zip[entryName] ?: continue
            val outFile = File(dbDir, outName)
            writeAtomically(outFile, bytes)
        }
    }


    private fun restoreSharedPreferencesFromJson(
        prefsJsonBytes: ByteArray,
    ) {
        val prefsDir = File(
            context.applicationInfo.dataDir,
            "shared_prefs"
        )

        if (!prefsDir.exists()) prefsDir.mkdirs()

        prefsDir.listFiles()?.forEach { file ->
            if (file.isFile && file.extension.equals("xml", true)) {
                file.delete()
            }
        }

        val mapType = Map::class.java

        @Suppress("UNCHECKED_CAST")
        val allPrefs = gson.fromJson(
            prefsJsonBytes.toString(Charsets.UTF_8),
            mapType,
        ) as Map<String, Any?>

        for ((prefsName, valuesAny) in allPrefs) {
            @Suppress("UNCHECKED_CAST")
            val values = valuesAny as? Map<String, Any?> ?: emptyMap()

            val prefs = context.getSharedPreferences(
                prefsName,
                Context.MODE_PRIVATE
            )

            val editor = prefs.edit().clear()

            for ((key, wrappedAny) in values) {
                @Suppress("UNCHECKED_CAST")
                val wrapped = wrappedAny as? Map<String, Any?> ?: continue

                val type = wrapped["type"] as? String ?: continue
                val value = wrapped["value"]

                when (type) {
                    "null" -> editor.remove(key)

                    "boolean" -> {
                        if (value is Boolean) {
                            editor.putBoolean(key, value)
                        }
                    }

                    "int" -> {
                        if (value is Number) {
                            editor.putInt(key, value.toInt())
                        }
                    }

                    "long" -> {
                        if (value is Number) {
                            editor.putLong(key, value.toLong())
                        }
                    }

                    "float" -> {
                        if (value is Number) {
                            editor.putFloat(key, value.toFloat())
                        }
                    }

                    "string" -> {
                        editor.putString(key, value?.toString())
                    }

                    "string_set" -> {
                        val set = (value as? List<*>)
                            ?.filterIsInstance<String>()
                            ?.toSet()
                            ?: emptySet()

                        editor.putStringSet(key, set)
                    }
                }
            }

            check(editor.commit()) {
                "Failed to restore shared preferences: $prefsName"
            }
        }
    }

    private fun restoreFilesDir(
        zip: Map<String, ByteArray>,
        conflictResolutions: Map<String, ConflictResolution>,
        overwriteAllConflicts: Boolean,
        skipAllConflicts: Boolean,
    ) {
        val root = context.filesDir
        if (!root.exists()) root.mkdirs()

        // destructive restore: wipe everything except the extension zip files
        root.listFiles()?.forEach { child ->
            val keep = child.isFile && child.extension.equals("zip", true)
            if (!keep) {
                child.deleteRecursively()
            }
        }

        // Extract "files/..." entries
        for ((name, bytes) in zip) {
            if (!name.startsWith("files/")) continue
            val relative = name.removePrefix("files/")
            if (relative.isBlank()) continue

            val outFile = File(root, relative)

            // Conflicts lol
            if (outFile.parentFile == root && outFile.extension.equals(
                    "zip",
                    true
                ) && outFile.exists()
            ) {
                if (skipAllConflicts) continue
                if (!overwriteAllConflicts) {
                    val resolution =
                        conflictResolutions[outFile.name] ?: ConflictResolution.KEEP_OLD
                    if (resolution == ConflictResolution.KEEP_OLD) continue
                }
            }

            outFile.parentFile?.let { if (!it.exists()) it.mkdirs() }

            writeAtomically(outFile, bytes)
        }
    }

    private fun detectExtensionZipConflicts(
        zip: Map<String, ByteArray>,
    ): List<ExtensionZipConflict> {

        val root = context.filesDir
        val conflicts = mutableListOf<ExtensionZipConflict>()

        for ((entryName, bytes) in zip) {
            if (!entryName.startsWith("files/")) continue
            val relative = entryName.removePrefix("files/")

            if (!relative.endsWith(".zip", ignoreCase = true)) continue
            if (relative.contains('/')) continue

            val existingFile = File(root, relative)
            if (!existingFile.exists()) continue

            val existingMeta =
                runCatching { readExtensionMeta(existingFile.readBytes()) }.getOrNull()
            val incomingMeta = runCatching { readExtensionMeta(bytes) }.getOrNull()

            conflicts += ExtensionZipConflict(
                extensionName = incomingMeta?.name ?: existingMeta?.name,
                fileName = relative,
                existingVersion = existingMeta?.version,
                incomingVersion = incomingMeta?.version,
            )
        }

        return conflicts.sortedBy { it.fileName.lowercase() }
    }

    private fun readExtensionMeta(zipBytes: ByteArray): PluginMetadata? {
        val zipContents = readZip(zipBytes.inputStream())
        val metaBytes = zipContents["meta/plugin.json"] ?: return null
        return gson.fromJson(metaBytes.toString(Charsets.UTF_8), PluginMetadata::class.java)
    }

    private fun readZip(input: InputStream): Map<String, ByteArray> {
        val map = linkedMapOf<String, ByteArray>()

        ZipInputStream(input).use { zis ->
            while (true) {
                val entry: ZipEntry = zis.nextEntry ?: break
                if (entry.isDirectory) continue

                val buffer = ByteArrayOutputStream(
                    if (entry.size > 0 && entry.size < Int.MAX_VALUE) entry.size.toInt() else 32 * 1024
                )

                // Note: we validate via sha256Hex(bytes) later.
                zis.copyTo(buffer, BUFFER_SIZE)
                map[entry.name] = buffer.toByteArray()
            }
        }

        return map
    }

    private fun writeAtomically(
        outFile: File,
        bytes: ByteArray,
    ) {
        val parent = outFile.parentFile
        if (parent != null && !parent.exists()) parent.mkdirs()

        val tmp = File(parent, outFile.name + ".tmp")
        tmp.outputStream().buffered(BUFFER_SIZE).use { it.write(bytes) }

        if (outFile.exists()) {
            if (!tmp.renameTo(outFile)) {
                outFile.delete()
                tmp.renameTo(outFile)
            }
        } else {
            tmp.renameTo(outFile)
        }
    }

    private fun sha256Hex(bytes: ByteArray): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { b -> "%02x".format(b) }
    }

    private fun OutputStream.buffered(size: Int): BufferedOutputStream =
        BufferedOutputStream(this, size)

    private fun restartApp() {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)

        intent?.apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
        }

        context.startActivity(intent)

        if (context is Activity) {
            context.finishAffinity()
        }

        Runtime.getRuntime().exit(0)

    }
}


