package org.example.project.Composables.Shared.Topbar

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.manglyextension.plugins.ExtensionMetadata
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch
import org.example.project.MainActivity
import org.example.project.di.FileManagersEntryPoint

@Composable
fun TopBarNewExtension() {
    val context = LocalContext.current

    val appContext = context.applicationContext
    val entryPoint =
        EntryPointAccessors.fromApplication(appContext, FileManagersEntryPoint::class.java)
    val fileManager = entryPoint.fileManager()

    val coroutineScope = rememberCoroutineScope()

    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let {
                val name =
                    context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (cursor.moveToFirst() && nameIndex >= 0) {
                            cursor.getString(nameIndex)
                        } else null
                    }

                if (name?.endsWith(".mangly") == true) {
                    val inputStream = context.contentResolver.openInputStream(it)
                    if (inputStream != null) {
                        coroutineScope.launch {
                            fileManager.saveAndInsertEntry(
                                context = context,
                                inputStream = inputStream,
                            )

                            // TODO: stop being lazy
                            val mainActivity = context as? MainActivity
                            val intent = mainActivity?.intent
                            intent?.addFlags(
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            )
                            mainActivity?.finish()
                            mainActivity?.startActivity(intent)
                        }
                    }
                } else {
                    Toast.makeText(
                        context,
                        "Selected file is not a valid Mangly extension (.mangly)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 25.dp)
            .height(48.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        IconButton(
            onClick = {
                // Let the user pick a Mangly extension file
                zipPickerLauncher.launch(arrayOf("*/*"))
            },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Import extension"
            )
        }
    }
}

@Composable
fun TopBarDeleteExtensionFromExtensionDetails(metadata: ExtensionMetadata) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val entryPoint =
        EntryPointAccessors.fromApplication(appContext, FileManagersEntryPoint::class.java)

    val openDialog = remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 25.dp)
            .height(48.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        IconButton(
            onClick = {
                openDialog.value = true
            },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete extension"
            )
        }
    }

    if (openDialog.value) {
        DeleteWarningDialog(
            entryPoint = entryPoint,
            metadata = metadata,
            context = context,
            onDismiss = { openDialog.value = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteWarningDialog(
    entryPoint: FileManagersEntryPoint,
    metadata: ExtensionMetadata,
    context: Context,
    onDismiss: () -> Unit
) {
    val fileManager = entryPoint.fileManager()
    val extensionManager = entryPoint.extensionManager()
    val coroutineScope = rememberCoroutineScope()

    BasicAlertDialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Delete extension?",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Are you sure you want to delete this extension?\nThis action cannot be undone."
                )
                Text(
                    text = "Deleting an extension WILL also remove any associated data, such as favorites",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 14.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.align(Alignment.End)
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.size(8.dp))

                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                val entityToBeDeleted =
                                    extensionManager.getDatabaseEntryByMetadata(metadata, context)
                                fileManager.deleteAndRemoveEntry(entityToBeDeleted, context)

                                // TODO: stop being lazy
                                val mainActivity = context as? MainActivity
                                val intent = mainActivity?.intent
                                intent?.addFlags(
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                )
                                mainActivity?.finish()
                                mainActivity?.startActivity(intent)
                            }
                        }
                    ) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}
