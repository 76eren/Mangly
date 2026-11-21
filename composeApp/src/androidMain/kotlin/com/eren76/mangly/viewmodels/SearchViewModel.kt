package com.eren76.mangly.viewmodels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eren76.mangly.search.querySearchSources
import com.eren76.manglyextension.plugins.ExtensionMetadata
import com.eren76.manglyextension.plugins.LoadedSource
import com.eren76.manglyextension.plugins.Source
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@HiltViewModel
class SearchViewModel @Inject constructor() : ViewModel() {
    var uiState = mutableStateOf(SearchUiState())
        private set

    private var searchJob: Job? = null

    fun search(query: String, workingSources: List<LoadedSource>) {
        searchJob?.cancel()

        uiState.value = SearchUiState(
            query = query,
            workingSourceMetadataInOrder = workingSources.map { it.metadata },
            pendingWorkingSourceMetadata = workingSources.mapTo(mutableSetOf()) { it.metadata },
            hasSearched = true
        )

        searchJob = viewModelScope.launch {
            querySearchSources(query = query, workingSources = workingSources, uiState = uiState)
        }
    }

    fun clearSearchResults() {
        searchJob?.cancel()
        uiState.value = SearchUiState()
    }
}

data class SearchUiState(
    val query: String = "",
    val workingSourceMetadataInOrder: List<ExtensionMetadata> = emptyList(),
    val resultsByWorkingSource: Map<ExtensionMetadata, List<Source.SearchResult>> = emptyMap(),
    val errorsByWorkingSource: Map<ExtensionMetadata, String> = emptyMap(),
    val pendingWorkingSourceMetadata: Set<ExtensionMetadata> = emptySet(),
    val hasSearched: Boolean = false
) {
    val isLoading: Boolean
        get() = pendingWorkingSourceMetadata.isNotEmpty()
}
