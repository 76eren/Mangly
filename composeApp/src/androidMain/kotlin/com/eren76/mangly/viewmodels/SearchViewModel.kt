package com.eren76.mangly.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eren76.mangly.search.SearchQueryResult
import com.eren76.mangly.search.querySearchSources
import com.eren76.manglyextension.plugins.ExtensionMetadata
import com.eren76.manglyextension.plugins.Source
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor() : ViewModel() {
    var uiState by mutableStateOf(SearchUiState())
        private set

    private var searchJob: Job? = null

    fun search(query: String, sources: List<ExtensionMetadata>) {
        searchJob?.cancel()

        uiState = uiState.copy(
            query = query,
            isLoading = true,
            hasSearched = true
        )

        searchJob = viewModelScope.launch {
            val searchResult: SearchQueryResult =
                querySearchSources(query = query, sources = sources)
            uiState = uiState.copy(
                results = searchResult.results,
                errors = searchResult.errors,
                isLoading = false
            )
        }
    }

    fun clearSearchResults() {
        searchJob?.cancel()
        uiState = SearchUiState()
    }
}

data class SearchUiState(
    val query: String = "",
    val results: Map<ExtensionMetadata, List<Source.SearchResult>> = emptyMap(),
    val errors: Map<ExtensionMetadata, String> = emptyMap(),
    val isLoading: Boolean = false,
    val hasSearched: Boolean = false
)