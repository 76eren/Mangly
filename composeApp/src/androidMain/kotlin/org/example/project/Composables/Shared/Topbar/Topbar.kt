package org.example.project.Composables.Shared.Topbar

import android.net.Uri
import androidx.activity.compose.BackHandler
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
import org.example.project.Rooms.Entities.ExtensionEntity
import java.util.UUID

@Composable
fun TopBarNewExtension() {
    val context = LocalContext.current
    val fileManager = FileManager()
    val coroutineScope = rememberCoroutineScope()

    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let {
                val inputStream = context.contentResolver.openInputStream(it)
                if (inputStream != null) {
                    val id = UUID.randomUUID()
                    coroutineScope.launch {
                        fileManager.saveAndInsertEntry(
                            context = context,
                            fileName = "$id.zip",
                            inputStream = inputStream,
                            id = id
                        )
                    }
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
                zipPickerLauncher.launch(arrayOf("application/zip"))
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
                    val entityToBeDeleted: ExtensionEntity = extensionManager.getDatabaseEntryByMetadata(metadata, context)
                    fileManager.deleteAndRemoveEntry(entityToBeDeleted, context)

                    // Todo: go back to the previous screen

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
