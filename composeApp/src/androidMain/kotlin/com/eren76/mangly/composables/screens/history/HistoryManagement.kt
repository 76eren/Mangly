package com.eren76.mangly.composables.screens.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.eren76.mangly.rooms.entities.HistoryEntity
import com.eren76.mangly.viewmodels.ExtensionMetadataViewModel
import com.eren76.mangly.viewmodels.HistoryViewModel
import com.eren76.manglyextension.plugins.ExtensionMetadata
import com.eren76.manglyextension.plugins.Source
import java.net.URLEncoder
import java.util.SortedMap
import java.util.UUID

@Composable
fun HistoryManagement(
    historyViewModel: HistoryViewModel,
    navHostController: NavHostController,
    extensionMetaDataViewModel: ExtensionMetadataViewModel
) {
    val historyWithChapters by historyViewModel.historyWithChapters

    if (historyWithChapters.all { it.readChapters.isEmpty() }) {
        HistoryEmptyState()
        return
    }

    val historyData: SortedMap<Long, HistoryEntity> = remember(historyWithChapters) {
        getHistoryDataNewestToOldest(historyViewModel)
    }

    val sourcesById: Map<UUID, ExtensionMetadata> =
        remember(extensionMetaDataViewModel.getAllSources()) {
            extensionMetaDataViewModel.getAllSources().associateBy { metadata ->
                UUID.fromString(metadata.source.getExtensionId())
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "History",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        HorizontalDivider()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(
                items = historyData.entries.toList(),
                key = { entry ->
                    val timeKey = entry.key
                    val historyEntity = entry.value
                    historyEntity.id.toString() + timeKey.toString()
                }
            ) { entry ->
                val timeKey = entry.key
                val historyEntity = entry.value

                val sourceMetadata = sourcesById[historyEntity.extensionId]
                val source: Source? = sourceMetadata?.source

                HistoryRow(
                    historyEntity = historyEntity,
                    lastReadAt = timeKey,
                    source = source,
                    historyViewModel = historyViewModel,
                    onClick = {
                        val encodedUrl = URLEncoder.encode(
                            historyEntity.mangaUrl,
                            Charsets.UTF_8.name()
                        )
                        extensionMetaDataViewModel.setSelectedSource(sourcesById[historyEntity.extensionId]!!) // TODO: not ideal
                        navHostController.navigate("chapters/$encodedUrl")
                    }
                )
            }
        }
    }
}

@Composable
private fun HistoryEmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No history yet",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Start reading a manga to see it here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
