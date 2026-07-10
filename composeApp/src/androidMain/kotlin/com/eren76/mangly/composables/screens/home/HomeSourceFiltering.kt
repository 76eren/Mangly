package com.eren76.mangly.composables.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.eren76.mangly.rooms.entities.FavoritesEntity
import com.eren76.mangly.rooms.relations.DownloadWithChapters
import com.eren76.mangly.viewmodels.ExtensionMetadataViewModel
import com.eren76.manglyextension.plugins.ExtensionMetadata

data class HomeSourceFilterState(
    val itemCountsBySource: Map<ExtensionMetadata, Int>,
    val activeSource: ExtensionMetadata?,
    val onSelectedSourceChange: (ExtensionMetadata?) -> Unit
)

@Composable
fun rememberHomeSourceFilterState(
    itemCountsBySource: Map<ExtensionMetadata, Int>,
    resetKey: Any
): HomeSourceFilterState {
    var selectedSource: ExtensionMetadata? by remember(resetKey) {
        mutableStateOf<ExtensionMetadata?>(null)
    }

    val activeSource: ExtensionMetadata? = selectedSource.takeIf { source ->
        source == null || itemCountsBySource.containsKey(source)
    }

    LaunchedEffect(selectedSource, itemCountsBySource) {
        if (selectedSource != activeSource) {
            selectedSource = activeSource
        }
    }

    return HomeSourceFilterState(
        itemCountsBySource = itemCountsBySource,
        activeSource = activeSource,
        onSelectedSourceChange = { source -> selectedSource = source }
    )
}

fun favoriteSourceCounts(
    favorites: List<FavoritesEntity>,
    extensionMetadataViewModel: ExtensionMetadataViewModel
): Map<ExtensionMetadata, Int> {
    val sourcesById = extensionMetadataViewModel.sourcesById()

    return favorites
        .mapNotNull { favorite -> sourcesById[favorite.extensionId.toString()] }
        .sourceCounts()
}

fun downloadSourceCounts(
    downloads: List<DownloadWithChapters>,
    extensionMetadataViewModel: ExtensionMetadataViewModel
): Map<ExtensionMetadata, Int> {
    val sourcesById = extensionMetadataViewModel.sourcesById()

    return downloads
        .mapNotNull { downloadWithChapters ->
            sourcesById[downloadWithChapters.download.extensionId.toString()]
        }
        .sourceCounts()
}

fun filterFavoritesBySource(
    favorites: List<FavoritesEntity>,
    selectedSource: ExtensionMetadata?
): List<FavoritesEntity> {
    val selectedSourceId = selectedSource?.source?.getExtensionId() ?: return favorites

    return favorites.filter { favorite ->
        favorite.extensionId.toString() == selectedSourceId
    }
}

fun filterDownloadsBySource(
    downloads: List<DownloadWithChapters>,
    selectedSource: ExtensionMetadata?
): List<DownloadWithChapters> {
    val selectedSourceId = selectedSource?.source?.getExtensionId() ?: return downloads

    return downloads.filter { downloadWithChapters ->
        downloadWithChapters.download.extensionId.toString() == selectedSourceId
    }
}

private fun ExtensionMetadataViewModel.sourcesById(): Map<String, ExtensionMetadata> {
    return getAllSources().associateBy { source -> source.source.getExtensionId() }
}

private fun List<ExtensionMetadata>.sourceCounts(): Map<ExtensionMetadata, Int> {
    return groupingBy { source -> source }
        .eachCount()
}
