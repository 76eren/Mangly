package com.eren76.mangly

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.room.withTransaction
import com.eren76.mangly.rooms.database.APP_DATABASE_VERSION
import com.eren76.mangly.rooms.database.AppDatabase
import com.eren76.manglyextension.plugins.PluginMetadata
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.DigestOutputStream
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
        private const val PROGRESS_LOG_INTERVAL_BYTES = 256L * 1024 * 1024
        private const val SUPPORTED_FORMAT_VERSION = 2
        private const val MAX_ARCHIVE_ENTRIES = 100_000
        private const val MAX_METADATA_BYTES = 1024 * 1024L
        private const val MAX_PREFERENCES_BYTES = 8 * 1024 * 1024L
        private const val MAX_PLUGIN_METADATA_BYTES = 1024 * 1024L
        private const val IMPORT_STAGING_DIRECTORY = "backup-imports"
        private const val TAG = "BackupImport"

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
        val stagingDirectory: File,
        val entries: Map<String, File>,
        val databaseName: String,
    )

    private data class StagedArchive(
        val entries: Map<String, File>,
        val sha256ByEntry: Map<String, String>,
        val totalBytes: Long,
    )

    private val pendingImports = ConcurrentHashMap<String, PendingImport>()
    private val stagingDirectoryLock = Any()
    private var orphanedStagingCleanupDone = false

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
        val token = UUID.randomUUID().toString()
        val importId = token.take(8)
        val startedAt = SystemClock.elapsedRealtime()
        Log.i(TAG, "Import $importId started staging and validation")
        val stagingDirectory = createStagingDirectory(token)

        try {
            val resolver = context.contentResolver
            val archive = resolver.openInputStream(inputUri)
                ?.let { BufferedInputStream(it, BUFFER_SIZE) }
                ?.use { input -> stageZip(input, stagingDirectory, importId) }
                ?: error("Unable to open input stream")
            Log.i(
                TAG,
                "Import $importId staged ${archive.entries.size} files " +
                        "and ${archive.totalBytes.toMiB()} MiB in " +
                        "${SystemClock.elapsedRealtime() - startedAt} ms"
            )

            val metadataFile = archive.entries["metadata.json"]
                ?: error("Backup is missing metadata.json")
            val metadata = gson.fromJson(
                readLimitedBytes(
                    file = metadataFile,
                    maxBytes = MAX_METADATA_BYTES,
                    description = "metadata.json",
                ).toString(Charsets.UTF_8),
                BackupMetadata::class.java,
            ) ?: error("Backup metadata is invalid")

            require(metadata.formatVersion == SUPPORTED_FORMAT_VERSION) {
                "Unsupported backup format version ${metadata.formatVersion}"
            }

            metadata.databaseUserVersion?.let { backupDatabaseVersion ->
                require(backupDatabaseVersion <= APP_DATABASE_VERSION) {
                    "This backup was created by a newer version of Mangly. " +
                            "Please update the app before importing it."
                }
            }

            require(metadata.databaseFiles.isNotEmpty()) {
                "Backup does not contain a database"
            }
            for (dbEntry in metadata.databaseFiles) {
                require(dbEntry in DATABASE_ENTRY_NAMES) {
                    "Unexpected database entry name: $dbEntry"
                }
                require(archive.entries.containsKey(dbEntry)) {
                    "Backup missing database file: $dbEntry"
                }

                val expectedHash = metadata.databaseSha256[dbEntry]
                    ?: error("Backup metadata is missing the hash for $dbEntry")
                val actualHash = archive.sha256ByEntry.getValue(dbEntry)
                require(actualHash.equals(expectedHash, true)) {
                    "Hash mismatch for $dbEntry"
                }
            }

            require(metadata.databaseSha256.keys == metadata.databaseFiles.toSet()) {
                "Backup database hash list does not match its database files"
            }

            val preferencesFile = archive.entries["preferences.json"]
                ?: error("Backup missing preferences.json")
            require(preferencesFile.length() <= MAX_PREFERENCES_BYTES) {
                "preferences.json is too large"
            }
            val preferencesHash = archive.sha256ByEntry.getValue("preferences.json")
            require(preferencesHash.equals(metadata.preferencesSha256, true)) {
                "Hash mismatch for preferences.json"
            }

            require(metadata.overallContentSha256.matches(Regex("[0-9a-fA-F]{64}"))) {
                "Invalid overall hash"
            }
            val contentHashes = archive.sha256ByEntry
                .filterKeys { it != "metadata.json" }
                .values
                .sorted()
            val actualOverallHash = sha256Hex(
                gson.toJson(contentHashes).toByteArray(Charsets.UTF_8)
            )
            require(actualOverallHash.equals(metadata.overallContentSha256, true)) {
                "Backup content hash mismatch"
            }

            val conflicts = detectExtensionZipConflicts(archive.entries)
            pendingImports[token] = PendingImport(
                stagingDirectory = stagingDirectory,
                entries = archive.entries,
                databaseName = databaseName,
            )

            Log.i(
                TAG,
                "Import $importId validated successfully with ${conflicts.size} conflicts"
            )
            if (conflicts.isNotEmpty()) {
                StartImportResult.NeedsExtensionConflictResolution(token, conflicts)
            } else {
                StartImportResult.Ready(token)
            }
        } catch (error: CancellationException) {
            pendingImports.remove(token)
            deleteStagingDirectory(stagingDirectory, importId)
            Log.i(TAG, "Import $importId cancelled during staging or validation")
            throw error
        } catch (throwable: Throwable) {
            pendingImports.remove(token)
            deleteStagingDirectory(stagingDirectory, importId)
            Log.e(TAG, "Import $importId failed during staging or validation", throwable)
            throw throwable
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

        val pending: PendingImport = pendingImports.remove(token)
            ?: error("Import session expired. Please start import again.")
        val importId: String = token.take(8)
        val startedAt: Long = SystemClock.elapsedRealtime()
        Log.i(TAG, "Import $importId started restoring validated data")

        try {
            runCatching {
                database.openHelper
                    .writableDatabase
                    .query("PRAGMA wal_checkpoint(FULL)")
                    .close()
            }.onFailure { error ->
                Log.w(TAG, "Import $importId could not checkpoint the current database", error)
            }

            database.withTransaction { }

            restoreDatabaseFiles(
                entries = pending.entries,
                databaseName = pending.databaseName,
            )
            Log.d(TAG, "Import $importId restored database files")

            val preferencesFile: File = pending.entries["preferences.json"]
                ?: error("Backup missing preferences.json")
            restoreSharedPreferencesFromJson(
                readLimitedBytes(
                    file = preferencesFile,
                    maxBytes = MAX_PREFERENCES_BYTES,
                    description = "preferences.json",
                )
            )
            Log.d(TAG, "Import $importId restored shared preferences")

            restoreFilesDir(
                entries = pending.entries,
                conflictResolutions = conflictResolutions,
                overwriteAllConflicts = overwriteAllConflicts,
                skipAllConflicts = skipAllConflicts,
            )
            Log.d(TAG, "Import $importId restored application files")
        } catch (error: CancellationException) {
            Log.i(TAG, "Import $importId cancelled during restore")
            throw error
        } catch (error: Throwable) {
            Log.e(TAG, "Import $importId failed during restore", error)
            throw error
        } finally {
            deleteStagingDirectory(pending.stagingDirectory, importId)
        }

        Log.i(
            TAG,
            "Import $importId completed in ${SystemClock.elapsedRealtime() - startedAt} ms"
        )
        restartApp()
    }

    suspend fun cancelImport(token: String): Unit = withContext(Dispatchers.IO) {
        val importId = token.take(8)
        val pending = pendingImports.remove(token)
        if (pending == null) {
            Log.d(TAG, "Import $importId was already removed")
            return@withContext
        }

        deleteStagingDirectory(pending.stagingDirectory, importId)
        Log.i(TAG, "Import $importId cancelled while awaiting conflict resolution")
    }

    private fun restoreDatabaseFiles(
        entries: Map<String, File>,
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
            val stagedFile = entries[entryName] ?: continue
            val outFile = File(dbDir, outName)
            writeAtomically(outFile, stagedFile)
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
        entries: Map<String, File>,
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
        for ((name, stagedFile) in entries) {
            if (!name.startsWith("files/")) continue
            val relative = name.removePrefix("files/")
            if (relative.isBlank()) continue

            val outFile = File(root, relative)
            require(isInsideDirectory(root, outFile)) {
                "Unsafe file path in backup: $name"
            }

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

            writeAtomically(outFile, stagedFile)
        }
    }

    private fun detectExtensionZipConflicts(
        entries: Map<String, File>,
    ): List<ExtensionZipConflict> {

        val root = context.filesDir
        val conflicts = mutableListOf<ExtensionZipConflict>()

        for ((entryName, stagedFile) in entries) {
            if (!entryName.startsWith("files/")) continue
            val relative = entryName.removePrefix("files/")

            if (!relative.endsWith(".zip", ignoreCase = true)) continue
            if (relative.contains('/')) continue

            val existingFile = File(root, relative)
            if (!existingFile.exists()) continue

            val existingMeta: PluginMetadata? =
                runCatching { readExtensionMeta(existingFile) }.getOrNull()
            val incomingMeta: PluginMetadata? =
                runCatching { readExtensionMeta(stagedFile) }.getOrNull()

            conflicts += ExtensionZipConflict(
                extensionName = incomingMeta?.name ?: existingMeta?.name,
                fileName = relative,
                existingVersion = existingMeta?.version,
                incomingVersion = incomingMeta?.version,
            )
        }

        return conflicts.sortedBy { it.fileName.lowercase() }
    }

    private fun readExtensionMeta(zipFile: File): PluginMetadata? {
        ZipInputStream(
            BufferedInputStream(FileInputStream(zipFile), BUFFER_SIZE)
        ).use { zipInput ->
            while (true) {
                val entry = zipInput.nextEntry ?: return null
                if (!entry.isDirectory && entry.name == "meta/plugin.json") {
                    val bytes = readLimitedBytes(
                        input = zipInput,
                        maxBytes = MAX_PLUGIN_METADATA_BYTES,
                        description = "extension metadata",
                    )
                    return gson.fromJson(
                        bytes.toString(Charsets.UTF_8),
                        PluginMetadata::class.java,
                    )
                }
                zipInput.closeEntry()
            }
        }
    }

    private fun stageZip(
        input: InputStream,
        stagingDirectory: File,
        importId: String,
    ): StagedArchive {
        val entries = linkedMapOf<String, File>()
        val hashes = linkedMapOf<String, String>()
        val seenNames = hashSetOf<String>()
        var entryCount = 0
        var totalBytes = 0L
        var nextProgressLog = PROGRESS_LOG_INTERVAL_BYTES

        ZipInputStream(input).use { zipInput ->
            while (true) {
                val entry: ZipEntry = zipInput.nextEntry ?: break
                entryCount++
                require(entryCount <= MAX_ARCHIVE_ENTRIES) {
                    "Backup contains too many entries"
                }

                val entryName = entry.name
                validateEntryName(entryName)
                require(seenNames.add(entryName)) {
                    "Backup contains duplicate entry: $entryName"
                }

                if (entry.isDirectory) {
                    zipInput.closeEntry()
                    continue
                }

                val stagedFile = File(stagingDirectory, entryName)
                require(isInsideDirectory(stagingDirectory, stagedFile)) {
                    "Unsafe file path in backup: $entryName"
                }
                stagedFile.parentFile?.let { parent ->
                    check(parent.exists() || parent.mkdirs()) {
                        "Unable to create temporary import directory"
                    }
                }

                val digest = MessageDigest.getInstance("SHA-256")
                DigestOutputStream(
                    BufferedOutputStream(FileOutputStream(stagedFile), BUFFER_SIZE),
                    digest,
                ).use { output ->
                    copyEntryWithLimit(
                        input = zipInput,
                        output = output,
                        maxBytes = when (entryName) {
                            "metadata.json" -> MAX_METADATA_BYTES
                            "preferences.json" -> MAX_PREFERENCES_BYTES
                            else -> Long.MAX_VALUE
                        },
                        description = entryName,
                        onBytesCopied = { copiedBytes ->
                            totalBytes += copiedBytes
                            if (totalBytes >= nextProgressLog) {
                                Log.d(
                                    TAG,
                                    "Import $importId staged ${totalBytes.toMiB()} MiB"
                                )
                                nextProgressLog =
                                    ((totalBytes / PROGRESS_LOG_INTERVAL_BYTES) + 1) *
                                            PROGRESS_LOG_INTERVAL_BYTES
                            }
                        },
                    )
                }

                entries[entryName] = stagedFile
                hashes[entryName] = digest.digest().toHex()
                zipInput.closeEntry()
            }
        }

        return StagedArchive(entries, hashes, totalBytes)
    }

    private fun validateEntryName(entryName: String) {
        require(entryName.isNotBlank()) { "Backup contains an empty entry name" }
        require(
            '\\' !in entryName &&
                    !entryName.startsWith('/') &&
                    "//" !in entryName
        ) {
            "Unsafe file path in backup: $entryName"
        }
        require(entryName.split('/').none { it == "." || it == ".." }) {
            "Unsafe file path in backup: $entryName"
        }
        require(
            entryName == "metadata.json" ||
                    entryName == "preferences.json" ||
                    entryName in DATABASE_ENTRY_NAMES ||
                    entryName.startsWith("files/")
        ) {
            "Unexpected backup entry: $entryName"
        }
    }

    private fun createStagingDirectory(token: String): File {
        return synchronized(stagingDirectoryLock) {
            val stagingRoot = File(context.cacheDir, IMPORT_STAGING_DIRECTORY)
            if (!orphanedStagingCleanupDone) {
                stagingRoot.listFiles()?.forEach { it.deleteRecursively() }
                orphanedStagingCleanupDone = true
            }

            val stagingDirectory = File(stagingRoot, token)
            check(stagingDirectory.mkdirs()) {
                "Unable to create temporary import directory"
            }
            stagingDirectory
        }
    }

    private fun copyEntryWithLimit(
        input: InputStream,
        output: OutputStream,
        maxBytes: Long,
        description: String,
        onBytesCopied: (Int) -> Unit = {},
    ): Long {
        val buffer = ByteArray(BUFFER_SIZE)
        var totalBytes = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            totalBytes += read
            require(totalBytes <= maxBytes) { "$description is too large" }
            output.write(buffer, 0, read)
            onBytesCopied(read)
        }
        return totalBytes
    }

    private fun writeAtomically(
        outFile: File,
        sourceFile: File,
    ) {
        val parent = outFile.parentFile
        if (parent != null) {
            check(parent.exists() || parent.mkdirs()) {
                "Unable to create directory for ${outFile.name}"
            }
        }

        val tmp = File(parent, outFile.name + ".tmp")
        if (tmp.exists()) tmp.delete()

        try {
            FileInputStream(sourceFile).buffered(BUFFER_SIZE).use { input ->
                FileOutputStream(tmp).buffered(BUFFER_SIZE).use { output ->
                    input.copyTo(output, BUFFER_SIZE)
                }
            }

            if (!tmp.renameTo(outFile)) {
                if (outFile.exists()) {
                    check(outFile.delete()) { "Unable to replace ${outFile.name}" }
                }
                check(tmp.renameTo(outFile)) { "Unable to replace ${outFile.name}" }
            }
        } finally {
            if (tmp.exists()) tmp.delete()
        }
    }


    private fun readLimitedBytes(
        file: File,
        maxBytes: Long,
        description: String,
    ): ByteArray {
        require(file.length() <= maxBytes && file.length() <= Int.MAX_VALUE) {
            "$description is too large"
        }
        return file.readBytes()
    }

    private fun readLimitedBytes(
        input: InputStream,
        maxBytes: Long,
        description: String,
    ): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        copyEntryWithLimit(input, output, maxBytes, description)
        return output.toByteArray()
    }

    private fun isInsideDirectory(root: File, child: File): Boolean {
        val rootPath = root.canonicalFile.toPath()
        val childPath = child.canonicalFile.toPath()
        return childPath.startsWith(rootPath) && childPath != rootPath
    }

    private fun deleteStagingDirectory(directory: File, importId: String) {
        if (directory.exists() && !directory.deleteRecursively()) {
            Log.w(TAG, "Import $importId could not completely remove its staging directory")
        }
    }

    private fun Long.toMiB(): Long = this / (1024 * 1024)

    private fun sha256Hex(bytes: ByteArray): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .toHex()
    }

    private fun ByteArray.toHex(): String {
        val chars = CharArray(size * 2)
        val hex = "0123456789abcdef"
        var index = 0
        for (byte in this) {
            val value = byte.toInt() and 0xFF
            chars[index++] = hex[value ushr 4]
            chars[index++] = hex[value and 0x0F]
        }
        return String(chars)
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


