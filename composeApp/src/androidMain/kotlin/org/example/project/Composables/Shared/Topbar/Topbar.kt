package org.example.project.Composables.Shared.Topbar

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.example.project.FileManager.FileManager
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
