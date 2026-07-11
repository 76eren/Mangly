package com.eren76.mangly.composables.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eren76.mangly.composables.shared.collection.CollectionGrid
import com.eren76.mangly.composables.shared.collection.CollectionGridState
import com.eren76.mangly.composables.shared.collection.GridViewMode
import com.eren76.mangly.composables.shared.collection.rememberCollectionGridState

@Composable
fun HomeLayout(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        content()
    }
}

@Composable
fun <T> ColumnScope.HomeLibraryContent(
    mode: HomeMode,
    allItems: List<T>,
    filteredItems: List<T>,
    sourceFilterState: HomeSourceFilterState,
    viewMode: GridViewMode,
    isLoadingItems: Boolean,
    isLoadingSources: Boolean,
    itemKey: (T) -> Any,
    supportingContent: @Composable ColumnScope.() -> Unit = {},
    itemContent: @Composable (T) -> Unit
) {
    val collectionState: CollectionGridState = rememberCollectionGridState(
        viewMode = viewMode,
        itemCount = filteredItems.size,
        resetKey = sourceFilterState.activeSourceId
    )
    val isContentReady = !isLoadingItems &&
            allItems.isNotEmpty() &&
            !isLoadingSources &&
            filteredItems.isNotEmpty()

    HomeHeader(
        mode = mode,
        sourceFilterState = sourceFilterState,
        showSourceFilter = !isLoadingItems &&
                !isLoadingSources &&
                allItems.isNotEmpty() &&
                sourceFilterState.sourceOptions.size > 1,
        pageLabel = collectionState.pageInfo
            ?.takeIf { isContentReady }
            ?.let { pageInfo -> "${pageInfo.currentPage} / ${pageInfo.totalPages}" }
    )

    supportingContent()

    if (isLoadingItems) {
        HomeLoadingState(
            text = mode.loadingText,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
        return
    }

    if (allItems.isEmpty()) {
        HomeEmptyState(
            text = mode.emptyText,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
        return
    }

    if (isLoadingSources) {
        HomeLoadingState(
            text = "Loading sources...",
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
        return
    }

    if (filteredItems.isEmpty()) {
        HomeEmptyState(
            text = mode.emptyFilteredText,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
        return
    }

    CollectionGrid(
        items = filteredItems,
        state = collectionState,
        itemKey = itemKey,
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth(),
        itemContent = itemContent
    )
}

@Composable
fun HomeHeader(
    mode: HomeMode,
    sourceFilterState: HomeSourceFilterState,
    showSourceFilter: Boolean,
    modifier: Modifier = Modifier,
    pageLabel: String? = null
) {
    val selectedSource = sourceFilterState.sourceOptions
        .firstOrNull { option -> option.id == sourceFilterState.activeSourceId }
    val sourceLabel = selectedSource?.displayName ?: "All sources"
    val visibleItemCount = selectedSource?.itemCount
        ?: sourceFilterState.sourceOptions.sumOf { option -> option.itemCount }
    val itemLabel = if (visibleItemCount == 1) "item" else "items"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 18.dp, top = 8.dp, end = 12.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mode.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (showSourceFilter) {
                Text(
                    text = "$sourceLabel \u00B7 $visibleItemCount $itemLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (pageLabel != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = pageLabel,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                )
            }
        }

        if (showSourceFilter) {
            Spacer(modifier = Modifier.width(6.dp))
            HomeSourceFilter(
                sourceOptions = sourceFilterState.sourceOptions,
                selectedSourceId = sourceFilterState.activeSourceId,
                onSelectedSourceChange = sourceFilterState.onSelectedSourceChange
            )
        }
    }
}

@Composable
private fun HomeLoadingState(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun HomeEmptyState(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}
