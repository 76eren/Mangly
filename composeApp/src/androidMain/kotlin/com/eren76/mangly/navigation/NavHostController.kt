package com.eren76.mangly.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.eren76.mangly.composables.screens.ExtensionDetails
import com.eren76.mangly.composables.screens.Extensions
import com.eren76.mangly.composables.screens.chapterslist.ChaptersList
import com.eren76.mangly.composables.screens.history.HistoryManagement
import com.eren76.mangly.composables.screens.home.Home
import com.eren76.mangly.composables.screens.read.Read
import com.eren76.mangly.composables.screens.search.Search
import com.eren76.mangly.composables.screens.settings.Settings
import com.eren76.mangly.viewmodels.BackupSettingsViewModel
import com.eren76.mangly.viewmodels.ChaptersListViewModel
import com.eren76.mangly.viewmodels.DownloadsViewModel
import com.eren76.mangly.viewmodels.ExtensionDetailsViewModel
import com.eren76.mangly.viewmodels.ExtensionMetadataViewModel
import com.eren76.mangly.viewmodels.FavoritesViewModel
import com.eren76.mangly.viewmodels.HistoryViewModel
import com.eren76.mangly.viewmodels.SearchViewModel
import com.eren76.manglyextension.plugins.LoadedSource
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun NavHostContainer(
    navController: NavHostController,
    padding: PaddingValues,

    // View models
    extensionsViewModel: ExtensionDetailsViewModel,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    searchViewModel: SearchViewModel,
    chaptersListViewModel: ChaptersListViewModel,
    favoritesViewModel: FavoritesViewModel,
    historyViewModel: HistoryViewModel,
    downloadsViewModel: DownloadsViewModel,
    backupSettingsViewModel: BackupSettingsViewModel,

    ) {

    NavHost(
        navController = navController,

        // set the start destination as home
        startDestination = "home",

        // Set the padding provided by scaffold
        modifier = Modifier.padding(paddingValues = padding),

        builder = {

            // Bottom nav routes
            composable("home") {
                searchViewModel.clearSearchResults()
                chaptersListViewModel.clear()
                Home(
                    favoritesViewModel,
                    downloadsViewModel,
                    extensionMetadataViewModel,
                    historyViewModel,
                    navController,
                    showDownloads = false
                )
            }

            composable("home/downloads") {
                searchViewModel.clearSearchResults()
                chaptersListViewModel.clear()
                Home(
                    favoritesViewModel,
                    downloadsViewModel,
                    extensionMetadataViewModel,
                    historyViewModel,
                    navController,
                    showDownloads = true
                )
            }

            composable("search") {
                chaptersListViewModel.clear()

                Search(
                    extensionMetadataViewModel = extensionMetadataViewModel,
                    navHostController = navController,
                    searchViewModel = searchViewModel
                )
            }

            composable("settings") {
                searchViewModel.clearSearchResults()
                chaptersListViewModel.clear()
                Settings(
                    navController = navController,
                    backupSettingsViewModel = backupSettingsViewModel,
                )
            }

            composable("sources") {
                searchViewModel.clearSearchResults()
                chaptersListViewModel.clear()
                Extensions(navController, extensionsViewModel, extensionMetadataViewModel)
            }

            // Regular routes
            composable("extensionDetails/{id}") { backStackEntry ->
                val sourceId = backStackEntry.arguments?.getString("id")
                extensionsViewModel.selectCardBySource(sourceId ?: "")
                extensionsViewModel.selectedCardData?.let {
                    ExtensionDetails(
                        cardData = it,
                        loadedSource = extensionMetadataViewModel.getWorkingSourceById(it.metadata.extensionId)
                    )
                }
            }

            composable("chapters/{url}") { backStackEntry ->
                val encodedUrl = backStackEntry.arguments?.getString("url").orEmpty()
                val url = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())
                val workingSource = extensionMetadataViewModel.selectedWorkingSource
                if (workingSource == null) {
                    Text(
                        text = "Extension needs updating",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    ChaptersList(
                        targetUrl = url,
                        source = workingSource,
                        chaptersListViewModel = chaptersListViewModel,
                        favoritesViewModel = favoritesViewModel,
                        historyViewModel = historyViewModel,
                        downloadsViewModel = downloadsViewModel,
                        navHostController = navController,
                        showDownloads = false
                    )
                }
            }

            composable("chapters/{url}/downloads") { backStackEntry ->
                val encodedUrl = backStackEntry.arguments?.getString("url").orEmpty()
                val url = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())
                ChaptersList(
                    targetUrl = url,
                    source = null,
                    chaptersListViewModel = chaptersListViewModel,
                    favoritesViewModel = favoritesViewModel,
                    historyViewModel = historyViewModel,
                    downloadsViewModel = downloadsViewModel,
                    navHostController = navController,
                    showDownloads = true
                )
            }

            composable("read/{url}") { backStackEntry ->
                val encodedUrl = backStackEntry.arguments?.getString("url").orEmpty()
                val url = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())
                val workingSource: LoadedSource? = extensionMetadataViewModel.selectedWorkingSource
                if (workingSource == null) {
                    Text(
                        text = "Extension needs updating",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    Read(
                        targetUrl = url,
                        source = workingSource,
                        chaptersListViewModel = chaptersListViewModel,
                        historyViewModel = historyViewModel,
                        downloadsViewModel = downloadsViewModel,
                        download = false
                    )
                }
            }

            composable("read/{url}/downloads") { backStackEntry ->
                val encodedUrl = backStackEntry.arguments?.getString("url").orEmpty()
                val url = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())
                Read(
                    targetUrl = url,
                    source = null,
                    chaptersListViewModel = chaptersListViewModel,
                    historyViewModel = historyViewModel,
                    downloadsViewModel = downloadsViewModel,
                    download = true
                )
            }

            composable("history") { backStackEntry ->
                searchViewModel.clearSearchResults()
                chaptersListViewModel.clear()
                HistoryManagement(
                    historyViewModel = historyViewModel,
                    navHostController = navController,
                    extensionMetaDataViewModel = extensionMetadataViewModel
                )

            }
        })
}
