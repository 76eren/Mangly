package com.eren76.mangly.viewmodels

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eren76.mangly.BackupExportManager
import com.eren76.mangly.BackupImportManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "BackupSettings"

@HiltViewModel
class BackupSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupExportManager: BackupExportManager,
    private val backupImportManager: BackupImportManager,
) : ViewModel() {

    // This triggers the import confirmation dialog when set to a non-null value
    var pendingImportUri by mutableStateOf<Uri?>(null)
        private set

    // This is set when the import validation detects extension conflicts, and is used to continue the import after the user resolves the conflicts
    var pendingImportToken by mutableStateOf<String?>(null)
        private set

    // This is set when the import validation detects extension conflicts, and is used to display the conflicts to the user for resolution
    var pendingExtensionConflicts by mutableStateOf<List<BackupImportManager.ExtensionZipConflict>>(
        emptyList()
    )
        private set

    var extensionConflictSelections by mutableStateOf<Map<String, BackupImportManager.ConflictResolution>>(
        emptyMap()
    )
        private set

    var isImportRunning by mutableStateOf(false)
        private set

    var isExportRunning by mutableStateOf(false)
        private set

    fun exportBackup(uri: Uri) {
        if (isExportRunning || isImportRunning) return

        isExportRunning = true
        viewModelScope.launch {
            try {
                backupExportManager.export(outputUri = uri)
                showToast("Backup exported")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                showToast("Failed to export backup: ${error.userMessage()}")
            } finally {
                isExportRunning = false
            }
        }
    }

    fun selectImportBackup(uri: Uri?) {
        if (uri == null) return

        val fileName = queryDisplayName(uri)
        if (fileName?.endsWith(".manglybackup", ignoreCase = true) != true) {
            showToast("Please select a valid .manglybackup file")
            return
        }

        pendingImportUri = uri
    }

    fun dismissImportConfirmation() {
        pendingImportUri = null
    }

    fun startImport(uri: Uri) {
        if (isImportRunning || isExportRunning) return

        pendingImportUri = null
        isImportRunning = true
        viewModelScope.launch {
            Log.i(TAG, "Starting backup import validation")
            try {
                when (val result: BackupImportManager.StartImportResult =
                    backupImportManager.startImport(inputUri = uri)) {
                    is BackupImportManager.StartImportResult.Ready -> {
                        Log.i(TAG, "Backup validated without extension conflicts")
                        backupImportManager.continueImport(token = result.token)
                    }

                    is BackupImportManager.StartImportResult.NeedsExtensionConflictResolution -> {
                        Log.i(
                            TAG,
                            "Backup validated with ${result.conflicts.size} extension conflicts"
                        )
                        showExtensionConflicts(result.token, result.conflicts)
                    }
                }
            } catch (error: CancellationException) {
                Log.i(TAG, "Backup import cancelled by its lifecycle owner")
                throw error
            } catch (error: Exception) {
                Log.e(TAG, "Backup import failed", error)
                showToast("Failed to import backup: ${error.userMessage()}")
            } finally {
                isImportRunning = false
            }
        }
    }

    fun updateExtensionConflictSelections(
        selections: Map<String, BackupImportManager.ConflictResolution>,
    ) {
        extensionConflictSelections = selections
    }

    fun overwriteAllConflicts() {
        continueImport(overwriteAllConflicts = true)
    }

    fun skipAllConflicts() {
        continueImport(skipAllConflicts = true)
    }

    fun continueWithSelectedConflicts() {
        continueImport(conflictResolutions = extensionConflictSelections)
    }

    fun cancelConflictResolution() {
        val token = pendingImportToken ?: return
        clearExtensionConflicts()

        viewModelScope.launch {
            try {
                backupImportManager.cancelImport(token)
                Log.i(TAG, "Pending backup import cancelled")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e(TAG, "Failed to clean up cancelled backup import", error)
            }
        }
    }

    private fun continueImport(
        conflictResolutions: Map<String, BackupImportManager.ConflictResolution> = emptyMap(),
        overwriteAllConflicts: Boolean = false,
        skipAllConflicts: Boolean = false,
    ) {
        val token = pendingImportToken ?: return
        if (isImportRunning) return

        clearExtensionConflicts()
        isImportRunning = true
        viewModelScope.launch {
            Log.i(TAG, "Continuing validated backup import")
            try {
                backupImportManager.continueImport(
                    token = token,
                    conflictResolutions = conflictResolutions,
                    overwriteAllConflicts = overwriteAllConflicts,
                    skipAllConflicts = skipAllConflicts,
                )
            } catch (error: CancellationException) {
                Log.i(TAG, "Backup restore cancelled by its lifecycle owner")
                throw error
            } catch (error: Exception) {
                Log.e(TAG, "Backup restore failed", error)
                showToast("Failed to import backup: ${error.userMessage()}")
            } finally {
                isImportRunning = false
            }
        }
    }

    private fun showExtensionConflicts(
        token: String,
        conflicts: List<BackupImportManager.ExtensionZipConflict>,
    ) {
        // This triggers the extension conflict resolution dialog to be displayed
        pendingImportToken = token
        pendingExtensionConflicts = conflicts
        extensionConflictSelections = conflicts.associate { conflict ->
            conflict.fileName to BackupImportManager.ConflictResolution.KEEP_OLD
        }
    }

    private fun clearExtensionConflicts() {
        pendingImportToken = null
        pendingExtensionConflicts = emptyList()
        extensionConflictSelections = emptyMap()
    }

    private fun queryDisplayName(uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                } else {
                    null
                }
            }
        }.onFailure { error ->
            Log.w(TAG, "Unable to read selected backup display name", error)
        }.getOrNull()
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    private fun Throwable.userMessage(): String = message ?: "Unknown error"
}
