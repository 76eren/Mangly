package com.eren76.mangly.composables.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.eren76.mangly.navigation.navigateToManga
import com.eren76.mangly.viewmodels.ExtensionMetadataViewModel
import com.eren76.mangly.viewmodels.SearchViewModel

@Composable
fun Search(
    extensionMetadataViewModel: ExtensionMetadataViewModel,
    navHostController: NavHostController,
    searchViewModel: SearchViewModel
) {
    val textFieldState = remember { TextFieldState(searchViewModel.uiState.value.query) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        SearchBarContent(
            textFieldState = textFieldState,
            onSearch = { query ->
                searchViewModel.search(
                    query = query,
                    workingSources = extensionMetadataViewModel.getOnlyValidSourcesMetadata()
                )
            },
            uiState = searchViewModel.uiState.value,
            onResultClick = { result, extensionMetadata ->
                navigateToManga(
                    mangaUrl = result.url,
                    navController = navHostController,
                    extensionMetadataViewModel = extensionMetadataViewModel,
                    extensionId = extensionMetadata.extensionId
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
