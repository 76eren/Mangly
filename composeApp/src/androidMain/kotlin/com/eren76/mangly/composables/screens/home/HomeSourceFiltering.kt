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
    val sourceOptions: List<HomeSourceOption>,
    val activeSourceId: String?,
    val onSelectedSourceChange: (String?) -> Unit
)

@Composable
fun rememberHomeSourceFilterState(
    sourceOptions: List<HomeSourceOption>,
    resetKey: Any
): HomeSourceFilterState {
    val selectedSourceId = remember(resetKey) {
        mutableStateOf<String?>(null)
    }
    val activeSourceId: String? = selectedSourceId.value.takeIf { sourceId ->
        sourceId == null || sourceOptions.any { option -> option.id == sourceId }
    }

    LaunchedEffect(selectedSourceId.value, activeSourceId) {
        if (selectedSourceId.value != activeSourceId) {
            selectedSourceId.value = activeSourceId
        }
    }

    val onSelectedSourceChange: (String?) -> Unit = remember(selectedSourceId) {
        { sourceId -> selectedSourceId.value = sourceId }
    }

    return remember(sourceOptions, activeSourceId, onSelectedSourceChange) {
        HomeSourceFilterState(
            sourceOptions = sourceOptions,
            activeSourceId = activeSourceId,
            onSelectedSourceChange = onSelectedSourceChange
        )
    }
}

fun favoriteSourceOptions(
    favorites: List<FavoritesEntity>,
    sources: List<ExtensionMetadata>
): List<HomeSourceOption> {
    return homeSourceOptions(
        itemCountsBySourceId = favorites
            .groupingBy { favorite -> favorite.extensionId.toString() }
            .eachCount(),
        sources = sources
    )
}

fun downloadSourceOptions(
    downloads: List<DownloadWithChapters>,
    sources: List<ExtensionMetadata>
): List<HomeSourceOption> {
    return homeSourceOptions(
        itemCountsBySourceId = downloads
            .groupingBy { download -> download.download.extensionId.toString() }
            .eachCount(),
        sources = sources
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
    sources: List<ExtensionMetadata>
): List<HomeSourceOption> {
    val seenSourceIds = mutableSetOf<String>()

    return sources.mapNotNull { metadata ->
        val sourceId = runCatching { metadata.source.getExtensionId() }
            .getOrNull()
            ?.takeIf { id -> id.isNotBlank() }
            ?: return@mapNotNull null
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
    val metadataName = name.takeIf { value -> value.isNotBlank() }
    if (metadataName != null) return metadataName

    return runCatching { source.getExtensionName() }
        .getOrNull()
        ?.takeIf { value -> value.isNotBlank() }
        ?: "Unknown source"
}
