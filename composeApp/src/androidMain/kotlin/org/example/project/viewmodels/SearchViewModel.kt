package org.example.project.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.manglyextension.plugins.ExtensionMetadata
import com.example.manglyextension.plugins.Source
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor() : ViewModel() {
    var searchResults by mutableStateOf(HashMap<ExtensionMetadata, List<Source.SearchResult>>())
    var searchQuery by mutableStateOf("")

    fun updateSearchViewModel(
        results: HashMap<ExtensionMetadata, List<Source.SearchResult>>,
        searchQuery: String
    ) {
        this.searchResults = results
        this.searchQuery = searchQuery
    }

    fun clearSearchResults() {
        this.searchResults.clear()
        this.searchQuery = ""
    }

}