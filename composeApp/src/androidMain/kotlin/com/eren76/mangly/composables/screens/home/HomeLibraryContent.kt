package com.eren76.mangly.composables.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeLayout(
    mode: HomeMode,
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
        Text(
            text = mode.title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

        content()
    }
}

@Composable
fun <T> ColumnScope.HomeLibraryContent(
    mode: HomeMode,
    allItems: List<T>,
    filteredItems: List<T>,
    sourceFilterState: HomeSourceFilterState,
    isLoadingItems: Boolean,
    isLoadingSources: Boolean,
    content: @Composable (Modifier) -> Unit
) {
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

    HomeSourceFilter(
        sourceOptions = sourceFilterState.sourceOptions,
        selectedSourceId = sourceFilterState.activeSourceId,
        onSelectedSourceChange = sourceFilterState.onSelectedSourceChange
    )

    if (filteredItems.isEmpty()) {
        HomeEmptyState(
            text = mode.emptyFilteredText,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
        return
    }

    content(
        Modifier
            .weight(1f)
            .fillMaxWidth()
    )
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
