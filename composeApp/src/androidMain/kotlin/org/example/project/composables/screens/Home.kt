package org.example.project.composables.screens

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
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.example.manglyextension.plugins.ExtensionMetadata
import com.example.manglyextension.plugins.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.rooms.entities.FavoritesEntity
import org.example.project.viewmodels.ExtensionMetadataViewModel
import org.example.project.viewmodels.FavoritesViewModel
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
                items(sortedFavorites) { favorite ->
                    FavoriteCard(
                        favorite = favorite,
                        extensionMetadataViewModel = extensionMetadataViewModel,
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var imageUrl by remember { mutableStateOf<String?>(null) }
    var headers by remember { mutableStateOf<List<Source.Header>>(emptyList()) }

    LaunchedEffect(favorite) {
        val targetMetadata: ExtensionMetadata? = extensionMetadataViewModel
            .getAllSources()
            .find { it.source.getExtensionId() == favorite.extensionId.toString() }

        targetMetadata?.let { metadata ->
            val image: Source.ImageForChaptersList? = runCatching {
                withContext(Dispatchers.IO) { metadata.source.getImageForChaptersList(favorite.mangaUrl) }
            }.getOrNull()
            imageUrl = image?.imageUrl
            headers = image?.headers ?: emptyList()
        }
    }

    val networkHeaders = remember(headers) {
        NetworkHeaders.Builder().apply {
            for (header in headers) {
                this[header.name] = header.value
            }
        }.build()
    }

    val cacheKey = "favorite_cover_${favorite.mangaUrl.hashCode()}"
    val imageRequest = remember(imageUrl, networkHeaders, cacheKey) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .apply { if (headers.isNotEmpty()) httpHeaders(networkHeaders) }
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .crossfade(true)
            .build()
    }

    if (imageUrl == null) {
        // Placeholder while loading
        Box(
            modifier = modifier.background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Loading…", style = MaterialTheme.typography.bodySmall)
        }
    } else {
        AsyncImage(
            model = imageRequest,
            contentDescription = favorite.mangaTitle,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
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