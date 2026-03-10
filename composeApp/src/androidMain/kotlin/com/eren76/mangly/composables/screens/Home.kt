package com.eren76.mangly.composables.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil3.compose.SubcomposeAsyncImage
import com.eren76.mangly.Constants
import com.eren76.mangly.composables.shared.image.CoverCache
import com.eren76.mangly.composables.shared.image.CoverImageRequests
import com.eren76.mangly.composables.shared.image.ImageLoadingComposable
import com.eren76.mangly.composables.shared.image.ImageLoadingErrorComposable
import com.eren76.mangly.rooms.entities.FavoritesEntity
import com.eren76.mangly.rooms.entities.HistoryChapterEntity
import com.eren76.mangly.rooms.entities.HistoryWithReadChapters
import com.eren76.mangly.viewmodels.ExtensionMetadataViewModel
import com.eren76.mangly.viewmodels.FavoritesViewModel
import com.eren76.mangly.viewmodels.HistoryViewModel
import com.eren76.manglyextension.plugins.ExtensionMetadata
import com.eren76.manglyextension.plugins.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// TODO: This should be moved somewhere else but I am not sure where
enum class HomeSorting(val prefValue: String, val label: String) {
    LatestRead("latest_read", "Latest read"),
    LatestFavorite("latest_favorite", "Latest favorites"),
    Alphabetical("alphabetical", "By alphabet");

    companion object {
        const val DEFAULT_PREF_VALUE = "latest_read"

        fun fromPrefValue(value: String?): HomeSorting = when (value) {
            LatestFavorite.prefValue -> LatestFavorite
            Alphabetical.prefValue -> Alphabetical
            else -> LatestRead
        }
    }
}

@Composable
fun Home(
    favoritesViewModel: FavoritesViewModel,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    historyViewModel: HistoryViewModel,
    navHostController: NavHostController
) {
    val context = LocalContext.current
    val favorites = favoritesViewModel.favorites.value

    val sortingPref = remember {
        context.getSharedPreferences(
            Constants.HOME_SETTING_KEY,
            Context.MODE_PRIVATE
        ).getString(
            Constants.HOME_SORTING_SETTING_KEY,
            HomeSorting.DEFAULT_PREF_VALUE
        )
    }

    val sorting = remember(sortingPref) {
        HomeSorting.fromPrefValue(sortingPref)
    }

    val sortedFavorites = remember(favorites, sorting) {
        sortFavorites(favorites, sorting, historyViewModel)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            "Favorites",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

        if (sortedFavorites.isEmpty()) {
            Text(
                text = "No favorites yet.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sortedFavorites) { favorite: FavoritesEntity ->
                    FavoriteCard(
                        favorite = favorite,
                        extensionMetadataViewModel = extensionMetadataViewModel,
                        favoritesViewModel = favoritesViewModel,
                        onClick = {
                            onFavoriteClick(
                                favorite = favorite,
                                extensionMetadataViewModel = extensionMetadataViewModel,
                                navController = navHostController
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteCard(
    favorite: FavoritesEntity,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    favoritesViewModel: FavoritesViewModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(220.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            FavoriteImage(
                favorite = favorite,
                extensionMetadataViewModel = extensionMetadataViewModel,
                favoritesViewModel = favoritesViewModel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = favorite.mangaTitle,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                )
            }
        }
    }
}


@Composable
private fun FavoriteImage(
    favorite: FavoritesEntity,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    favoritesViewModel: FavoritesViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var localCoverFile by remember(favorite.id, favorite.coverImageFilename) {
        mutableStateOf<File?>(null)
    }

    LaunchedEffect(favorite.id, favorite.coverImageFilename) {
        localCoverFile = favorite.coverImageFilename?.let { filename ->
            favoritesViewModel.getCoverFile(filename = filename, context = context)
        }
    }

    var imageForList by remember(favorite.id, favorite.mangaUrl) {
        mutableStateOf<Source.ImageForChaptersList?>(null)
    }

    var loadFailed by remember(favorite.id, favorite.mangaUrl) { mutableStateOf(false) }

    LaunchedEffect(favorite.id, favorite.mangaUrl, localCoverFile) {
        if (localCoverFile != null) return@LaunchedEffect

        loadFailed = false
        imageForList = null

        val metadata = findMetadataForFavorite(extensionMetadataViewModel, favorite)
        if (metadata == null) {
            loadFailed = true
            return@LaunchedEffect
        }

        val coverInfo = runCatching { getCoverImageInfoForFavorite(metadata, favorite) }.getOrNull()
        if (coverInfo == null || coverInfo.imageUrl.isBlank()) {
            loadFailed = true
            return@LaunchedEffect
        }

        imageForList = coverInfo

        val downloaded = runCatching {
            CoverCache.downloadImage(coverInfo.imageUrl, coverInfo.headers)
        }.getOrNull()

        if (downloaded == null || downloaded.bytes.isEmpty()) {
            loadFailed = true
            return@LaunchedEffect
        }

        val ext = CoverCache.inferImageExtension(
            contentType = downloaded.contentType,
            finalUrl = downloaded.finalUrl,
            originalUrl = coverInfo.imageUrl
        )

        val filename = "${favorite.id}.$ext"

        val savedFile = withContext(Dispatchers.IO) {
            favoritesViewModel.saveCoverBytes(
                filename = filename,
                bytes = downloaded.bytes,
                context = context
            )
        }

        favoritesViewModel.updateFavoriteCoverFilename(
            favoriteId = favorite.id,
            filename = filename
        )
        localCoverFile = savedFile
    }

    val networkHeaders = remember(imageForList?.headers) {
        CoverCache.buildNetworkHeaders(imageForList?.headers ?: emptyList())
    }

    val localRequest = remember(localCoverFile) {
        CoverImageRequests.local(context = context, file = localCoverFile)
    }

    val remoteRequest = remember(imageForList?.imageUrl, networkHeaders) {
        CoverImageRequests.remote(
            context = context,
            imageForList = imageForList,
            networkHeaders = networkHeaders,
            crossfade = false
        )
    }

    when {
        loadFailed -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                ImageLoadingErrorComposable()
            }
        }

        localRequest != null -> {
            SubcomposeAsyncImage(
                model = localRequest,
                contentDescription = favorite.mangaTitle,
                modifier = modifier,
                contentScale = ContentScale.Crop,
                error = { ImageLoadingErrorComposable() },
                loading = { ImageLoadingComposable() }
            )
        }

        imageForList?.imageUrl == null -> {
            Box(
                modifier = modifier.background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                ImageLoadingComposable()
            }
        }

        else -> {
            SubcomposeAsyncImage(
                model = remoteRequest,
                contentDescription = favorite.mangaTitle,
                modifier = modifier,
                contentScale = ContentScale.Crop,
                error = { ImageLoadingErrorComposable() },
                loading = { ImageLoadingComposable() }
            )
        }
    }
}

private fun onFavoriteClick(
    favorite: FavoritesEntity,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    navController: NavHostController
) {
    // Find the corresponding extension by ID and set it as selected
    val targetMetadata: ExtensionMetadata? = extensionMetadataViewModel
        .getAllSources()
        .find { it.source.getExtensionId() == favorite.extensionId.toString() }

    if (targetMetadata != null) {
        extensionMetadataViewModel.setSelectedSource(targetMetadata)
        val encodedUrl = URLEncoder.encode(favorite.mangaUrl, StandardCharsets.UTF_8.toString())
        navController.navigate("chapters/${encodedUrl}")
    }
}

private fun findMetadataForFavorite(
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    favorite: FavoritesEntity
): ExtensionMetadata? {
    return extensionMetadataViewModel
        .getAllSources()
        .find { it.source.getExtensionId() == favorite.extensionId.toString() }
}

private suspend fun getCoverImageInfoForFavorite(
    metadata: ExtensionMetadata,
    favorite: FavoritesEntity
): Source.ImageForChaptersList? {
    return withContext(Dispatchers.IO) {
        metadata.source.getImageForChaptersList(favorite.mangaUrl)
    }
}

private fun sortFavorites(
    favorites: List<FavoritesEntity>,
    sorting: HomeSorting,
    historyViewModel: HistoryViewModel
): List<FavoritesEntity> {
    if (favorites.isEmpty()) return favorites

    return when (sorting) {
        HomeSorting.Alphabetical -> favorites.sortedWith(
            compareBy(
                { it.mangaTitle.lowercase() },
                { it.mangaUrl },
                { it.id.toString() }
            ))

        HomeSorting.LatestFavorite -> favorites.sortedWith(
            compareByDescending<FavoritesEntity> { favorite ->
                val ts = favorite.created_at
                if (ts > 0L) ts else Long.MIN_VALUE
            }.thenBy { it.mangaTitle.lowercase() }
                .thenBy { it.mangaUrl }
                .thenBy { it.id.toString() }
        )

        HomeSorting.LatestRead -> {
            val historyWithChapters = historyViewModel.historyWithChapters.value
            val latestReadByUrl: Map<String, Long> =
                computeLatestReadByMangaUrl(historyWithChapters)

            favorites.sortedWith(
                compareByDescending<FavoritesEntity> { favorite ->
                    val lastRead = latestReadByUrl[favorite.mangaUrl] ?: 0L
                    val hasHistory = lastRead > 0L
                    if (hasHistory) 1 else 0
                }
                    .thenByDescending { favorite ->
                        val lastRead = latestReadByUrl[favorite.mangaUrl] ?: 0L
                        if (lastRead > 0L) lastRead else Long.MIN_VALUE
                    }
                    .thenByDescending { favorite ->
                        val ts = favorite.created_at
                        if (ts > 0L) ts else Long.MIN_VALUE
                    }
                    .thenBy { it.mangaTitle.lowercase() }
                    .thenBy { it.mangaUrl }
                    .thenBy { it.id.toString() }
            )
        }
    }
}

private fun computeLatestReadByMangaUrl(
    historyWithChapters: List<HistoryWithReadChapters>
): Map<String, Long> {
    val latestByUrl = mutableMapOf<String, Long>()

    for (entry: HistoryWithReadChapters in historyWithChapters) {
        val history = entry.history
        var latest = 0L
        for (readChapter: HistoryChapterEntity in entry.readChapters) {
            val readAt = readChapter.readAt ?: continue
            if (readAt > latest) {
                latest = readAt
            }
        }
        if (latest <= 0L) continue

        val url = history.mangaUrl
        val current = latestByUrl[url]
        if (current == null || latest > current) {
            latestByUrl[url] = latest
        }
    }

    return latestByUrl
}