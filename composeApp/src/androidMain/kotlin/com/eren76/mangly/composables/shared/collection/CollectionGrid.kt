package com.eren76.mangly.composables.shared.collection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Immutable
sealed interface GridViewMode {
    data object Scrolling : GridViewMode

    @Immutable
    data class Paginated(val pageSize: Int) : GridViewMode {
        init {
            require(pageSize > 0) { "pageSize must be greater than zero" }
        }
    }
}

@Immutable
data class PageInfo(
    val currentPage: Int,
    val totalPages: Int
)

@Stable
class CollectionGridState internal constructor(
    val viewMode: GridViewMode,
    internal val pagerState: PagerState?,
    val totalPages: Int
) {
    val pageInfo: PageInfo?
        get() {
            val currentPagerState = pagerState ?: return null
            if (totalPages <= 1) return null

            return PageInfo(
                currentPage = (currentPagerState.currentPage + 1).coerceAtMost(totalPages),
                totalPages = totalPages
            )
        }
}

@Composable
fun rememberCollectionGridState(
    viewMode: GridViewMode,
    itemCount: Int,
    resetKey: Any?
): CollectionGridState {
    return when (viewMode) {
        GridViewMode.Scrolling -> remember(viewMode) {
            CollectionGridState(
                viewMode = viewMode,
                pagerState = null,
                totalPages = 1
            )
        }

        is GridViewMode.Paginated -> key(resetKey, viewMode.pageSize) {
            val pageSize = viewMode.pageSize
            val totalPages = if (itemCount == 0) {
                1
            } else {
                (itemCount + pageSize - 1) / pageSize
            }
            val pagerState = rememberPagerState(
                initialPage = 0,
                pageCount = { totalPages }
            )

            LaunchedEffect(totalPages) {
                val lastPage = (totalPages - 1).coerceAtLeast(0)
                if (pagerState.currentPage > lastPage) {
                    pagerState.scrollToPage(lastPage)
                }
            }

            remember(viewMode, pagerState, totalPages) {
                CollectionGridState(
                    viewMode = viewMode,
                    pagerState = pagerState,
                    totalPages = totalPages
                )
            }
        }
    }
}

@Composable
fun <T> CollectionGrid(
    items: List<T>,
    state: CollectionGridState,
    itemKey: (T) -> Any,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T) -> Unit
) {
    when (val viewMode = state.viewMode) {
        GridViewMode.Scrolling -> ScrollingCollectionGrid(
            items = items,
            itemKey = itemKey,
            modifier = modifier,
            itemContent = itemContent
        )

        is GridViewMode.Paginated -> PaginatedCollectionGrid(
            items = items,
            pageSize = viewMode.pageSize,
            pagerState = checkNotNull(state.pagerState),
            itemKey = itemKey,
            modifier = modifier,
            itemContent = itemContent
        )
    }
}

@Composable
private fun <T> ScrollingCollectionGrid(
    items: List<T>,
    itemKey: (T) -> Any,
    modifier: Modifier,
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
            items = items,
            key = itemKey
        ) { item ->
            itemContent(item)
        }
    }
}

@Composable
private fun <T> PaginatedCollectionGrid(
    items: List<T>,
    pageSize: Int,
    pagerState: PagerState,
    itemKey: (T) -> Any,
    modifier: Modifier,
    itemContent: @Composable (T) -> Unit
) {
    val columns = 2
    val rows = maxOf(1, (pageSize + columns - 1) / columns)

    HorizontalPager(
        state = pagerState,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    ) { page ->
        val startIndex = page * pageSize
        val endIndex = minOf(startIndex + pageSize, items.size)
        val pageItems = if (startIndex < items.size) {
            items.subList(startIndex, endIndex)
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
                    repeat(columns) { columnIndex ->
                        val itemIndex = rowIndex * columns + columnIndex

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            if (itemIndex < pageItems.size) {
                                val item = pageItems[itemIndex]
                                key(itemKey(item)) {
                                    itemContent(item)
                                }
                            } else {
                                Spacer(modifier = Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }
        }
    }
}
