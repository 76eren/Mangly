package org.example.project.Composables.Shared.Navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import org.example.project.Composables.Standard.Home
import org.example.project.Composables.Standard.Search
import org.example.project.Composables.Standard.Settings
import org.example.project.Composables.Standard.Sources

@Composable
fun NavHostContainer(
    navController: NavHostController,
    padding: PaddingValues
) {

    NavHost(
        navController = navController,

        // set the start destination as home
        startDestination = "home",

        // Set the padding provided by scaffold
        modifier = Modifier.padding(paddingValues = padding),

        builder = {

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
                Sources()
            }
        })
}