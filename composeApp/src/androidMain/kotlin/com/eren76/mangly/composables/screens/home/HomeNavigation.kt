package com.eren76.mangly.composables.screens.home

import androidx.navigation.NavHostController
import com.eren76.mangly.rooms.entities.FavoritesEntity
import com.eren76.mangly.viewmodels.ExtensionMetadataViewModel
import com.eren76.manglyextension.plugins.ExtensionMetadata
import com.eren76.manglyextension.plugins.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun onFavoriteClick(
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

fun findMetadataForFavorite(
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    favorite: FavoritesEntity
): ExtensionMetadata? {
    return extensionMetadataViewModel
        .getAllSources()
        .find { it.source.getExtensionId() == favorite.extensionId.toString() }
}

suspend fun getCoverImageInfoForFavorite(
    metadata: ExtensionMetadata,
    favorite: FavoritesEntity
): Source.ImageForChaptersList? {
    return withContext(Dispatchers.IO) {
        metadata.source.getImageForChaptersList(favorite.mangaUrl)
    }
}

