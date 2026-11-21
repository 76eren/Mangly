package com.eren76.mangly.composables.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.eren76.mangly.composables.shared.cards.CardTypeOne
import com.eren76.mangly.composables.shared.topbar.TopBarNewExtension
import com.eren76.mangly.viewmodels.ExtensionDetailsViewModel
import com.eren76.mangly.viewmodels.ExtensionMetadataViewModel
import com.eren76.manglyextension.plugins.ExtensionMetadata

@Composable
fun Extensions(
    navController: NavHostController,
    extensionDetailsViewModel: ExtensionDetailsViewModel,
    extensionMetadataViewModel: ExtensionMetadataViewModel
) {
    val allSourceCards =
        extensionMetadataViewModel.getAllValidAndInvalidSourcesMetadata().map { metadata ->
            CardData(
                imagePainter = metadata.icon,
                title = metadata.name,
                description = metadata.version,
                metadata = metadata
            )
        }
    val (workingSourceCards, disabledSourceCards) = allSourceCards.partition { it.metadata.canSourceBeLoaded }

    LaunchedEffect(allSourceCards) { extensionDetailsViewModel.setAllSourceCards(allSourceCards) }


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
                items(workingSourceCards, key = { it.metadata.extensionId }) { item ->
                    CardTypeOne(
                        cardData = item,
                        onClick = { navController.navigate("extensionDetails/${item.metadata.extensionId}") }
                    )
                }
                if (disabledSourceCards.isNotEmpty()) {
                    item {
                        Text(
                            text = "Disabled extensions",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    items(disabledSourceCards, key = { it.metadata.extensionId }) { item ->
                        CardTypeOne(
                            cardData = item,
                            onClick = { navController.navigate("extensionDetails/${item.metadata.extensionId}") }
                        )
                    }
                }
            }

        }
    }
}


data class CardData(
    val imagePainter: Painter,
    val title: String,
    val description: String,
    val metadata: ExtensionMetadata
)
