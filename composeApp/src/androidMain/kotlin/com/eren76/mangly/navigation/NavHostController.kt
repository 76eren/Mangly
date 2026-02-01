package com.eren76.mangly.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.eren76.mangly.composables.screens.ChaptersList
import com.eren76.mangly.composables.screens.ExtensionDetails
import com.eren76.mangly.composables.screens.Extensions
import com.eren76.mangly.composables.screens.HistoryManagement
import com.eren76.mangly.composables.screens.Home
import com.eren76.mangly.composables.screens.Read
import com.eren76.mangly.composables.screens.Search
import com.eren76.mangly.composables.screens.Settings
import com.eren76.mangly.viewmodels.ChaptersListViewModel
import com.eren76.mangly.viewmodels.ExtensionDetailsViewModel
import com.eren76.mangly.viewmodels.ExtensionMetadataViewModel
import com.eren76.mangly.viewmodels.FavoritesViewModel
import com.eren76.mangly.viewmodels.HistoryViewModel
import com.eren76.mangly.viewmodels.SearchViewModel
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
    historyViewModel: HistoryViewModel

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
                Home(favoritesViewModel, extensionMetadataViewModel, navController)
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
                    navController = navController
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
                    ExtensionDetails(cardData = it)
                }
            }

            composable("chapters/{url}") { backStackEntry ->
                val encodedUrl = backStackEntry.arguments?.getString("url").orEmpty()
                val url = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())
                ChaptersList(
                    url,
                    extensionMetadataViewModel,
                    chaptersListViewModel,
                    favoritesViewModel,
                    historyViewModel,
                    navController
                )
            }

            composable("read/{url}") { backStackEntry ->
                val encodedUrl = backStackEntry.arguments?.getString("url").orEmpty()
                val url = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())
                Read(
                    targetUrl = url,
                    extensionMetadataViewModel = extensionMetadataViewModel,
                    chaptersListViewModel = chaptersListViewModel,
                    historyViewModel = historyViewModel
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