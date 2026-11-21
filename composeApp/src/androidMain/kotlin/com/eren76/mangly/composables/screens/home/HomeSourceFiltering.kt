package com.eren76.mangly.composables.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.eren76.mangly.rooms.entities.FavoritesEntity
import com.eren76.mangly.rooms.relations.DownloadWithChapters
import com.eren76.manglyextension.plugins.ExtensionMetadata

@Immutable
data class HomeSourceOption(
    val id: String,
    val displayName: String,
    val itemCount: Int
)

@Immutable
data class HomeSourceFilterState(
    val allLibrarySourceOptions: List<HomeSourceOption>,
    val activeSourceId: String?,
    val onSelectedSourceChange: (String?) -> Unit
)

@Composable
fun rememberHomeSourceFilterState(
    allLibrarySourceOptions: List<HomeSourceOption>,
    resetKey: Any
): HomeSourceFilterState {
    val selectedSourceId = remember(resetKey) {
        mutableStateOf<String?>(null)
    }
    val activeSourceId: String? = selectedSourceId.value.takeIf { sourceId ->
        sourceId == null || allLibrarySourceOptions.any { option -> option.id == sourceId }
    }

    LaunchedEffect(selectedSourceId.value, activeSourceId) {
        if (selectedSourceId.value != activeSourceId) {
            selectedSourceId.value = activeSourceId
        }
    }

    val onSelectedSourceChange: (String?) -> Unit = remember(selectedSourceId) {
        { sourceId -> selectedSourceId.value = sourceId }
    }

    return remember(allLibrarySourceOptions, activeSourceId, onSelectedSourceChange) {
        HomeSourceFilterState(
            allLibrarySourceOptions = allLibrarySourceOptions,
            activeSourceId = activeSourceId,
            onSelectedSourceChange = onSelectedSourceChange
        )
    }
}

fun favoriteSourceOptions(
    favorites: List<FavoritesEntity>,
    allSourceMetadata: List<ExtensionMetadata>
): List<HomeSourceOption> {
    return homeSourceOptions(
        itemCountsBySourceId = favorites
            .groupingBy { favorite -> favorite.extensionId.toString() }
            .eachCount(),
        allSourceMetadata = allSourceMetadata
    )
}

fun downloadSourceOptions(
    downloads: List<DownloadWithChapters>,
    allSourceMetadata: List<ExtensionMetadata>
): List<HomeSourceOption> {
    return homeSourceOptions(
        itemCountsBySourceId = downloads
            .groupingBy { download -> download.download.extensionId.toString() }
            .eachCount(),
        allSourceMetadata = allSourceMetadata
    )
}

fun filterFavoritesBySource(
    favorites: List<FavoritesEntity>,
    selectedSourceId: String?
): List<FavoritesEntity> {
    if (selectedSourceId == null) return favorites

    return favorites.filter { favorite ->
        favorite.extensionId.toString() == selectedSourceId
    }
}

fun filterDownloadsBySource(
    downloads: List<DownloadWithChapters>,
    selectedSourceId: String?
): List<DownloadWithChapters> {
    if (selectedSourceId == null) return downloads

    return downloads.filter { download ->
        download.download.extensionId.toString() == selectedSourceId
    }
}

private fun homeSourceOptions(
    itemCountsBySourceId: Map<String, Int>,
    allSourceMetadata: List<ExtensionMetadata>
): List<HomeSourceOption> {
    val seenSourceIds = mutableSetOf<String>()

    return allSourceMetadata.mapNotNull { metadata ->
        val sourceId = metadata.extensionId
        val itemCount = itemCountsBySourceId[sourceId] ?: return@mapNotNull null
        if (!seenSourceIds.add(sourceId)) return@mapNotNull null

        HomeSourceOption(
            id = sourceId,
            displayName = metadata.displayName(),
            itemCount = itemCount
        )
    }.sortedWith(
        compareBy<HomeSourceOption>(
            { option -> option.displayName.lowercase() },
            { option -> option.id }
        )
    )
}

private fun ExtensionMetadata.displayName(): String {
    return name.ifBlank { "Unknown source" }
}
