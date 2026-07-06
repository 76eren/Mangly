package com.eren76.mangly.composables.screens.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import coil3.compose.SubcomposeAsyncImage
import com.eren76.mangly.composables.shared.image.CoverCache
import com.eren76.mangly.composables.shared.image.CoverImageRequests
import com.eren76.mangly.composables.shared.image.ImageLoadingComposable
import com.eren76.mangly.composables.shared.image.ImageLoadingErrorComposable
import com.eren76.mangly.rooms.entities.FavoritesEntity
import com.eren76.mangly.viewmodels.ExtensionMetadataViewModel
import com.eren76.mangly.viewmodels.FavoritesViewModel
import com.eren76.manglyextension.plugins.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun FavoriteCard(
    favorite: FavoritesEntity,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    favoritesViewModel: FavoritesViewModel,
    onClick: () -> Unit,
) {
    val context = LocalContext.current

    HomeMangaCard(
        title = favorite.mangaTitle,
        menuKey = favorite.id,
        menuText = "Delete favorite",
        onClick = onClick,
        onDelete = {
            favoritesViewModel.removeFavorite(favorite.id, context)
            Toast.makeText(context, "Favorite deleted", Toast.LENGTH_SHORT).show()
        },
        imageContent = { modifier ->
            FavoriteImage(
                favorite = favorite,
                extensionMetadataViewModel = extensionMetadataViewModel,
                favoritesViewModel = favoritesViewModel,
                modifier = modifier
            )
        }
    )
}

@Composable
fun FavoriteImage(
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
