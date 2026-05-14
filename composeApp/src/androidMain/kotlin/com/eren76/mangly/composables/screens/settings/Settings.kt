package com.eren76.mangly.composables.screens.settings

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.navigation.NavHostController
import com.eren76.mangly.Constants
import com.eren76.mangly.composables.shared.dialogs.BackupExtensionConflictResolutionDialog
import com.eren76.mangly.composables.shared.dialogs.BackupImportConfirmDialog

@Composable
fun Settings(
    navController: NavHostController
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val backupSettingsState = rememberBackupSettingsState()

    val downloadsPrefs = remember {
        context.getSharedPreferences(
            Constants.READING_SETTING_KEY,
            Context.MODE_PRIVATE
        )
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            backupSettingsState.selectImportBackup(uri)
        }
    )

    var isDownloadModeEnabled by remember {
        mutableStateOf(
            downloadsPrefs.getBoolean(Constants.MANGLY_ENBALE_DOWNLOADS, false)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        AppearanceSettingsSection()

        SettingsDivider(top = 20.dp)

        ReaderSettingsSection()

        SettingsDivider()

        LibrarySettingsSection(
            navController = navController,
            isDownloadModeEnabled = isDownloadModeEnabled,
            onDownloadModeChanged = {
                isDownloadModeEnabled = it
                downloadsPrefs.edit { putBoolean(Constants.MANGLY_ENBALE_DOWNLOADS, it) }
            }
        )

        SettingsDivider()

        BackupSettingsSection(
            onExportRequested = backupSettingsState::exportBackup,
            onImportRequested = {
                importBackupLauncher.launch(arrayOf("*/*"))
            }
        )
    }

    SettingsBackupDialogs(backupSettingsState)
}

@Composable
private fun SettingsBackupDialogs(
    backupSettingsState: SettingsBackupState,
) {
    BackupImportConfirmDialog(
        context = backupSettingsState.context,
        coroutineScope = backupSettingsState.coroutineScope,
        backupImportManager = backupSettingsState.backupImportManager,
        pendingImportUri = backupSettingsState.pendingImportUri,
        onDismiss = backupSettingsState::dismissImportConfirmation,
        onStartBackupConflictResolution = backupSettingsState::startConflictResolution,
    )

    BackupExtensionConflictResolutionDialog(
        context = backupSettingsState.context,
        coroutineScope = backupSettingsState.coroutineScope,
        backupImportManager = backupSettingsState.backupImportManager,
        token = backupSettingsState.pendingImportToken,
        conflicts = backupSettingsState.pendingExtensionConflicts,
        selections = backupSettingsState.extensionConflictSelections,
        onSelectionsChanged = backupSettingsState::updateExtensionConflictSelections,
        onDismiss = backupSettingsState::dismissConflictResolution,
    )
}

@Composable
internal fun SettingsDivider(
    top: Dp = 10.dp,
    bottom: Dp = 10.dp,
) {
    HorizontalDivider(
        modifier = Modifier.padding(top = top, bottom = bottom),
    )
}
