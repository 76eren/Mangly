package com.eren76.mangly.composables.screens.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp
import com.eren76.mangly.viewmodels.SearchUiState
import com.eren76.manglyextension.plugins.ExtensionMetadata
import com.eren76.manglyextension.plugins.Source

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarContent(
    textFieldState: TextFieldState,
    onSearch: (String) -> Unit,
    uiState: SearchUiState,
    onResultClick: (Source.SearchResult, ExtensionMetadata) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier
            .fillMaxSize()
            .semantics { isTraversalGroup = true }
    ) {
        SearchBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .semantics { traversalIndex = 0f },
            inputField = {
                SearchBarDefaults.InputField(
                    query = textFieldState.text.toString(),
                    onQueryChange = {
                        textFieldState.edit { replace(0, length, it) }
                    },
                    onSearch = {
                        onSearch(textFieldState.text.toString())
                        expanded = true
                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    placeholder = { Text("Search") }
                )
            },
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            if (uiState.isLoading || uiState.hasSearched || uiState.results.isNotEmpty()) {
                SearchResultsContent(
                    uiState = uiState,
                    onResultClick = onResultClick,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}
