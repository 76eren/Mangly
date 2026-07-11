package com.eren76.mangly.composables.screens.home

import android.content.Context
import androidx.compose.foundation.layout.ColumnScope
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

    HomeLayout(mode = mode) {
        when (mode) {
            HomeMode.Favorites -> HomeFavoritesContent(
                displayPreferences = displayPreferences,
                favoritesViewModel = favoritesViewModel,
                extensionMetadataViewModel = extensionMetadataViewModel,
                historyViewModel = historyViewModel,
                navHostController = navHostController
            )

            HomeMode.Downloads -> HomeDownloadsContent(
                displayPreferences = displayPreferences,
                downloadsViewModel = downloadsViewModel,
                extensionMetadataViewModel = extensionMetadataViewModel,
                historyViewModel = historyViewModel,
                navHostController = navHostController,
                context = context
            )
        }
    }
}

@Composable
private fun ColumnScope.HomeFavoritesContent(
    displayPreferences: HomeDisplayPreferences,
    favoritesViewModel: FavoritesViewModel,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    historyViewModel: HistoryViewModel,
    navHostController: NavHostController
) {
    val favorites: List<FavoritesEntity> = favoritesViewModel.favorites.value
    val historyWithChapters: List<HistoryWithReadChapters> =
        historyForSorting(displayPreferences.sorting, historyViewModel)
    val sortedFavorites: List<FavoritesEntity> =
        remember(favorites, displayPreferences.sorting, historyWithChapters) {
            sortFavorites(favorites, displayPreferences.sorting, historyViewModel)
        }

    val sources: List<ExtensionMetadata> = extensionMetadataViewModel.getAllSources()
    val sourceOptions: List<HomeSourceOption> = remember(sortedFavorites, sources) {
        favoriteSourceOptions(favorites = sortedFavorites, sources = sources)
    }
    val sourceFilterState: HomeSourceFilterState = rememberHomeSourceFilterState(
        sourceOptions = sourceOptions,
        resetKey = HomeMode.Favorites
    )
    val filteredFavorites: List<FavoritesEntity> =
        remember(sortedFavorites, sourceFilterState.activeSourceId) {
            filterFavoritesBySource(sortedFavorites, sourceFilterState.activeSourceId)
        }

    HomeFavoritesSection(
        allFavorites = sortedFavorites,
        filteredFavorites = filteredFavorites,
        sourceFilterState = sourceFilterState,
        displayPreferences = displayPreferences,
        isLoadingItems = favoritesViewModel.isLoading.value,
        isLoadingSources = extensionMetadataViewModel.isLoading.value,
        extensionMetadataViewModel = extensionMetadataViewModel,
        favoritesViewModel = favoritesViewModel,
        navHostController = navHostController
    )
}

@Composable
private fun ColumnScope.HomeDownloadsContent(
    displayPreferences: HomeDisplayPreferences,
    downloadsViewModel: DownloadsViewModel,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    historyViewModel: HistoryViewModel,
    navHostController: NavHostController,
    context: Context
) {
    val downloads: List<DownloadWithChapters> = downloadsViewModel.downloads.value
    val historyWithChapters: List<HistoryWithReadChapters> =
        historyForSorting(displayPreferences.sorting, historyViewModel)
    val sortedDownloads: List<DownloadWithChapters> =
        remember(downloads, displayPreferences.sorting, historyWithChapters) {
            sortDownloads(downloads, displayPreferences.sorting, historyViewModel)
        }

    val sources: List<ExtensionMetadata> = extensionMetadataViewModel.getAllSources()
    val sourceOptions: List<HomeSourceOption> = remember(sortedDownloads, sources) {
        downloadSourceOptions(downloads = sortedDownloads, sources = sources)
    }
    val sourceFilterState: HomeSourceFilterState = rememberHomeSourceFilterState(
        sourceOptions = sourceOptions,
        resetKey = HomeMode.Downloads
    )
    val filteredDownloads: List<DownloadWithChapters> =
        remember(sortedDownloads, sourceFilterState.activeSourceId) {
            filterDownloadsBySource(sortedDownloads, sourceFilterState.activeSourceId)
        }

    HomeDownloadsSection(
        allDownloads = sortedDownloads,
        filteredDownloads = filteredDownloads,
        sourceFilterState = sourceFilterState,
        displayPreferences = displayPreferences,
        isLoadingItems = downloadsViewModel.isLoading.value,
        isLoadingSources = extensionMetadataViewModel.isLoading.value,
        downloadQueue = downloadsViewModel.downloadQueue.value,
        downloadsViewModel = downloadsViewModel,
        extensionMetadataViewModel = extensionMetadataViewModel,
        navHostController = navHostController,
        context = context
    )
}

@Composable
private fun historyForSorting(
    sorting: HomeSorting,
    historyViewModel: HistoryViewModel
): List<HistoryWithReadChapters> {
    return if (sorting == HomeSorting.LatestRead) {
        historyViewModel.historyWithChapters.value
    } else {
        emptyList()
    }
}
