package com.eren76.mangly.composables.screens.home

import android.content.Context
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.eren76.mangly.composables.shared.collection.GridViewMode
import com.eren76.mangly.preferences.rememberGridViewMode
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
    val sorting: HomeSorting = rememberHomeSorting(context)
    val viewMode: GridViewMode = rememberGridViewMode(context)

    LaunchedEffect(showDownloads, context) {
        if (showDownloads) {
            downloadsViewModel.observeDownloadQueue(context)
        }
    }

    HomeLayout {
        when (mode) {
            HomeMode.Favorites -> HomeFavoritesContent(
                sorting = sorting,
                viewMode = viewMode,
                favoritesViewModel = favoritesViewModel,
                extensionMetadataViewModel = extensionMetadataViewModel,
                historyViewModel = historyViewModel,
                navHostController = navHostController
            )

            HomeMode.Downloads -> HomeDownloadsContent(
                sorting = sorting,
                viewMode = viewMode,
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
    sorting: HomeSorting,
    viewMode: GridViewMode,
    favoritesViewModel: FavoritesViewModel,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    historyViewModel: HistoryViewModel,
    navHostController: NavHostController
) {
    val favorites: List<FavoritesEntity> = favoritesViewModel.favorites.value
    val historyWithChapters: List<HistoryWithReadChapters> =
        historyForSorting(sorting, historyViewModel)
    val sortedFavorites: List<FavoritesEntity> =
        remember(favorites, sorting, historyWithChapters) {
            sortFavorites(favorites, sorting, historyViewModel)
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

    val isLoadingFavorites: Boolean = favoritesViewModel.isLoading.value
    val isLoadingSources: Boolean = extensionMetadataViewModel.isLoading.value
    HomeFavoritesSection(
        allFavorites = sortedFavorites,
        filteredFavorites = filteredFavorites,
        sourceFilterState = sourceFilterState,
        viewMode = viewMode,
        isLoadingItems = isLoadingFavorites,
        isLoadingSources = isLoadingSources,
        extensionMetadataViewModel = extensionMetadataViewModel,
        favoritesViewModel = favoritesViewModel,
        navHostController = navHostController
    )
}

@Composable
private fun ColumnScope.HomeDownloadsContent(
    sorting: HomeSorting,
    viewMode: GridViewMode,
    downloadsViewModel: DownloadsViewModel,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    historyViewModel: HistoryViewModel,
    navHostController: NavHostController,
    context: Context
) {
    val downloads: List<DownloadWithChapters> = downloadsViewModel.downloads.value
    val historyWithChapters: List<HistoryWithReadChapters> =
        historyForSorting(sorting, historyViewModel)
    val sortedDownloads: List<DownloadWithChapters> =
        remember(downloads, sorting, historyWithChapters) {
            sortDownloads(downloads, sorting, historyViewModel)
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
    val isLoadingDownloads = downloadsViewModel.isLoading.value
    val isLoadingSources = extensionMetadataViewModel.isLoading.value
    HomeDownloadsSection(
        allDownloads = sortedDownloads,
        filteredDownloads = filteredDownloads,
        sourceFilterState = sourceFilterState,
        viewMode = viewMode,
        isLoadingItems = isLoadingDownloads,
        isLoadingSources = isLoadingSources,
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
