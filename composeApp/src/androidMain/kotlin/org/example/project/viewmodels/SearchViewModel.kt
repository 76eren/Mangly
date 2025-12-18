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


    fun updateSearchResults(
        results: HashMap<ExtensionMetadata, List<Source.SearchResult>>,
    ) {
        searchResults = results
    }

    fun clearSearchResults() {
        searchResults.clear()

    }
}