package com.eren76.mangly.composables.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    onPageStartIndexChanged: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    PaginatedHomeGrid(
        pageSize = pageSize,
        homeItems = downloads,
        onPageStartIndexChanged = onPageStartIndexChanged,
        modifier = modifier,
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
    downloadsViewModel: DownloadsViewModel,
    modifier: Modifier = Modifier
) {
    ShowHomeItemsInLazyGrid(
        homeItems = downloads,
        key = { item -> item.download.downloadId },
        modifier = modifier,
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
