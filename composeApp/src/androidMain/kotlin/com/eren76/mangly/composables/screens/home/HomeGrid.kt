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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private const val MAX_PAGE_INDICATORS = 9

// Shows either favorites or downloads in a LazyVerticalGrid.
@Composable
fun <T> ShowHomeItemsInLazyGrid(
    homeItems: List<T>,
    key: ((T) -> Any)? = null,
    modifier: Modifier = Modifier.fillMaxSize(),
    itemContent: @Composable (T) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.padding(8.dp),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = homeItems,
            key = key
        ) { item ->
            itemContent(item)
        }
    }
}

// Shows either favorites or downloads in a Paginated HorizontalPager with a grid layout.
@Composable
fun <T> PaginatedHomeGrid(
    pageSize: Int,
    homeItems: List<T>,
    onPageStartIndexChanged: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier.fillMaxSize(),
    itemContent: @Composable (T) -> Unit
) {
    val safePageSize = remember(pageSize) { maxOf(1, pageSize) }
    val totalPages =
        if (homeItems.isEmpty()) 1 else (homeItems.size + safePageSize - 1) / safePageSize
    val columns = 2
    val rows = remember(safePageSize) {
        maxOf(1, (safePageSize + columns - 1) / columns)
    }

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
        modifier = modifier.padding(16.dp)
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
            val endIndex = minOf(startIndex + safePageSize, homeItems.size)
            val pageItems = if (startIndex < homeItems.size) {
                homeItems.subList(startIndex, endIndex)
            } else {
                emptyList()
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(rows) { rowIndex ->
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        repeat(2) { columnIndex ->
                            val itemIndex = rowIndex * columns + columnIndex

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                if (itemIndex < pageItems.size) {
                                    itemContent(pageItems[itemIndex])
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
                if (totalPages > MAX_PAGE_INDICATORS) {
                    Text(
                        text = "${pagerState.currentPage + 1} / $totalPages",
                        style = MaterialTheme.typography.labelMedium
                    )
                } else {
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
}
