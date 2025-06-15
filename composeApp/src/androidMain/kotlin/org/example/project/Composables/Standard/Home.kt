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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import org.example.project.FileManager.FileManager
import org.example.project.Rooms.Entities.ExtensionEntity

@Composable
fun Home() {

    // Todo: this is for testing purposes, remove later
    val fileManager = FileManager()
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val allEntries: List<ExtensionEntity> = fileManager.getAllEntries(context = context)
        for (entry in allEntries) {
            Log.d("lol", "Entry: ${entry.name}, Path: ${entry.filePath}")
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