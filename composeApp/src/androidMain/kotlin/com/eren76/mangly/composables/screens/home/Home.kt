package com.eren76.mangly.composables.screens.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.eren76.mangly.Constants
import com.eren76.mangly.viewmodels.DownloadsViewModel
import com.eren76.mangly.viewmodels.ExtensionMetadataViewModel
import com.eren76.mangly.viewmodels.FavoritesViewModel
import com.eren76.mangly.viewmodels.HistoryViewModel

@Composable
fun Home(
    favoritesViewModel: FavoritesViewModel,
    downloadsViewModel: DownloadsViewModel,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    historyViewModel: HistoryViewModel,
    navHostController: NavHostController,
    showDownloads: Boolean = false,
) {
    val context = LocalContext.current

    // Preferences
    val sortingPref = remember {
        context.getSharedPreferences(
            Constants.HOME_SETTING_KEY,
            Context.MODE_PRIVATE
        ).getString(
            Constants.HOME_SORTING_SETTING_KEY,
            HomeSorting.DEFAULT_PREF_VALUE
        )
    }
    val paginationPreferences = remember {
        context.getSharedPreferences(
            Constants.PAGINATION_SETTINGS_KEY,
            Context.MODE_PRIVATE
        )
    }
    val sorting = remember(sortingPref) {
        HomeSorting.fromPrefValue(sortingPref)
    }

    val favorites = favoritesViewModel.favorites.value
    val downloads = downloadsViewModel.downloads.value
    val historyWithChapters = historyViewModel.historyWithChapters.value

    val sortedFavorites = remember(favorites, sorting, historyWithChapters) {
        sortFavorites(favorites, sorting, historyViewModel)
    }
    val sortedDownloads = remember(downloads, sorting, historyWithChapters) {
        sortDownloads(downloads, sorting, historyViewModel)
    }

    val isPaginationEnabled: Boolean =
        paginationPreferences.getBoolean(Constants.PAGINATION_ENABLED_KEY, false)
    val pageSize: Int = paginationPreferences.getInt(
        Constants.MANGLY_PAGINATION_SIZE_KEY.toString(),
        Constants.MANGLY_PAGINATION_SIZE_KEY
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            if (showDownloads) "Downloads" else "Favorites",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

        // I kinda hate this but for now it works, needs to be refactored at some point
        if (showDownloads) {
            if (sortedDownloads.isEmpty()) {
                Text(
                    text = "No downloads yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                if (!isPaginationEnabled) {
                    ShowDownloadsInLazyGrid(
                        downloads = sortedDownloads,
                        extensionMetadataViewModel = extensionMetadataViewModel,
                        navHostController = navHostController,
                        downloadsViewModel = downloadsViewModel
                    )
                } else {
                    PaginatedDownloads(
                        pageSize = pageSize,
                        downloads = sortedDownloads,
                        extensionMetadataViewModel = extensionMetadataViewModel,
                        downloadsViewModel = downloadsViewModel,
                        navHostController = navHostController
                    )
                }
            }
        } else if (sortedFavorites.isEmpty()) {
            Text(
                text = "No favorites yet.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            if (!isPaginationEnabled) {
                ShowItemsInLazyGrid(
                    sortedFavorites = sortedFavorites,
                    extensionMetadataViewModel = extensionMetadataViewModel,
                    favoritesViewModel = favoritesViewModel,
                    navHostController = navHostController
                )
            } else {
                PaginatedFavorites(
                    pageSize = pageSize,
                    sortedFavorites = sortedFavorites,
                    extensionMetadataViewModel = extensionMetadataViewModel,
                    favoritesViewModel = favoritesViewModel,
                    navHostController = navHostController
                )
            }
        }
    }
}
