package com.eren76.mangly.composables.screens.home

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.eren76.mangly.rooms.relations.DownloadWithChapters
import com.eren76.mangly.viewmodels.DownloadsViewModel
import com.eren76.mangly.viewmodels.ExtensionMetadataViewModel

@Composable
fun PaginatedDownloads(
    pageSize: Int,
    downloads: List<DownloadWithChapters>,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    downloadsViewModel: DownloadsViewModel,
    navHostController: NavHostController,
    onPageStartIndexChanged: ((Int) -> Unit)? = null
) {
    PaginatedHomeGrid(
        pageSize = pageSize,
        homeItems = downloads,
        onPageStartIndexChanged = onPageStartIndexChanged,
        itemContent = { item ->
            DownloadCard(
                downloadWithChapters = item,
                downloadsViewModel = downloadsViewModel,
                onClick = {
                    onHomeMangaClick(
                        mangaUrl = item.download.mangaUrl,
                        extensionId = item.download.extensionId,
                        extensionMetadataViewModel = extensionMetadataViewModel,
                        navController = navHostController,
                        isDownload = true
                    )
                }
            )
        }
    )
}

@Composable
fun ShowDownloadsInLazyGrid(
    downloads: List<DownloadWithChapters>,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    navHostController: NavHostController,
    downloadsViewModel: DownloadsViewModel
) {
    ShowHomeItemsInLazyGrid(
        homeItems = downloads,
        key = { item -> item.download.downloadId },
        itemContent = { item: DownloadWithChapters ->
            DownloadCard(
                downloadWithChapters = item,
                downloadsViewModel = downloadsViewModel,
                onClick = {
                    onHomeMangaClick(
                        mangaUrl = item.download.mangaUrl,
                        extensionId = item.download.extensionId,
                        extensionMetadataViewModel = extensionMetadataViewModel,
                        navController = navHostController,
                        isDownload = true
                    )
                }
            )
        }
    )
}
