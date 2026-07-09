package com.eren76.mangly.composables.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.eren76.mangly.rooms.entities.FavoritesEntity
import com.eren76.mangly.viewmodels.ExtensionMetadataViewModel
import com.eren76.mangly.viewmodels.FavoritesViewModel

@Composable
fun PaginatedFavorites(
    pageSize: Int,
    sortedFavorites: List<FavoritesEntity>,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    favoritesViewModel: FavoritesViewModel,
    navHostController: NavHostController,
    onPageStartIndexChanged: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    PaginatedHomeGrid(
        pageSize = pageSize,
        homeItems = sortedFavorites,
        onPageStartIndexChanged = onPageStartIndexChanged,
        modifier = modifier,
        itemContent = { favorite ->
            FavoriteCard(
                favorite = favorite,
                extensionMetadataViewModel = extensionMetadataViewModel,
                favoritesViewModel = favoritesViewModel,
                onClick = {
                    onHomeMangaClick(
                        mangaUrl = favorite.mangaUrl,
                        extensionId = favorite.extensionId,
                        extensionMetadataViewModel = extensionMetadataViewModel,
                        navController = navHostController
                    )
                }
            )
        }
    )
}


@Composable
fun ShowItemsInLazyGrid(
    sortedFavorites: List<FavoritesEntity>,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    favoritesViewModel: FavoritesViewModel,
    navHostController: NavHostController,
    modifier: Modifier = Modifier
) {
    ShowHomeItemsInLazyGrid(
        homeItems = sortedFavorites,
        key = { favorite -> favorite.id },
        modifier = modifier,
        itemContent = { favorite ->
            FavoriteCard(
                favorite = favorite,
                extensionMetadataViewModel = extensionMetadataViewModel,
                favoritesViewModel = favoritesViewModel,
                onClick = {
                    onHomeMangaClick(
                        mangaUrl = favorite.mangaUrl,
                        extensionId = favorite.extensionId,
                        extensionMetadataViewModel = extensionMetadataViewModel,
                        navController = navHostController
                    )
                }
            )
        }
    )
}
