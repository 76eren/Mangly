package com.eren76.mangly.composables.screens.chapterslist

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.eren76.mangly.rooms.entities.FavoritesEntity
import com.eren76.mangly.viewmodels.ChaptersListViewModel
import com.eren76.mangly.viewmodels.DownloadsViewModel
import com.eren76.mangly.viewmodels.ExtensionMetadataViewModel
import com.eren76.mangly.viewmodels.FavoritesViewModel
import com.eren76.mangly.viewmodels.HistoryViewModel
import com.eren76.manglyextension.plugins.ExtensionMetadata
import com.eren76.manglyextension.plugins.Source
import com.eren76.manglyextension.plugins.Source.ImageForChaptersList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@Composable
fun ChaptersList(
    targetUrl: String,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    chaptersListViewModel: ChaptersListViewModel,
    favoritesViewModel: FavoritesViewModel,
    historyViewModel: HistoryViewModel,
    downloadsViewModel: DownloadsViewModel,
    navHostController: NavHostController,
    showDownloads: Boolean = false
) {
    // TODO: Do this some other way, because now this needs to be set before navigating to this screen
    val metadata: ExtensionMetadata? = extensionMetadataViewModel.selectedSingleSource.value

    val downloads = downloadsViewModel.downloads.value
    val relatedDownload = downloads.find { it.download.mangaUrl == targetUrl }


    if (metadata == null) {
        Text(
            "Something went wrong while loading the chapters list.",
            modifier = Modifier.padding(16.dp)
        )
        return
    }

    var chapters by remember { mutableStateOf<List<Source.ChapterValue>?>(null) }
    var image by remember { mutableStateOf<ImageForChaptersList?>(null) }
    var summary by remember { mutableStateOf("") }
    var isSummaryExpanded by remember { mutableStateOf(false) }
    var mangaName by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    // Local selection state for this screen only
    val selectedChapters = remember { mutableStateListOf<String>() }
    val isSelectionMode = selectedChapters.isNotEmpty()

    BackHandler(enabled = isSelectionMode) {
        selectedChapters.clear()
    }

    DisposableEffect(Unit) {
        onDispose {
            val position = scrollState.value
            chaptersListViewModel.setScrollPosition(position)
        }
    }

    LaunchedEffect(targetUrl, metadata) {
        val fetchedChapters = runCatching {
            withContext(Dispatchers.IO) {
                if (chaptersListViewModel.getChapters().isEmpty()) {
                    fetchChapterList(metadata.source, targetUrl)
                } else {
                    chaptersListViewModel.getChapters()
                }
            }
        }.getOrNull()


        val fetchedImage = runCatching {
            withContext(Dispatchers.IO) {
                if (chaptersListViewModel.getImage() != null) {
                    chaptersListViewModel.getImage()
                } else {
                    fetchChapterImage(metadata.source, targetUrl)
                }
            }
        }.getOrNull()

        val fetchedSummary = runCatching {
            withContext(Dispatchers.IO) {
                if (chaptersListViewModel.getSummary().isNotBlank()) {
                    chaptersListViewModel.getSummary()
                } else {
                    fetchSummary(metadata.source, targetUrl)
                }
            }
        }.getOrDefault("")

        val fetchedMangaName = runCatching {
            withContext(Dispatchers.IO) {
                if (chaptersListViewModel.getName().isNotBlank()) {
                    chaptersListViewModel.getName()
                } else {
                    fetchMangaTitle(metadata.source, targetUrl)
                }
            }
        }.getOrDefault("")

        chapters = fetchedChapters
        image = fetchedImage
        summary = fetchedSummary
        mangaName = fetchedMangaName

        chaptersListViewModel.setChapters(fetchedChapters)
        chaptersListViewModel.setImage(fetchedImage)
        chaptersListViewModel.setSummary(fetchedSummary)
        chaptersListViewModel.setName(fetchedMangaName)

        // Restore scroll position after data is loaded
        val savedPosition = chaptersListViewModel.getScrollPosition()
        if (savedPosition > 0 && fetchedChapters != null) {
            // Small delay to ensure layout is complete
            delay(100)
            scrollState.scrollTo(savedPosition)
        }
    }


    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val context = LocalContext.current

        val isFavorite = remember(favoritesViewModel.favorites.value, targetUrl) {
            favoritesViewModel.favorites.value.any { it.mangaUrl == targetUrl }
        }

        ChaptersHeaderSection(
            targetUrl = targetUrl,
            image = image,
            mangaName = mangaName,
            summary = summary,
            isSummaryExpanded = isSummaryExpanded,
            extensionName = metadata.source.getExtensionName(),
            isFavorite = isFavorite,
            onToggleSummary = { isSummaryExpanded = !isSummaryExpanded },
            onToggleFavorite = {
                scope.launch {
                    if (isFavorite) {
                        for (favoriteItem in favoritesViewModel.favorites.value) {
                            if (favoriteItem.mangaUrl == targetUrl) {
                                favoritesViewModel.removeFavorite(favoriteItem.id, context)
                                Toast.makeText(
                                    context,
                                    "Removed from favorites",
                                    Toast.LENGTH_SHORT
                                ).show()
                                break
                            }
                        }
                    } else {
                        val favoriteEntity = FavoritesEntity(
                            id = UUID.randomUUID(),
                            mangaUrl = targetUrl,
                            mangaTitle = mangaName,
                            created_at = System.currentTimeMillis(),
                            extensionId = UUID.fromString(metadata.source.getExtensionId())
                        )
                        favoritesViewModel.addFavorite(favoriteEntity)
                        Toast.makeText(
                            context,
                            "Added to favorites",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )

        Text(
            text = "${chapters?.size ?: 0} chapters",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 8.dp)
        )

        if (isSelectionMode) {
            ChaptersSelectionBar(
                selectedCount = selectedChapters.size,
                modifier = Modifier.fillMaxWidth(),
                onApplySelection = {
                    scope.launch {
                        for (chapterUrl: String in selectedChapters.toList()) {
                            // Remove from history
                            if (historyViewModel.isChapterInHistory(
                                    mangaUrl = targetUrl,
                                    chapterUrl = chapterUrl
                                )
                            ) {
                                historyViewModel.deleteChapterFromHistory(
                                    targetUrl,
                                    chapterUrl,
                                    context
                                )
                            }
                            // Add to history
                            else {
                                historyViewModel.ensureHistoryAndAddChapter(
                                    mangaUrl = targetUrl,
                                    mangaName = mangaName,
                                    extensionId = UUID.fromString(metadata.source.getExtensionId()),
                                    chapterUrl = chapterUrl
                                )
                            }
                        }
                        selectedChapters.clear()
                    }
                },
                onDownloadSelection = {
                    scope.launch {
                        val selectedList = selectedChapters.toList()
                        val queueTotal = selectedList.size

                        for ((index, chapterUrl) in selectedList.withIndex()) {
                            downloadsViewModel.createDownload(
                                mangaurl = targetUrl,
                                mangaName = mangaName,
                                mangaSummary = summary,
                                chapterUrl = chapterUrl,
                                extensionMetadata = metadata,
                                context = context,
                                queueIndex = index + 1,
                                queueTotal = queueTotal
                            )
                        }
                        selectedChapters.clear()
                    }
                }
            )
        }

        chapters?.let {
            if (it.isEmpty()) {
                Text("No chapters found.", style = MaterialTheme.typography.bodySmall)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    it.forEach { chapter ->
                        ChapterListItemCard(
                            chapter = chapter,
                            targetUrl = targetUrl,
                            historyViewModel = historyViewModel,
                            chaptersListViewModel = chaptersListViewModel,
                            navHostController = navHostController,
                            scrollState = scrollState,
                            selectedChapterUrls = selectedChapters,
                            isSelectionMode = isSelectionMode
                        )
                    }
                }
            }
        }
    }
}
