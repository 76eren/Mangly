package com.eren76.mangly.composables.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun BackupSettingsSection(
    onExportRequested: (Uri) -> Unit,
    onImportRequested: () -> Unit,
) {
    BackupExportSetting(
        onExportRequested = onExportRequested
    )

    BackupImportSetting(
        onImportRequested = onImportRequested
    )
}

@Composable
private fun BackupExportSetting(
    onExportRequested: (Uri) -> Unit,
) {
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*"),
        onResult = { uri: Uri? ->
            if (uri != null) onExportRequested(uri)
        }
    )

    ElevatedCard(
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = "Export settings and data",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Creates a .manglybackup file you can restore later",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            FilledTonalButton(
                onClick = {
                    val suggestedName = "mangly_${System.currentTimeMillis()}.manglybackup"
                    createBackupLauncher.launch(suggestedName)
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Export")
            }
        }
    }
}

@Composable
private fun BackupImportSetting(
    onImportRequested: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(
                    text = "Import settings and data",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Restores from a .manglybackup file (overwrites everything)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            FilledTonalButton(
                onClick = onImportRequested,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Import")
            }
        }
    }
}
