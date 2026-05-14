package com.eren76.mangly.composables.screens.settings

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.eren76.mangly.BackupExportManager
import com.eren76.mangly.BackupImportManager
import com.eren76.mangly.di.FileManagersEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class SettingsBackupState(
    val context: Context,
    val coroutineScope: CoroutineScope,
    val backupExportManager: BackupExportManager,
    val backupImportManager: BackupImportManager,
) {
    var pendingImportUri by mutableStateOf<Uri?>(null)
        private set

    var pendingImportToken by mutableStateOf<String?>(null)
        private set

    var pendingExtensionConflicts by mutableStateOf<List<BackupImportManager.ExtensionZipConflict>>(
        emptyList()
    )
        private set

    var extensionConflictSelections by mutableStateOf<Map<String, BackupImportManager.ConflictResolution>>(
        emptyMap()
    )
        private set

    fun exportBackup(uri: Uri) {
        coroutineScope.launch {
            runCatching {
                backupExportManager.export(
                    outputUri = uri,
                )
            }.onSuccess {
                Toast.makeText(context, "Backup exported", Toast.LENGTH_LONG).show()
            }.onFailure { e ->
                Toast.makeText(
                    context,
                    "Failed to export backup: ${e.message ?: "Unknown error"}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun selectImportBackup(uri: Uri?) {
        if (uri == null) return

        val fileName = runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getString(
                        cursor.getColumnIndexOrThrow(
                            OpenableColumns.DISPLAY_NAME
                        )
                    )
                } else {
                    null
                }
            }
        }.getOrNull()

        val isValidBackup =
            fileName?.endsWith(".manglybackup", ignoreCase = true) == true

        if (!isValidBackup) {
            Toast.makeText(
                context,
                "Please select a valid .manglybackup file",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        pendingImportUri = uri
    }

    fun dismissImportConfirmation() {
        pendingImportUri = null
    }

    fun startConflictResolution(
        token: String,
        conflicts: List<BackupImportManager.ExtensionZipConflict>,
    ) {
        pendingImportToken = token
        pendingExtensionConflicts = conflicts
        extensionConflictSelections =
            conflicts.associate { it.fileName to BackupImportManager.ConflictResolution.KEEP_OLD }
    }

    fun updateExtensionConflictSelections(
        selections: Map<String, BackupImportManager.ConflictResolution>,
    ) {
        extensionConflictSelections = selections
    }

    fun dismissConflictResolution() {
        pendingImportToken = null
        pendingExtensionConflicts = emptyList()
        extensionConflictSelections = emptyMap()
    }
}

@Composable
internal fun rememberBackupSettingsState(): SettingsBackupState {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val backupExportManager: BackupExportManager = remember {
        val appContext = context.applicationContext
        val entryPoint =
            EntryPointAccessors.fromApplication(appContext, FileManagersEntryPoint::class.java)
        entryPoint.backupManager()
    }

    val backupImportManager: BackupImportManager = remember {
        val appContext = context.applicationContext
        val entryPoint =
            EntryPointAccessors.fromApplication(appContext, FileManagersEntryPoint::class.java)
        entryPoint.backupImportManager()
    }

    return remember(
        context,
        coroutineScope,
        backupExportManager,
        backupImportManager,
    ) {
        SettingsBackupState(
            context = context,
            coroutineScope = coroutineScope,
            backupExportManager = backupExportManager,
            backupImportManager = backupImportManager,
        )
    }
}
