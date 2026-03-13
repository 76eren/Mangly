package com.eren76.mangly.composables.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    onPageStartIndexChanged: ((Int) -> Unit)? = null
) {
    val safePageSize = remember(pageSize) { maxOf(1, pageSize) }

    var paginationIndexToGetFrom by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(sortedFavorites.size, safePageSize) {
        val maxStart = if (sortedFavorites.isEmpty()) 0
        else ((sortedFavorites.size - 1) / safePageSize) * safePageSize

        val clamped = paginationIndexToGetFrom.coerceIn(0, maxStart)
        if (clamped != paginationIndexToGetFrom) {
            paginationIndexToGetFrom = clamped
        }
        onPageStartIndexChanged?.invoke(paginationIndexToGetFrom)
    }

    val startIndex = paginationIndexToGetFrom
    val endIndex = minOf(startIndex + safePageSize, sortedFavorites.size)

    val pageItems = if (startIndex < sortedFavorites.size) {
        sortedFavorites.subList(startIndex, endIndex)
    } else {
        emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            text = "Page ${startIndex / safePageSize + 1} of ${(sortedFavorites.size + safePageSize - 1) / safePageSize}",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(3) { rowIndex ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(2) { columnIndex ->
                        val itemIndex = rowIndex * 2 + columnIndex

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            if (itemIndex < pageItems.size) {
                                val favorite = pageItems[itemIndex]

                                FavoriteCard(
                                    favorite = favorite,
                                    extensionMetadataViewModel = extensionMetadataViewModel,
                                    favoritesViewModel = favoritesViewModel,
                                    onClick = {
                                        onFavoriteClick(
                                            favorite = favorite,
                                            extensionMetadataViewModel = extensionMetadataViewModel,
                                            navController = navHostController
                                        )
                                    }
                                )
                            } else {
                                Spacer(modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Button(
                onClick = {
                    paginationIndexToGetFrom = maxOf(0, paginationIndexToGetFrom - safePageSize)
                    onPageStartIndexChanged?.invoke(paginationIndexToGetFrom)
                },
                enabled = paginationIndexToGetFrom > 0
            ) {
                Text("Previous")
            }

            Button(
                onClick = {
                    if (paginationIndexToGetFrom + safePageSize < sortedFavorites.size) {
                        paginationIndexToGetFrom += safePageSize
                        onPageStartIndexChanged?.invoke(paginationIndexToGetFrom)
                    }
                },
                enabled = paginationIndexToGetFrom + safePageSize < sortedFavorites.size
            ) {
                Text("Next")
            }
        }
    }
}

@Composable
fun ShowItemsInLazyGrid(
    sortedFavorites: List<FavoritesEntity>,
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    favoritesViewModel: FavoritesViewModel,
    navHostController: NavHostController
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sortedFavorites) { favorite: FavoritesEntity ->
            FavoriteCard(
                favorite = favorite,
                extensionMetadataViewModel = extensionMetadataViewModel,
                favoritesViewModel = favoritesViewModel,
                onClick = {
                    onFavoriteClick(
                        favorite = favorite,
                        extensionMetadataViewModel = extensionMetadataViewModel,
                        navController = navHostController
                    )
                }
            )
        }
    }
}

