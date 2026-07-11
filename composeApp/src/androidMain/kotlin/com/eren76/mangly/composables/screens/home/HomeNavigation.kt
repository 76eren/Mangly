package com.eren76.mangly.composables.screens.home

import androidx.navigation.NavHostController
import com.eren76.mangly.rooms.entities.FavoritesEntity
import com.eren76.mangly.viewmodels.ExtensionMetadataViewModel
import com.eren76.manglyextension.plugins.ExtensionMetadata
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID

fun onHomeMangaClick(
    mangaUrl: String,
    extensionId: UUID?,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    navController: NavHostController,
    isDownload: Boolean = false
) {
    if (extensionId == null) return

    val targetMetadata: ExtensionMetadata? =
        extensionMetadataViewModel.getSourceById(extensionId.toString())

    if (targetMetadata == null) return

    extensionMetadataViewModel.setSelectedSource(targetMetadata) // TODO: This should not be done this way but for now it's fine
    val encodedUrl = URLEncoder.encode(mangaUrl, StandardCharsets.UTF_8.toString())
    navController.navigate(
        if (isDownload) "chapters/$encodedUrl/downloads"
        else "chapters/$encodedUrl"
    )
}

fun findMetadataForFavorite(
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    favorite: FavoritesEntity
): ExtensionMetadata? {
    return extensionMetadataViewModel.getSourceById(favorite.extensionId.toString())
}

