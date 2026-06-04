package com.eren76.mangly.composables.screens.search

import androidx.navigation.NavHostController
import com.eren76.mangly.viewmodels.ExtensionMetadataViewModel
import com.eren76.manglyextension.plugins.ExtensionMetadata
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

fun navigateToSearchResult(
    url: String,
    navController: NavHostController,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    correspondingSource: ExtensionMetadata
) {
    extensionMetadataViewModel.setSelectedSource(source = correspondingSource)

    val encodedUrl = URLEncoder.encode(url, StandardCharsets.UTF_8.toString())
    navController.navigate("chapters/${encodedUrl}")
}
