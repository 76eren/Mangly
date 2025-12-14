package org.example.project.Composables.Standard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.navigation.NavHostController
import com.example.manglyextension.plugins.ExtensionMetadata
import com.example.manglyextension.plugins.Source
import org.example.project.Composables.Shared.Cards.CardTypeOne
import org.example.project.Composables.Shared.Topbar.TopBarNewExtension
import org.example.project.ViewModels.ExtensionDetailsViewModel
import org.example.project.ViewModels.ExtensionMetadataViewModel

@Composable
fun Extensions(
    navController: NavHostController,
    extensionDetailsViewModel: ExtensionDetailsViewModel,
    extensionMetadataViewModel: ExtensionMetadataViewModel
) {
    val cardItems = remember { mutableStateListOf<CardData>() }

    for (metadata in extensionMetadataViewModel.getAllSources()) {
        val extensionName = metadata.source.getExtensionName()
        val extensionImageBitmap: BitmapPainter = metadata.icon

        cardItems.add(
            CardData(
                imagePainter = extensionImageBitmap,
                title = extensionName,
                description = metadata.version,
                metadata = metadata,
                source = metadata.source
            )
        )
    }

    extensionDetailsViewModel.setCards(cardItems)


    Scaffold(
        topBar = { TopBarNewExtension() }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),

            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        )
        {
            LazyColumn {
                items(cardItems) { item ->
                    CardTypeOne(
                        cardData = item,
                        onClick = { navController.navigate("extensionDetails/${item.metadata.source.getExtensionId()}") }
                    )
                }
            }

        }
    }
}


data class CardData(
    val imagePainter: Painter,
    val title: String,
    val description: String,
    val metadata: ExtensionMetadata,
    val source: Source
)
