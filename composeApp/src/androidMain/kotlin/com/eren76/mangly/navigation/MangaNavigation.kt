package com.eren76.mangly.navigation

import androidx.navigation.NavHostController
import com.eren76.mangly.viewmodels.ExtensionMetadataViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun navigateToManga(
    mangaUrl: String,
    extensionId: String,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    navController: NavHostController,
    isDownload: Boolean = false
) {
    extensionMetadataViewModel.selectSource(extensionId)
    val encodedUrl = URLEncoder.encode(mangaUrl, StandardCharsets.UTF_8.toString())
    navController.navigate(
        if (isDownload) "chapters/$encodedUrl/downloads"
        else "chapters/$encodedUrl"
    )
}
