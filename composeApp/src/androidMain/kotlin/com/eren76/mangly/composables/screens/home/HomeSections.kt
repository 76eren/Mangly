package com.eren76.mangly.composables.screens.home

import android.content.Context
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.eren76.mangly.downloads.DownloadQueuePanel
import com.eren76.mangly.downloads.models.DownloadQueueItem
import com.eren76.mangly.rooms.entities.FavoritesEntity
import com.eren76.mangly.rooms.relations.DownloadWithChapters
import com.eren76.mangly.viewmodels.DownloadsViewModel
import com.eren76.mangly.viewmodels.ExtensionMetadataViewModel
import com.eren76.mangly.viewmodels.FavoritesViewModel

@Composable
fun ColumnScope.HomeFavoritesSection(
    allFavorites: List<FavoritesEntity>,
    filteredFavorites: List<FavoritesEntity>,
    sourceFilterState: HomeSourceFilterState,
    displayPreferences: HomeDisplayPreferences,
    isLoadingItems: Boolean,
    isLoadingSources: Boolean,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    favoritesViewModel: FavoritesViewModel,
    navHostController: NavHostController
) {
    HomeLibraryContent(
        mode = HomeMode.Favorites,
        allItems = allFavorites,
        filteredItems = filteredFavorites,
        sourceFilterState = sourceFilterState,
        isLoadingItems = isLoadingItems,
        isLoadingSources = isLoadingSources
    ) { listModifier ->
        if (displayPreferences.isPaginationEnabled) {
            PaginatedFavorites(
                pageSize = displayPreferences.pageSize,
                sortedFavorites = filteredFavorites,
                extensionMetadataViewModel = extensionMetadataViewModel,
                favoritesViewModel = favoritesViewModel,
                navHostController = navHostController,
                modifier = listModifier
            )
        } else {
            ShowItemsInLazyGrid(
                sortedFavorites = filteredFavorites,
                extensionMetadataViewModel = extensionMetadataViewModel,
                favoritesViewModel = favoritesViewModel,
                navHostController = navHostController,
                modifier = listModifier
            )
        }
    }
}

@Composable
fun ColumnScope.HomeDownloadsSection(
    allDownloads: List<DownloadWithChapters>,
    filteredDownloads: List<DownloadWithChapters>,
    sourceFilterState: HomeSourceFilterState,
    displayPreferences: HomeDisplayPreferences,
    isLoadingItems: Boolean,
    isLoadingSources: Boolean,
    downloadQueue: List<DownloadQueueItem>,
    downloadsViewModel: DownloadsViewModel,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    navHostController: NavHostController,
    context: Context
) {
    DownloadQueuePanel(
        queueItems = downloadQueue,
        onCancelQueue = { downloadsViewModel.cancelDownloadQueue(context) },
        onDismissSingleFinishedQueueItem = { item ->
            downloadsViewModel.dismissSingleFinishedDownloadQueueItem(context, item)
        },
        onDismissAllFinishedQueueItems = { items ->
            downloadsViewModel.dismissAllFinishedDownloadQueueItems(context, items)
        }
    )

    HomeLibraryContent(
        mode = HomeMode.Downloads,
        allItems = allDownloads,
        filteredItems = filteredDownloads,
        sourceFilterState = sourceFilterState,
        isLoadingItems = isLoadingItems,
        isLoadingSources = isLoadingSources
    ) { listModifier ->
        if (displayPreferences.isPaginationEnabled) {
            PaginatedDownloads(
                pageSize = displayPreferences.pageSize,
                downloads = filteredDownloads,
                extensionMetadataViewModel = extensionMetadataViewModel,
                downloadsViewModel = downloadsViewModel,
                navHostController = navHostController,
                modifier = listModifier
            )
        } else {
            ShowDownloadsInLazyGrid(
                downloads = filteredDownloads,
                extensionMetadataViewModel = extensionMetadataViewModel,
                navHostController = navHostController,
                downloadsViewModel = downloadsViewModel,
                modifier = listModifier
            )
        }
    }
}
