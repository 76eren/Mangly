package com.eren76.mangly.composables.screens.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.eren76.mangly.rooms.entities.FavoritesEntity
import com.eren76.mangly.viewmodels.ExtensionMetadataViewModel
import com.eren76.mangly.viewmodels.FavoritesViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

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
    val totalPages =
        if (sortedFavorites.isEmpty()) 1 else (sortedFavorites.size + safePageSize - 1) / safePageSize

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { totalPages }
    )

    LaunchedEffect(totalPages) {
        val maxPage = (totalPages - 1).coerceAtLeast(0)
        if (pagerState.currentPage > maxPage) {
            pagerState.scrollToPage(maxPage)
        }
    }

    LaunchedEffect(pagerState, safePageSize) {
        snapshotFlow { pagerState.currentPage }
            .map { it * safePageSize }
            .distinctUntilChanged()
            .collect { onPageStartIndexChanged?.invoke(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Spacer(modifier = Modifier.height(12.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            val startIndex = page * safePageSize
            val endIndex = minOf(startIndex + safePageSize, sortedFavorites.size)
            val pageItems = if (startIndex < sortedFavorites.size) {
                sortedFavorites.subList(startIndex, endIndex)
            } else {
                emptyList()
            }

            Column(
                modifier = Modifier.fillMaxSize(),
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
                                            onHomeMangaClick(
                                                mangaUrl = favorite.mangaUrl,
                                                extensionId = favorite.extensionId,
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
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (totalPages > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(totalPages) { index ->
                    val isSelected = index == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isSelected) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                    )
                }
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
                    onHomeMangaClick(
                        mangaUrl = favorite.mangaUrl,
                        extensionId = favorite.extensionId,
                        extensionMetadataViewModel = extensionMetadataViewModel,
                        navController = navHostController
                    )
                }
            )
        }
    }
}
