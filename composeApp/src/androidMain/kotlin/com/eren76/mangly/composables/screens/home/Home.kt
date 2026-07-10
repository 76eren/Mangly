package com.eren76.mangly.composables.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.eren76.mangly.rooms.entities.FavoritesEntity
import com.eren76.mangly.rooms.entities.HistoryWithReadChapters
import com.eren76.mangly.rooms.relations.DownloadWithChapters
import com.eren76.mangly.viewmodels.DownloadsViewModel
import com.eren76.mangly.viewmodels.ExtensionMetadataViewModel
import com.eren76.mangly.viewmodels.FavoritesViewModel
import com.eren76.mangly.viewmodels.HistoryViewModel
import com.eren76.manglyextension.plugins.ExtensionMetadata

@Composable
fun Home(
    favoritesViewModel: FavoritesViewModel,
    downloadsViewModel: DownloadsViewModel,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    historyViewModel: HistoryViewModel,
    navHostController: NavHostController,
    showDownloads: Boolean = false,
) {
    val context = LocalContext.current
    val mode: HomeMode = remember(showDownloads) {
        HomeMode.fromShowDownloads(showDownloads)
    }
    val displayPreferences: HomeDisplayPreferences = rememberHomeDisplayPreferences(context)

    LaunchedEffect(showDownloads, context) {
        if (showDownloads) {
            downloadsViewModel.observeDownloadQueue(context)
        }
    }

    val favorites: List<FavoritesEntity> = favoritesViewModel.favorites.value
    val downloads: List<DownloadWithChapters> = downloadsViewModel.downloads.value
    val historyWithChapters: List<HistoryWithReadChapters> =
        historyViewModel.historyWithChapters.value

    val sortedFavorites = remember(favorites, displayPreferences.sorting, historyWithChapters) {
        sortFavorites(favorites, displayPreferences.sorting, historyViewModel)
    }
    val sortedDownloads: List<DownloadWithChapters> =
        remember(downloads, displayPreferences.sorting, historyWithChapters) {
            sortDownloads(downloads, displayPreferences.sorting, historyViewModel)
        }

    val itemCountsBySource: Map<ExtensionMetadata, Int> = when (mode) {
        HomeMode.Favorites -> favoriteSourceCounts(
            favorites = sortedFavorites,
            extensionMetadataViewModel = extensionMetadataViewModel
        )

        HomeMode.Downloads -> downloadSourceCounts(
            downloads = sortedDownloads,
            extensionMetadataViewModel = extensionMetadataViewModel
        )
    }
    val sourceFilterState: HomeSourceFilterState = rememberHomeSourceFilterState(
        itemCountsBySource = itemCountsBySource,
        resetKey = mode
    )

    val filteredFavorites = remember(sortedFavorites, sourceFilterState.activeSource) {
        filterFavoritesBySource(sortedFavorites, sourceFilterState.activeSource)
    }
    val filteredDownloads = remember(sortedDownloads, sourceFilterState.activeSource) {
        filterDownloadsBySource(sortedDownloads, sourceFilterState.activeSource)
    }

    HomeLayout(mode = mode) {
        when (mode) {
            HomeMode.Favorites -> HomeFavoritesSection(
                allFavorites = sortedFavorites,
                filteredFavorites = filteredFavorites,
                sourceFilterState = sourceFilterState,
                displayPreferences = displayPreferences,
                extensionMetadataViewModel = extensionMetadataViewModel,
                favoritesViewModel = favoritesViewModel,
                navHostController = navHostController
            )

            HomeMode.Downloads -> HomeDownloadsSection(
                allDownloads = sortedDownloads,
                filteredDownloads = filteredDownloads,
                sourceFilterState = sourceFilterState,
                displayPreferences = displayPreferences,
                downloadQueue = downloadsViewModel.downloadQueue.value,
                downloadsViewModel = downloadsViewModel,
                extensionMetadataViewModel = extensionMetadataViewModel,
                navHostController = navHostController,
                context = context
            )
        }
    }
}
