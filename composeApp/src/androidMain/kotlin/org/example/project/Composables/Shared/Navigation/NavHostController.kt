package org.example.project.Composables.Shared.Navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.example.project.Composables.Standard.ChaptersList.ChaptersList
import org.example.project.Composables.Standard.ExtensionDetails.ExtensionDetails
import org.example.project.Composables.Standard.Extensions
import org.example.project.Composables.Standard.Home
import org.example.project.Composables.Standard.Read.Read
import org.example.project.Composables.Standard.Search
import org.example.project.Composables.Standard.Settings
import org.example.project.ViewModels.ChaptersListViewModel
import org.example.project.ViewModels.ExtensionDetailsViewModel
import org.example.project.ViewModels.ExtensionMetadataViewModel
import org.example.project.ViewModels.FavoritesViewModel
import org.example.project.ViewModels.SearchViewModel
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
    favoritesViewModel: FavoritesViewModel

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
                Settings()
            }

            composable("sources") {
                searchViewModel.clearSearchResults()
                chaptersListViewModel.clear()
                Extensions(navController, extensionsViewModel, extensionMetadataViewModel)
            }

            // Regular routes
            composable("extensionDetails/{id}") { backStackEntry ->
                val name = backStackEntry.arguments?.getString("id")
                extensionsViewModel.selectCardByName(name ?: "")
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
                    navController
                )
            }

            composable("read/{url}") { backStackEntry ->
                val encodedUrl = backStackEntry.arguments?.getString("url").orEmpty()
                val url = URLDecoder.decode(encodedUrl, StandardCharsets.UTF_8.toString())
                Read(url, extensionMetadataViewModel)
            }

        })
}