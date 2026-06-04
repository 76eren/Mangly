package com.eren76.mangly.viewmodels

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eren76.mangly.search.querySearchSources
import com.eren76.manglyextension.plugins.ExtensionMetadata
import com.eren76.manglyextension.plugins.Source
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor() : ViewModel() {
    var uiState = mutableStateOf(SearchUiState())
        private set

    private var searchJob: Job? = null

    fun search(query: String, sources: List<ExtensionMetadata>) {
        searchJob?.cancel()

        uiState.value = SearchUiState(
            query = query,
            sourceOrder = sources,
            loadingSources = sources.toSet(),
            hasSearched = true
        )

        searchJob = viewModelScope.launch {
            querySearchSources(query = query, sources = sources, uiState = uiState)
        }
    }

    fun clearSearchResults() {
        searchJob?.cancel()
        uiState.value = SearchUiState()
    }
}

data class SearchUiState(
    val query: String = "",
    val sourceOrder: List<ExtensionMetadata> = emptyList(),
    val results: Map<ExtensionMetadata, List<Source.SearchResult>> = emptyMap(),
    val errors: Map<ExtensionMetadata, String> = emptyMap(),
    val loadingSources: Set<ExtensionMetadata> = emptySet(),
    val hasSearched: Boolean = false
) {
    val isLoading: Boolean
        get() = loadingSources.isNotEmpty()
}
