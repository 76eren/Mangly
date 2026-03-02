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
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.eren76.mangly.composables.shared.image.ImageLoadingComposable
import com.eren76.mangly.composables.shared.image.ImageLoadingErrorComposable
import com.eren76.mangly.rooms.entities.FavoritesEntity
import com.eren76.mangly.viewmodels.ExtensionMetadataViewModel
import com.eren76.mangly.viewmodels.FavoritesViewModel
import com.eren76.manglyextension.plugins.ExtensionMetadata
import com.eren76.manglyextension.plugins.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun Home(
    favoritesViewModel: FavoritesViewModel,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    navHostController: NavHostController
) {
    val favorites = favoritesViewModel.favorites.value
    val sortedFavorites = remember(favorites) {
        favorites.sortedBy { it.mangaTitle.lowercase() }
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

        val downloaded: DownloadedImage? = runCatching {
            downloadImage(coverInfo.imageUrl, coverInfo.headers)
        }.getOrNull()

        if (downloaded == null || downloaded.bytes.isEmpty()) {
            loadFailed = true
            return@LaunchedEffect
        }

        val ext = resolveImageExtension(downloaded, coverInfo.imageUrl)
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

    val networkHeaders = remember(imageForList?.headers) { buildNetworkHeaders(imageForList) }

    val localRequest = remember(localCoverFile) {
        buildLocalImageRequest(context = context, file = localCoverFile)
    }

    val remoteRequest = remember(imageForList?.imageUrl, networkHeaders) {
        buildRemoteImageRequest(
            context = context,
            imageForList = imageForList,
            networkHeaders = networkHeaders
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


private class DownloadedImage(
    val bytes: ByteArray,
    val contentType: String?,
    val finalUrl: String
)

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

private suspend fun downloadImage(
    imageUrl: String,
    headers: List<Source.Header>
): DownloadedImage? {
    return withContext(Dispatchers.IO) {
        val client = OkHttpClient()
        val requestBuilder = Request.Builder().url(imageUrl)
        for (h in headers) {
            requestBuilder.addHeader(h.name, h.value)
        }

        client.newCall(requestBuilder.build()).execute().use { resp ->
            if (!resp.isSuccessful) return@use null
            val bodyBytes = resp.body?.bytes() ?: return@use null
            DownloadedImage(
                bytes = bodyBytes,
                contentType = resp.header("Content-Type"),
                finalUrl = resp.request.url.toString()
            )
        }
    }
}

private fun resolveImageExtension(downloaded: DownloadedImage, originalUrl: String): String {
    return extensionFromContentType(downloaded.contentType)
        ?: extensionFromUrl(downloaded.finalUrl)
        ?: extensionFromUrl(originalUrl)
        ?: "jpg"
}

private fun buildNetworkHeaders(imageForList: Source.ImageForChaptersList?): NetworkHeaders {
    return NetworkHeaders.Builder().apply {
        val headers = imageForList?.headers ?: emptyList()
        for (header in headers) {
            this[header.name] = header.value
        }
    }.build()
}

private fun buildLocalImageRequest(context: Context, file: File?): ImageRequest? {
    return file?.let {
        ImageRequest.Builder(context)
            .data(it)
            .crossfade(false)
            .build()
    }
}

private fun buildRemoteImageRequest(
    context: Context,
    imageForList: Source.ImageForChaptersList?,
    networkHeaders: NetworkHeaders
): ImageRequest {
    return ImageRequest.Builder(context)
        .data(imageForList?.imageUrl)
        .apply {
            val hdrs = imageForList?.headers
            if (!hdrs.isNullOrEmpty()) httpHeaders(networkHeaders)
        }
        .crossfade(false)
        .build()
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


private fun extensionFromContentType(contentType: String?): String? {
    val ct = contentType?.substringBefore(';')?.trim()?.lowercase()
    return when (ct) {
        "image/jpeg", "image/jpg" -> "jpg"
        "image/png" -> "png"
        "image/webp" -> "webp"
        "image/gif" -> "gif"
        "image/avif" -> "avif"
        "image/bmp" -> "bmp"
        "image/svg+xml" -> "svg"
        else -> null
    }
}

private fun extensionFromUrl(url: String): String? {
    val withoutQuery = url.substringBefore('?').substringBefore('#')
    val ext = withoutQuery.substringAfterLast('.', missingDelimiterValue = "")
        .trim()
        .lowercase()

    if (ext.isBlank() || ext.length > 5) return null
    if (!ext.all { it.isLetterOrDigit() }) return null

    return ext
}