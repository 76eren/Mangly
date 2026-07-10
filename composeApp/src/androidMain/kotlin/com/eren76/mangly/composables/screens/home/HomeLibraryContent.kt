package com.eren76.mangly.composables.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
    content: @Composable (Modifier) -> Unit
) {
    if (allItems.isEmpty()) {
        HomeEmptyState(
            text = mode.emptyText,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
        return
    }

    HomeSourceFilter(
        itemCountsBySource = sourceFilterState.itemCountsBySource,
        selectedSource = sourceFilterState.activeSource,
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
