package com.eren76.mangly.composables.shared.dialogs

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eren76.mangly.BackupImportManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun BackupImportConfirmDialog(
    context: Context,
    coroutineScope: CoroutineScope,
    backupImportManager: BackupImportManager,
    pendingImportUri: Uri?,
    onDismiss: () -> Unit,
    onStarted: (token: String, conflicts: List<BackupImportManager.ExtensionZipConflict>) -> Unit,
) {
    pendingImportUri?.let { uri ->
        ConfirmDialog(
            title = "Import backup?",
            message = "This will OVERWRITE your database, settings, and stored files with the contents of the selected backup.\n\nThis is a checkpoint restore, not a selective import.",
            confirmText = "Import",
            confirmColor = MaterialTheme.colorScheme.error,
            onDismiss = onDismiss,
            onConfirm = {
                onDismiss()
                coroutineScope.launch {
                    try {
                        val result = backupImportManager.startImport(inputUri = uri)
                        when (result) {
                            is BackupImportManager.StartImportResult.Ready -> {
                                backupImportManager.continueImport(token = result.token)

                            }

                            is BackupImportManager.StartImportResult.NeedsExtensionConflictResolution -> {
                                onStarted(result.token, result.conflicts)
                            }
                        }

                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "Failed to import backup: " + (e.message ?: "Unknown error"),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }
}

@Composable
fun BackupExtensionConflictResolutionDialog(
    context: Context,
    coroutineScope: CoroutineScope,
    backupImportManager: BackupImportManager,
    token: String?,
    conflicts: List<BackupImportManager.ExtensionZipConflict>,
    selections: Map<String, BackupImportManager.ConflictResolution>,
    onSelectionsChanged: (Map<String, BackupImportManager.ConflictResolution>) -> Unit,
    onDismiss: () -> Unit,
) {
    if (token == null || conflicts.isEmpty()) return

    @OptIn(ExperimentalMaterial3Api::class)
    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {


            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Extension conflicts",
                    style = MaterialTheme.typography.titleLarge,
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Select which version to keep for each extension.",
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
                    items(conflicts, key = { it.fileName }) { conflict ->
                        val selected = selections[conflict.fileName]
                            ?: BackupImportManager.ConflictResolution.KEEP_OLD

                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Text(
                                text = conflict.extensionName ?: conflict.fileName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )

                            Text(
                                text = "Installed: ${conflict.existingVersion ?: "Unknown"}  |  Backup: ${conflict.incomingVersion ?: "Unknown"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 6.dp)
                            ) {
                                TextButton(
                                    onClick = {
                                        onSelectionsChanged(
                                            selections + (conflict.fileName to BackupImportManager.ConflictResolution.KEEP_OLD)
                                        )
                                    }
                                ) {
                                    Text(
                                        text = "Keep installed",
                                        color = if (selected == BackupImportManager.ConflictResolution.KEEP_OLD)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurface,
                                    )
                                }

                                Spacer(Modifier.width(8.dp))

                                TextButton(
                                    onClick = {
                                        onSelectionsChanged(
                                            selections + (conflict.fileName to BackupImportManager.ConflictResolution.USE_NEW)
                                        )
                                    }
                                ) {
                                    Text(
                                        text = "Use backup",
                                        color = if (selected == BackupImportManager.ConflictResolution.USE_NEW)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }

                        HorizontalDivider()
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    backupImportManager.continueImport(
                                        token = token,
                                        overwriteAllConflicts = true,
                                    )

                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "Failed to import backup: " + (e.message
                                            ?: "Unknown error"),
                                        Toast.LENGTH_LONG
                                    ).show()
                                } finally {
                                    onDismiss()
                                }
                            }
                        }
                    ) { Text("Overwrite all") }

                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    backupImportManager.continueImport(
                                        token = token,
                                        skipAllConflicts = true,
                                    )

                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "Failed to import backup: " + (e.message
                                            ?: "Unknown error"),
                                        Toast.LENGTH_LONG
                                    ).show()
                                } finally {
                                    onDismiss()
                                }
                            }
                        }
                    ) { Text("Skip all") }
                }

                Spacer(Modifier.height(8.dp))

                Row(modifier = Modifier.align(Alignment.End)) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    backupImportManager.continueImport(
                                        token = token,
                                        conflictResolutions = selections,
                                    )

                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "Failed to import backup: " + (e.message
                                            ?: "Unknown error"),
                                        Toast.LENGTH_LONG
                                    ).show()
                                } finally {
                                    onDismiss()
                                }
                            }
                        }
                    ) { Text("Continue") }
                }
            }
        }
    }
}

