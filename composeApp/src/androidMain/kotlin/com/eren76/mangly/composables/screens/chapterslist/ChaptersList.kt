package com.eren76.mangly.composables.screens.chapterslist

import android.content.Context
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
import com.eren76.mangly.Constants
import com.eren76.mangly.composables.shared.dialogs.BatteryOptimizationPromptDialog
import com.eren76.mangly.downloads.models.DownloadChapterQueueRequest
import com.eren76.mangly.permissions.BatteryOptimizationPermissionHandling
import com.eren76.mangly.rooms.entities.DownloadedChapterEntity
import com.eren76.mangly.rooms.entities.FavoritesEntity
import com.eren76.mangly.rooms.relations.DownloadWithChapters
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
import java.io.File
import java.util.UUID

// TODO: This is a very large composable function, needs to be refactored in the future.
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

    val relatedDownload: DownloadWithChapters? =
        downloads.find { it.download.mangaUrl == targetUrl }

    // In downloads mode we must be fully offline-capable.
    // That means we must NOT require extension metadata (which is normally set before navigation).
    if (!showDownloads && metadata == null) {
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
    var loadError by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    // Local selection state for this screen only
    val selectedChapters = remember { mutableStateListOf<String>() }
    val isSelectionMode = selectedChapters.isNotEmpty()
    val context = LocalContext.current
    var showBatteryOptimizationPrompt by remember { mutableStateOf(false) }
    val downloadsPrefs = remember {
        context.getSharedPreferences(Constants.READING_SETTING_KEY, Context.MODE_PRIVATE)
    }
    val isDownloadModeEnabled = remember(downloadsPrefs) {
        downloadsPrefs.getBoolean(Constants.MANGLY_ENBALE_DOWNLOADS, false)
    }
    var localCoverFile by remember { mutableStateOf<File?>(null) }

    BackHandler(enabled = isSelectionMode) {
        selectedChapters.clear()
    }

    DisposableEffect(Unit) {
        onDispose {
            val position = scrollState.value
            chaptersListViewModel.setScrollPosition(position)
        }
    }

    LaunchedEffect(targetUrl, metadata, showDownloads, relatedDownload) {
        // Download only mode no networking
        if (showDownloads) {
            val localChapters: List<Source.ChapterValue> = relatedDownload
                ?.chapters
                ?.filter { it.isFullyDownloaded }
                ?.mapNotNull { downloadedChapter -> mapDownloadedChapter(downloadedChapter) }
                .orEmpty()

            chapters = localChapters
            image = null
            summary = relatedDownload?.download?.mangaSummary.orEmpty()
            mangaName = relatedDownload?.download?.mangaName ?: targetUrl
            loadError = null
            localCoverFile = relatedDownload?.download?.coverImageFilename?.let { filename ->
                downloadsViewModel.getCoverFile(filename = filename, context = context)
            }

            chaptersListViewModel.setChapters(localChapters)
            chaptersListViewModel.setImage(null)
            chaptersListViewModel.setSummary(summary)
            chaptersListViewModel.setName(mangaName)
            return@LaunchedEffect
        }

        localCoverFile = null
        loadError = null

        // Regular online mode
        val safeMetadata = metadata ?: return@LaunchedEffect

        val fetchedChapters = runCatching {
            withContext(Dispatchers.IO) {
                if (chaptersListViewModel.getChapters().isEmpty()) {
                    fetchChapterList(safeMetadata.source, targetUrl)
                } else {
                    chaptersListViewModel.getChapters()
                }
            }
        }.onFailure { error ->
            loadError = "Failed to load chapters from ${safeMetadata.source.getExtensionName()}: ${
                formatSourceError(error)
            }"
        }.getOrNull()


        val fetchedImage = runCatching {
            withContext(Dispatchers.IO) {
                if (chaptersListViewModel.getImage() != null) {
                    chaptersListViewModel.getImage()
                } else {
                    fetchChapterImage(safeMetadata.source, targetUrl)
                }
            }
        }.getOrNull()

        val fetchedSummary = runCatching {
            withContext(Dispatchers.IO) {
                if (chaptersListViewModel.getSummary().isNotBlank()) {
                    chaptersListViewModel.getSummary()
                } else {
                    fetchSummary(safeMetadata.source, targetUrl)
                }
            }
        }.getOrDefault("")

        val fetchedMangaName = runCatching {
            withContext(Dispatchers.IO) {
                if (chaptersListViewModel.getName().isNotBlank()) {
                    chaptersListViewModel.getName()
                } else {
                    fetchMangaTitle(safeMetadata.source, targetUrl)
                }
            }
        }.getOrDefault("")

        chapters = fetchedChapters ?: emptyList()
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
        val isFavorite = remember(favoritesViewModel.favorites.value, targetUrl) {
            favoritesViewModel.favorites.value.any { it.mangaUrl == targetUrl }
        }

        if (chapters == null) {
            ChaptersListSkeleton()
        } else {
            val chapterList = chapters.orEmpty()

            val continueReadingChapter: Source.ChapterValue? = remember(
                historyViewModel.historyWithChapters.value,
                targetUrl,
                chapterList,
                showDownloads
            ) {
                val availableChapterUrls: Set<String> =
                    chapterList.map { chapter -> chapter.url }.toSet()
                val latestReadChapter = if (showDownloads) {
                    historyViewModel.getLatestReadChapter(targetUrl, availableChapterUrls)
                } else {
                    historyViewModel.getLatestReadChapter(targetUrl)
                }

                latestReadChapter?.chapterUrl?.let { chapterUrl ->
                    chapterList.firstOrNull { chapter -> chapter.url == chapterUrl }
                        ?: Source.ChapterValue(
                            title = fallbackChapterTitle(chapterUrl),
                            url = chapterUrl
                        )
                }
            }

            loadError?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            ChaptersHeaderSection(
                targetUrl = targetUrl,
                image = image,
                localCoverFile = localCoverFile,
                mangaName = mangaName,
                summary = summary,
                isSummaryExpanded = isSummaryExpanded,
                extensionName = metadata?.source?.getExtensionName().orEmpty(),
                isFavorite = isFavorite,
                isContinueReadingEnabled = continueReadingChapter != null,
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
                                // If we reached this code path, we are not in downloads mode and metadata is present.
                                extensionId = UUID.fromString(metadata!!.source.getExtensionId())
                            )
                            favoritesViewModel.addFavorite(favoriteEntity)
                            Toast.makeText(
                                context,
                                "Added to favorites",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                },
                onContinueReading = {
                    continueReadingChapter?.let { chapter: Source.ChapterValue ->
                        onChapterClick(
                            navHostController = navHostController,
                            mangaUrl = chapter.url,
                            chaptersListViewModel = chaptersListViewModel,
                            scrollPosition = scrollState.value,
                            chapterTitle = chapter.title,
                            chapterUrl = targetUrl,
                            isDownload = showDownloads
                        )
                    }
                }
            )

            Text(
                text = "${chapterList.size} chapters",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 8.dp)
            )

            if (isSelectionMode) {
                ChaptersSelectionBar(
                    selectedCount = selectedChapters.size,
                    modifier = Modifier.fillMaxWidth(),
                    onSelectAll = {
                        selectedChapters.clear()
                        selectedChapters.addAll(
                            chapterList.map { chapter -> chapter.url }.distinct()
                        )
                    },
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
                                        extensionId = UUID.fromString(metadata!!.source.getExtensionId()),
                                        chapterUrl = chapterUrl
                                    )
                                }
                            }
                            selectedChapters.clear()
                        }
                    },
                    onDownloadOrDeleteSelection = {
                        scope.launch {
                            if (!showDownloads) {
                                // Download chapters
                                val selectedList: List<String> = selectedChapters.toList()
                                val chaptersToQueue: List<DownloadChapterQueueRequest> =
                                    selectedList.map { chapterUrl ->
                                        DownloadChapterQueueRequest(
                                            chapterUrl = chapterUrl,
                                            chapterName = chapterList.find { it.url == chapterUrl }?.title
                                                ?: chapterUrl
                                        )
                                    }
                                val enqueuedCount: Int = downloadsViewModel.queueDownloads(
                                    mangaurl = targetUrl,
                                    mangaName = mangaName,
                                    mangaSummary = summary,
                                    chapters = chaptersToQueue,
                                    extensionMetadata = metadata!!,
                                    context = context
                                )
                                val toastText = if (enqueuedCount == 0) {
                                    "Selected chapters are already in the queue"
                                } else {
                                    "$enqueuedCount chapters added to the queue"
                                }
                                Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
                                if (
                                    enqueuedCount > 0 &&
                                    BatteryOptimizationPermissionHandling
                                        .shouldShowBackgroundDownloadPrompt(context)
                                ) {
                                    showBatteryOptimizationPrompt = true
                                }
                                selectedChapters.clear()
                            } else {
                                // Delete chapters
                                scope.launch {
                                    val selectedList = selectedChapters.toList()
                                    for (chapterUrl in selectedList) {
                                        downloadsViewModel.deleteChapter(
                                            mangaUrl = targetUrl,
                                            chapterUrl = chapterUrl,
                                            context = context
                                        )
                                    }
                                    selectedChapters.clear()
                                    val found = downloadsViewModel.hasDownload(targetUrl)
                                    if (!found) {
                                        navHostController.popBackStack()
                                    }
                                }

                            }
                        }
                    },
                    showDownloadUi = isDownloadModeEnabled || showDownloads,
                    isDownloadMode = showDownloads
                )
            }

            if (chapterList.isEmpty()) {
                Text(
                    if (showDownloads) "No downloaded chapters found." else "No chapters found.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    chapterList.forEach { chapter: Source.ChapterValue ->
                        ChapterListItemCard(
                            chapter = chapter,
                            targetUrl = targetUrl,
                            historyViewModel = historyViewModel,
                            chaptersListViewModel = chaptersListViewModel,
                            navHostController = navHostController,
                            scrollState = scrollState,
                            selectedChapterUrls = selectedChapters,
                            isSelectionMode = isSelectionMode,
                            isDownload = showDownloads
                        )
                    }
                }
            }
        }
    }

    BatteryOptimizationPromptDialog(
        visible = showBatteryOptimizationPrompt,
        onConfirm = {
            showBatteryOptimizationPrompt = false
            val opened: Boolean =
                BatteryOptimizationPermissionHandling.openBatteryOptimizationSettings(
                    context
                )
            if (!opened) {
                Toast.makeText(
                    context,
                    "Could not open battery optimization settings",
                    Toast.LENGTH_SHORT
                ).show()
            }
        },
        onDismiss = {
            showBatteryOptimizationPrompt = false
            BatteryOptimizationPermissionHandling.dismissBackgroundDownloadPrompt(context)
        }
    )
}

private fun formatSourceError(error: Throwable): String {
    val message = error.message
        ?.takeIf { it.isNotBlank() }
        ?: error::class.java.simpleName

    return "${error::class.java.simpleName}: $message"
}

private fun mapDownloadedChapter(downloadedChapter: DownloadedChapterEntity): Source.ChapterValue? {
    val chapterUrl = downloadedChapter.chapterUrl?.takeIf { it.isNotBlank() } ?: return null
    val chapterTitle = downloadedChapter.chapterName
        ?.takeIf { it.isNotBlank() }
        ?: chapterUrl.substringAfterLast('/')
            .substringBefore('?')
            .takeIf { it.isNotBlank() }
        ?: "Downloaded chapter"

    return Source.ChapterValue(
        title = chapterTitle,
        url = chapterUrl
    )
}

// Because there is the risk that the chapter title is not available (meaning it got source changed the url or title) we need to fallback to a title based on the chapter url.
private fun fallbackChapterTitle(chapterUrl: String): String {
    return chapterUrl.substringAfterLast('/')
        .substringBefore('?')
        .takeIf { it.isNotBlank() }
        ?: "Last read chapter"
}
