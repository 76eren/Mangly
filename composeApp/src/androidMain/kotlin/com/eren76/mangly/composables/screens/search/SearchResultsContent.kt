package com.eren76.mangly.composables.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eren76.mangly.viewmodels.SearchUiState
import com.eren76.manglyextension.plugins.ExtensionMetadata
import com.eren76.manglyextension.plugins.Source

@Composable
fun SearchResultsContent(
    uiState: SearchUiState,
    onResultClick: (Source.SearchResult, ExtensionMetadata) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        uiState.sourceOrder.forEach { extensionMetadata ->
            key(extensionMetadata.source.getExtensionId()) {
                val results = uiState.results[extensionMetadata]
                if (results == null && extensionMetadata in uiState.loadingSources) {
                    SearchSourceLoading(extensionMetadata = extensionMetadata)
                } else if (results != null) {
                    SearchSourceResults(
                        extensionMetadata = extensionMetadata,
                        results = results,
                        error = uiState.errors[extensionMetadata],
                        query = uiState.query,
                        onResultClick = onResultClick
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchSourceResults(
    extensionMetadata: ExtensionMetadata,
    results: List<Source.SearchResult>,
    error: String?,
    query: String,
    onResultClick: (Source.SearchResult, ExtensionMetadata) -> Unit
) {
    Text(
        text = extensionMetadata.source.getExtensionName(),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(vertical = 8.dp)
    )

    when {
        error != null -> {
            Text(
                text = "Extension error: $error",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        results.isEmpty() -> {
            Text(
                text = "No results found.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        else -> {
            val listState = rememberLazyListState()

            LaunchedEffect(query) {
                listState.scrollToItem(0)
            }

            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = results,
                    key = { index, result -> "${result.url}-$index" }
                ) { _, result ->
                    SearchResultCard(
                        searchResult = result,
                        onClick = { onResultClick(result, extensionMetadata) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchSourceLoading(
    extensionMetadata: ExtensionMetadata
) {
    Text(
        text = extensionMetadata.source.getExtensionName(),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(vertical = 8.dp)
    )

    SearchResultCardsSkeleton()
}
