package com.eren76.mangly.composables.screens.home

import android.content.Context
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.eren76.mangly.composables.shared.collection.GridViewMode
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
    viewMode: GridViewMode,
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
        viewMode = viewMode,
        isLoadingItems = isLoadingItems,
        isLoadingSources = isLoadingSources,
        itemKey = { favorite -> favorite.id },
        itemContent = { favorite ->
            FavoriteCard(
                favorite = favorite,
                extensionMetadataViewModel = extensionMetadataViewModel,
                favoritesViewModel = favoritesViewModel,
                onClick = {
                    onHomeMangaClick(
                        mangaUrl = favorite.mangaUrl,
                        extensionId = favorite.extensionId,
                        extensionMetadataViewModel = extensionMetadataViewModel,
                        navController = navHostController
                    )
                }
            )
        }
    )
}

@Composable
fun ColumnScope.HomeDownloadsSection(
    allDownloads: List<DownloadWithChapters>,
    filteredDownloads: List<DownloadWithChapters>,
    sourceFilterState: HomeSourceFilterState,
    viewMode: GridViewMode,
    isLoadingItems: Boolean,
    isLoadingSources: Boolean,
    downloadQueue: List<DownloadQueueItem>,
    downloadsViewModel: DownloadsViewModel,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    navHostController: NavHostController,
    context: Context
) {
    HomeLibraryContent(
        mode = HomeMode.Downloads,
        allItems = allDownloads,
        filteredItems = filteredDownloads,
        sourceFilterState = sourceFilterState,
        viewMode = viewMode,
        isLoadingItems = isLoadingItems,
        isLoadingSources = isLoadingSources,
        itemKey = { download -> download.download.downloadId },
        supportingContent = {
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
        },
        itemContent = { download: DownloadWithChapters ->
            DownloadCard(
                downloadWithChapters = download,
                downloadsViewModel = downloadsViewModel,
                onClick = {
                    onHomeMangaClick(
                        mangaUrl = download.download.mangaUrl,
                        extensionId = download.download.extensionId,
                        extensionMetadataViewModel = extensionMetadataViewModel,
                        navController = navHostController,
                        isDownload = true
                    )
                }
            )
        }
    )
}
