package org.example.project.Composables.Shared.Topbar

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.manglyextension.plugins.ExtensionMetadata
import kotlinx.coroutines.launch
import org.example.project.Extension.ExtensionManager
import org.example.project.FileManager.FileManager
import org.example.project.MainActivity
import org.example.project.Rooms.Entities.ExtensionEntity

@Composable
fun TopBarNewExtension() {
    val context = LocalContext.current
    val fileManager = FileManager()
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

                            // Todo: stop being lazy
                            val mainActivity = context as? MainActivity
                            val intent = mainActivity?.intent
                            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                            mainActivity?.finish()
                            mainActivity?.startActivity(intent)
                        }
                    }
                } else {
                    Toast.makeText(
                        context,
                        "Selected file is not a valid .mangly file",
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
                zipPickerLauncher.launch(arrayOf("*/*"))
            },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add"
            )
        }
    }
}

@Composable
fun TopBarDeleteExtensionFromExtensionDetails(metadata: ExtensionMetadata) {
    val extensionManager = remember { ExtensionManager() }
    val coroutineScope = rememberCoroutineScope()
    val fileManager = remember { FileManager() }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 25.dp)
            .height(48.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        IconButton(
            onClick = {
                coroutineScope.launch {
                    val entityToBeDeleted: ExtensionEntity =
                        extensionManager.getDatabaseEntryByMetadata(metadata, context)
                    fileManager.deleteAndRemoveEntry(entityToBeDeleted, context)

                    // Todo: stop being lazy
                    val mainActivity = context as? MainActivity
                    val intent = mainActivity?.intent
                    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    mainActivity?.finish()
                    mainActivity?.startActivity(intent)

                }
            },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete"
            )
        }
    }
}
