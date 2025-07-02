package org.example.project.Composables.Shared.Navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.example.project.Composables.Standard.ExtensionDetails.ExtensionDetails
import org.example.project.Composables.Standard.Home
import org.example.project.Composables.Standard.Search
import org.example.project.Composables.Standard.Settings
import org.example.project.Composables.Standard.Extensions
import org.example.project.ViewModels.ExtensionDetailsViewModel

@Composable
fun NavHostContainer(
    navController: NavHostController,
    padding: PaddingValues,

    // View models
    extensionsViewModel: ExtensionDetailsViewModel

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
                Home()
            }

            composable("search") {
                Search()
            }

            composable("settings") {
                Settings()
            }

            composable("sources") {
                Extensions(navController, extensionsViewModel)
            }

            // Regular routes

            composable("extensionDetails/{id}") { backStackEntry ->
                val name = backStackEntry.arguments?.getString("id")
                extensionsViewModel.selectCardByName(name ?: "")
                extensionsViewModel.selectedCardData?.let {
                    ExtensionDetails(cardData = it)
                }
            }
        })
}