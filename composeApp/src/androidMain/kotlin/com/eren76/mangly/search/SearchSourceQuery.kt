package com.eren76.mangly.search

import androidx.compose.runtime.MutableState
import com.eren76.mangly.viewmodels.SearchUiState
import com.eren76.manglyextension.plugins.ExtensionMetadata
import com.eren76.manglyextension.plugins.LoadedSource
import com.eren76.manglyextension.plugins.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
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
    workingSources: List<LoadedSource>,
    uiState: MutableState<SearchUiState>
) = supervisorScope {
    workingSources.forEach { workingSource ->
        launch {
            val result: ExtensionSearchResult = querySource(query = query, workingSource = workingSource)
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
    workingSource: LoadedSource
): ExtensionSearchResult = withContext(Dispatchers.IO) {
    try {
        ExtensionSearchResult(
            metadata = workingSource.metadata,
            results = workingSource.source.search(query),
            error = null
        )
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        ExtensionSearchResult(
            metadata = workingSource.metadata,
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
        resultsByWorkingSource = currentState.resultsByWorkingSource + (result.metadata to result.results),
        errorsByWorkingSource = if (result.error == null) {
            currentState.errorsByWorkingSource - result.metadata
        } else {
            currentState.errorsByWorkingSource + (result.metadata to result.error)
        },
        pendingWorkingSourceMetadata = currentState.pendingWorkingSourceMetadata - result.metadata
    )
}

private fun formatExtensionError(error: Throwable): String {
    val message = error.message
        ?.takeIf { it.isNotBlank() }
        ?: error::class.java.simpleName

    return "${error::class.java.simpleName}: $message"
}
