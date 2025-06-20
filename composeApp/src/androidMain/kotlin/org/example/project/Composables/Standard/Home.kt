package org.example.project.Composables.Standard

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.manglyextension.plugins.ExtensionMetadata
import org.example.project.Extension.ExtensionManager
import org.example.project.FileManager.FileManager
import org.example.project.Rooms.Entities.ExtensionEntity
import java.io.File

@Composable
fun Home() {

    val fileManager = FileManager()
    val context = LocalContext.current
    val em = ExtensionManager()

    LaunchedEffect(Unit) {
        val allEntries: List<ExtensionEntity> = fileManager.getAllEntries(context = context)
        for (entry in allEntries) {

            // Todo: this is example code for testing purposes, remove later
            val x: ExtensionMetadata = em.extractExtensionMetadata(File(entry.filePath))
            val y = em.loadPluginSource(x, context)
            Log.d("lol", y.getExtensionName())
        }
    }

    Column ( modifier = Modifier
        .fillMaxSize()
        .background(Color.White),

        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    )
    {
        Icon(
            imageVector = Icons.Default.Home,
            contentDescription = "Home page",
            tint = Color(0xFF0F9D58)
        )
    }

}