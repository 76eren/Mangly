package org.example.project.Composables.Standard.ChaptersList

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.withContext
import org.example.project.Rooms.Entities.FavoritesEntity
import org.example.project.ViewModels.ChaptersListViewModel
import org.example.project.ViewModels.ExtensionMetadataViewModel
import org.example.project.ViewModels.FavoritesViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID


@Composable
fun ChaptersList(
    targetUrl: String,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    chaptersListViewModel: ChaptersListViewModel,
    favoritesViewModel: FavoritesViewModel,
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

        chapters?.let {
            if (it.isEmpty()) {
                Text("No chapters found.", style = MaterialTheme.typography.bodySmall)
            } else {
                it.forEach { chapter ->
                    OutlinedButton(
                        onClick = {
                            onChapterClick(
                                navHostController,
                                chapter.url,
                                chaptersListViewModel,
                                scrollState.value,
                                chapter.title
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(text = chapter.title)
                    }
                }
            }
        } ?: Text("Loading chapters...", style = MaterialTheme.typography.bodySmall)
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
    url: String,
    chaptersListViewModel: ChaptersListViewModel,
    scrollPosition: Int,
    chapterTitle: String
) {
    chaptersListViewModel.setScrollPosition(scrollPosition)
    chaptersListViewModel.setSelectedChapterNumber(chapterTitle)
    val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
    navHostController.navigate("read/${encodedUrl}")
}