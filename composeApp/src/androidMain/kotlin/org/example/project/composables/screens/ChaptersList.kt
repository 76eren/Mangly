package org.example.project.composables.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil3.compose.rememberAsyncImagePainter
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.manglyextension.plugins.ExtensionMetadata
import com.example.manglyextension.plugins.Source
import com.example.manglyextension.plugins.Source.ImageForChaptersList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.example.project.rooms.entities.FavoritesEntity
import org.example.project.viewmodels.ChaptersListViewModel
import org.example.project.viewmodels.ExtensionMetadataViewModel
import org.example.project.viewmodels.FavoritesViewModel
import org.example.project.viewmodels.HistoryViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID

@Composable
fun ChaptersList(
    targetUrl: String,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    chaptersListViewModel: ChaptersListViewModel,
    favoritesViewModel: FavoritesViewModel,
    historyViewModel: HistoryViewModel,
    navHostController: NavHostController
) {
    val metadata: ExtensionMetadata? = extensionMetadataViewModel.selectedSingleSource.value


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
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        val headers: List<Source.Header> = image?.headers.let {
            it ?: emptyList()
        }.map { header ->
            Source.Header(header.name, header.value)
        }

        val networkHeaders = NetworkHeaders.Builder().apply {
            for (header in headers) {
                this[header.name] = header.value
            }
        }.build()

        val imageRequest = ImageRequest.Builder(LocalContext.current)
            .data(image?.imageUrl)
            .httpHeaders(networkHeaders)
            .crossfade(true)
            .build()

        var isFavorite by remember { mutableStateOf(false) }
        for (favoriteItem in favoritesViewModel.favorites.value) {
            if (favoriteItem.mangaUrl == targetUrl) {
                isFavorite = true
                break
            }
        }

        if (isFavorite) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = "Favorite",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    for (favoriteItem in favoritesViewModel.favorites.value) {
                        if (favoriteItem.mangaUrl == targetUrl) {
                            favoritesViewModel.removeFavorite(favoriteItem.id)
                            isFavorite = !isFavorite
                            break
                        }
                    }
                }
            )
        } else {
            Icon(
                imageVector = Icons.Default.FavoriteBorder,
                contentDescription = "Favorite",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    val favoriteEntity = FavoritesEntity(
                        id = UUID.randomUUID(),
                        mangaUrl = targetUrl,
                        mangaTitle = mangaName,
                        created_at = System.currentTimeMillis(),
                        extensionId = UUID.fromString(metadata.source.getExtensionId())
                    )
                    favoritesViewModel.addFavorite(favoriteEntity)
                    isFavorite = !isFavorite
                }
            )
        }

        image?.let {
            Image(
                painter = rememberAsyncImagePainter(imageRequest),
                contentDescription = "Comic Cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(220.dp)
                    .height(320.dp)
                    .padding(bottom = 16.dp)
            )
        }

        if (summary.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "Description",
                    modifier = Modifier.padding(bottom = 4.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (isSummaryExpanded) summary else summary.take(200) + "...",
                    modifier = Modifier
                        .clickable { isSummaryExpanded = !isSummaryExpanded }
                        .padding(8.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = if (isSummaryExpanded) "Show less" else "Read more",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .padding(top = 4.dp, start = 8.dp, bottom = 4.dp)
                        .clickable { isSummaryExpanded = !isSummaryExpanded }
                )

                Text(
                    text = "Extension",
                    modifier = Modifier.padding(bottom = 4.dp, top = 20.dp),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = metadata.source.getExtensionName(),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
                )
            }
        }

        Text(
            text = "Chapters",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (isSelectionMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedChapters.size} selected",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = {
                    scope.launch {
                        for (chapterUrl: String in selectedChapters.toList()) {
                            // Remove from history
                            if (historyViewModel.isChapterInHistory(
                                    mangaUrl = targetUrl,
                                    chapterUrl = chapterUrl
                                )
                            ) {
                                historyViewModel.deleteChapterFromHistory(targetUrl, chapterUrl)

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
                }) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Select or unselect chapters"
                    )
                }
            }
        }

        chapters?.let {
            if (it.isEmpty()) {
                Text("No chapters found.", style = MaterialTheme.typography.bodySmall)
            } else {
                it.forEach { chapter ->
                    ChapterItem(
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

@Composable
fun ChapterItem(
    chapter: Source.ChapterValue,
    targetUrl: String,
    historyViewModel: HistoryViewModel,
    chaptersListViewModel: ChaptersListViewModel,
    navHostController: NavHostController,
    scrollState: androidx.compose.foundation.ScrollState,
    selectedChapterUrls: MutableList<String>,
    isSelectionMode: Boolean,
) {
    val isRead =
        historyViewModel.historyWithChapters.value.any { history ->
            history.history.mangaUrl == targetUrl && history.readChapters.any { readChapter ->
                readChapter.chapterUrl == chapter.url
            }
        }

    val isSelected = selectedChapterUrls.contains(chapter.url)

    fun toggleSelectionForChapter() {
        if (isSelected) {
            selectedChapterUrls.remove(chapter.url)
        } else {
            selectedChapterUrls.add(chapter.url)
        }
    }

    fun handleNormalClick() {
        if (isSelectionMode) {
            toggleSelectionForChapter()
        } else {
            onChapterClick(
                navHostController,
                chapter.url,
                chaptersListViewModel,
                scrollState.value,
                chapter.title,
                targetUrl
            )
        }
    }

    fun handleLongClick() {
        toggleSelectionForChapter()
    }

    OutlinedButton(
        onClick = { handleNormalClick() },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = when {
            isSelected -> ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )

            isRead -> ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )

            else -> ButtonDefaults.outlinedButtonColors()
        }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { handleNormalClick() },
                    onLongClick = { handleLongClick() }
                )
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else if (isRead) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = chapter.title,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onSecondaryContainer
                    isRead -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

suspend fun fetchChapterImage(source: Source, url: String): ImageForChaptersList {
    return source.getImageForChaptersList(url)
}

suspend fun fetchChapterList(source: Source, url: String): List<Source.ChapterValue> {
    return source.getChaptersFromChapterUrl(url)
}

suspend fun fetchSummary(source: Source, url: String): String {
    return source.getSummary(url)
}

suspend fun fetchMangaTitle(source: Source, url: String): String {
    return source.getMangaNameFromChapterUrl(url)
}

fun onChapterClick(
    navHostController: NavHostController,
    mangaUrl: String,
    chaptersListViewModel: ChaptersListViewModel,
    scrollPosition: Int,
    chapterTitle: String,
    chapterUrl: String
) {
    chaptersListViewModel.setScrollPosition(scrollPosition)
    chaptersListViewModel.setSelectedChapterNumber(chapterTitle)
    chaptersListViewModel.setSelectedMangaUrl(chapterUrl)
    val encodedUrl = URLEncoder.encode(mangaUrl, StandardCharsets.UTF_8.toString())
    navHostController.navigate("read/${encodedUrl}")
}