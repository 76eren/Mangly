package org.example.project.Composables.Standard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import org.example.project.Composables.Shared.Cards.CardTypeOne
import org.example.project.Composables.Shared.Topbar.TopBarNewExtension
import org.example.project.Extension.ExtensionManager
import org.example.project.FileManager.FileManager
import org.example.project.Rooms.Entities.ExtensionEntity
import java.io.File

@Composable
fun Extensions() {
    val context = LocalContext.current
    val fileManager = remember { FileManager() }
    val extensionManager = remember { ExtensionManager() }
    val cardItems = remember { mutableStateListOf<CardData>() }

    LaunchedEffect(Unit) {
        val allEntries: List<ExtensionEntity> = fileManager.getAllEntries(context)
        for (entry in allEntries) {
            val metadata = extensionManager.extractExtensionMetadata(File(entry.filePath))
            val source = extensionManager.loadPluginSource(metadata, context)

            val extensionName = source.getExtensionName()
            val extensionImageBitmap: BitmapPainter = metadata.icon


            cardItems.add(
                CardData(
                    imagePainter = extensionImageBitmap,
                    title = extensionName,
                    description = metadata.version
                )
            )
        }
    }

    Scaffold (
        topBar = { TopBarNewExtension() }
    ){ paddingValues ->

        Column ( modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .background(Color.White),

            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        )
        {
            LazyColumn {
                items(cardItems) { item ->
                    CardTypeOne(
                        imagePainter = item.imagePainter,
                        title = item.title,
                        description = item.description
                    )
                }
            }

        }
    }

}

data class CardData(
    val imagePainter: Painter,
    val title: String,
    val description: String
)
