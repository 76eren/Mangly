package com.eren76.mangly.search

import androidx.compose.runtime.MutableState
import com.eren76.mangly.viewmodels.SearchUiState
import com.eren76.manglyextension.plugins.ExtensionMetadata
import com.eren76.manglyextension.plugins.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext

private data class ExtensionSearchResult(
    val metadata: ExtensionMetadata,
    val results: List<Source.SearchResult>,
    val error: String?
)

internal suspend fun querySearchSources(
    query: String,
    sources: List<ExtensionMetadata>,
    uiState: MutableState<SearchUiState>
) = supervisorScope {
    val jobs = mutableListOf<Job>()
    sources.forEach { source ->
        jobs += launch {
            val result: ExtensionSearchResult = querySource(query = query, source = source)
            addSearchResultToUiState(
                query = query,
                result = result,
                uiState = uiState
            )
        }
    }
}

private suspend fun querySource(
    query: String,
    source: ExtensionMetadata
): ExtensionSearchResult = withContext(Dispatchers.IO) {
    try {
        ExtensionSearchResult(
            metadata = source,
            results = source.source.search(query),
            error = null
        )
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        ExtensionSearchResult(
            metadata = source,
            results = emptyList(),
            error = formatExtensionError(error)
        )
    }
}

private fun addSearchResultToUiState(
    query: String,
    result: ExtensionSearchResult,
    uiState: MutableState<SearchUiState>
) {
    val currentState = uiState.value

    if (currentState.query != query) {
        return
    }

    // Force re-composition of the entire source section by creating new collections, which is why I didn't make it a mutable map
    uiState.value = currentState.copy(
        results = currentState.results + (result.metadata to result.results),
        errors = if (result.error == null) {
            currentState.errors - result.metadata
        } else {
            currentState.errors + (result.metadata to result.error)
        },
        loadingSources = currentState.loadingSources - result.metadata
    )
}

private fun formatExtensionError(error: Throwable): String {
    val message = error.message
        ?.takeIf { it.isNotBlank() }
        ?: error::class.java.simpleName

    return "${error::class.java.simpleName}: $message"
}
